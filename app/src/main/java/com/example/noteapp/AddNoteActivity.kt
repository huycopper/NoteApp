package com.example.noteapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.noteapp.databinding.ActivityAddNoteBinding

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding //Hệ thống tự tạo ra class ActivityAddNoteBinding dựa trên file activity_add_note.xml.
    private lateinit var db: NotesDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAddNoteBinding.inflate(layoutInflater) //"nạp" giao diện XML vào bộ nhớ và chuẩn bị các liên kết đến từng View.
        setContentView(binding.root) // Gắn giao diện vào Activity.

        db = NotesDatabaseHelper(this) // Khởi tạo lớp cơ sở dữ liệu.

        binding.saveButton.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val content = binding.contentEditText.text.toString()
            val note = Note(0, title, content)
            db.insertNote(note)
            finish()
            Toast.makeText(this, "Note Saved", Toast.LENGTH_SHORT).show()
        }
    }
}