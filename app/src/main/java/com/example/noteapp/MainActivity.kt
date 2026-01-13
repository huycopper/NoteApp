package com.example.noteapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.noteapp.databinding.ActivityMainBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: NotesDatabaseHelper
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var syncManager: SyncManager
    private val supabase = SupabaseClientManager.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotesDatabaseHelper(this)
        syncManager = SyncManager(this)
        notesAdapter = NotesAdapter(db.getAllNotes(), this)

        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = notesAdapter

        setupClickListeners()
        updateAuthUI()
    }

    private fun setupClickListeners() {
        binding.addButton.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivity(intent)
        }

        binding.accountButton.setOnClickListener {
            lifecycleScope.launch {
                if (syncManager.isLoggedIn()) {
                    showAccountDialog()
                } else {
                    startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                }
            }
        }

        binding.syncButton.setOnClickListener {
            performSync()
        }
    }

    private fun updateAuthUI() {
        lifecycleScope.launch {
            val isLoggedIn = syncManager.isLoggedIn()
            
            runOnUiThread {
                if (isLoggedIn) {
                    binding.syncButton.visibility = View.VISIBLE
                    binding.syncStatusText.visibility = View.VISIBLE
                    binding.accountButton.setImageResource(R.drawable.baseline_account_circle_24)
                    updateSyncStatus()
                } else {
                    binding.syncButton.visibility = View.GONE
                    binding.syncStatusText.visibility = View.GONE
                    binding.syncStatusText.text = "Chưa đăng nhập"
                }
            }
        }
    }

    private fun updateSyncStatus() {
        val unsyncedCount = db.getUnsyncedNotes().filter { !it.isDeleted }.size
        if (unsyncedCount > 0) {
            binding.syncStatusText.text = "$unsyncedCount ghi chú chưa đồng bộ"
            binding.syncStatusText.setTextColor(getColor(R.color.orange))
        } else {
            binding.syncStatusText.text = "Đã đồng bộ"
            binding.syncStatusText.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun performSync() {
        lifecycleScope.launch {
            binding.syncButton.isEnabled = false
            binding.syncProgressBar.visibility = View.VISIBLE
            binding.syncStatusText.text = "Đang đồng bộ..."

            val result = syncManager.performSync()

            runOnUiThread {
                binding.syncButton.isEnabled = true
                binding.syncProgressBar.visibility = View.GONE

                when (result) {
                    is SyncResult.Success -> {
                        Toast.makeText(this@MainActivity, "Đồng bộ thành công!", Toast.LENGTH_SHORT).show()
                        notesAdapter.refreshData(db.getAllNotes())
                        updateSyncStatus()
                    }
                    is SyncResult.NoNetwork -> {
                        Toast.makeText(this@MainActivity, "Không có kết nối mạng", Toast.LENGTH_SHORT).show()
                        binding.syncStatusText.text = "Không có mạng"
                    }
                    is SyncResult.NotLoggedIn -> {
                        Toast.makeText(this@MainActivity, "Vui lòng đăng nhập để đồng bộ", Toast.LENGTH_SHORT).show()
                    }
                    is SyncResult.Error -> {
                        Toast.makeText(this@MainActivity, "Lỗi: ${result.message}", Toast.LENGTH_LONG).show()
                        binding.syncStatusText.text = "Lỗi đồng bộ"
                    }
                }
            }
        }
    }

    private fun showAccountDialog() {
        lifecycleScope.launch {
            val email = supabase.auth.currentUserOrNull()?.email ?: "Unknown"
            
            runOnUiThread {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Tài khoản")
                    .setMessage("Email: $email")
                    .setPositiveButton("Đăng xuất") { _, _ ->
                        performLogout()
                    }
                    .setNegativeButton("Đóng", null)
                    .show()
            }
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                supabase.auth.signOut()
                Toast.makeText(this@MainActivity, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                updateAuthUI()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi đăng xuất: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notesAdapter.refreshData(db.getAllNotes())
        updateAuthUI()
        
        // Auto sync on resume if logged in and has network
        lifecycleScope.launch {
            if (syncManager.isLoggedIn() && NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                performSync()
            }
        }
    }
}