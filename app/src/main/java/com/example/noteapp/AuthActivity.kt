package com.example.noteapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.noteapp.databinding.ActivityAuthBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AuthActivity"
    }
    
    private lateinit var binding: ActivityAuthBinding
    private val supabase = SupabaseClientManager.client
    private var isLoginMode = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        updateUIForMode()
    }
    
    private fun setupClickListeners() {
        binding.authButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (isLoginMode) {
                performLogin(email, password)
            } else {
                performSignUp(email, password)
            }
        }
        
        binding.switchModeText.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUIForMode()
        }
        
        binding.skipButton.setOnClickListener {
            // Continue without login - use local only
            navigateToMain()
        }
    }
    
    private fun updateUIForMode() {
        if (isLoginMode) {
            binding.authTitle.text = "Đăng Nhập"
            binding.authButton.text = "Đăng Nhập"
            binding.switchModeText.text = "Chưa có tài khoản? Đăng ký"
            binding.confirmPasswordInputLayout.visibility = View.GONE
            binding.confirmPasswordEditText.text?.clear()
        } else {
            binding.authTitle.text = "Đăng Ký"
            binding.authButton.text = "Đăng Ký"
            binding.switchModeText.text = "Đã có tài khoản? Đăng nhập"
            binding.confirmPasswordInputLayout.visibility = View.VISIBLE
        }
    }
    
    private fun performLogin(email: String, password: String) {
        showLoading(true)
        Log.d(TAG, "Attempting login with email: $email")
        
        lifecycleScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                Log.d(TAG, "Login successful!")
                Toast.makeText(this@AuthActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                
                // Assign user to existing notes and sync
                val syncManager = SyncManager(this@AuthActivity)
                syncManager.assignUserToLocalNotes()
                syncManager.performSync()
                
                navigateToMain()
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                val errorMessage = parseErrorMessage(e)
                Toast.makeText(this@AuthActivity, "Đăng nhập thất bại: $errorMessage", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun performSignUp(email: String, password: String) {
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        
        if (password != confirmPassword) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        Log.d(TAG, "Attempting signup with email: $email")
        
        lifecycleScope.launch {
            try {
                val result = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                Log.d(TAG, "Signup result: $result")
                Toast.makeText(this@AuthActivity, "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.", Toast.LENGTH_LONG).show()
                
                // Switch to login mode after successful signup
                isLoginMode = true
                updateUIForMode()
            } catch (e: Exception) {
                Log.e(TAG, "Signup failed", e)
                val errorMessage = parseErrorMessage(e)
                Toast.makeText(this@AuthActivity, "Đăng ký thất bại: $errorMessage", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun parseErrorMessage(e: Exception): String {
        val message = e.message ?: "Lỗi không xác định"
        return when {
            message.contains("Invalid login credentials") -> "Email hoặc mật khẩu không đúng"
            message.contains("Email not confirmed") -> "Email chưa được xác nhận. Vui lòng kiểm tra hộp thư"
            message.contains("User already registered") -> "Email này đã được đăng ký"
            message.contains("network", ignoreCase = true) -> "Lỗi kết nối mạng"
            message.contains("Unable to resolve host") -> "Không thể kết nối đến máy chủ. Kiểm tra kết nối mạng"
            message.contains("timeout", ignoreCase = true) -> "Kết nối quá thời gian chờ"
            else -> message
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.authButton.isEnabled = !isLoading
        binding.switchModeText.isEnabled = !isLoading
        binding.skipButton.isEnabled = !isLoading
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
