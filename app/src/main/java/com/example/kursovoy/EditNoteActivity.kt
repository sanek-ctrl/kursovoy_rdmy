package com.example.kursovoy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditNoteActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val contentEditText = findViewById<EditText>(R.id.contentEditText)
        val saveNoteButton = findViewById<MaterialButton>(R.id.saveNoteButton)
        val cancelButton = findViewById<MaterialButton>(R.id.cancelButton)

        noteId = intent.getStringExtra("noteId")
        val noteTitle = intent.getStringExtra("noteTitle")
        val noteContent = intent.getStringExtra("noteContent")

        if (noteTitle != null && noteContent != null) {
            titleEditText.setText(noteTitle)
            contentEditText.setText(noteContent)
        }

        saveNoteButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                saveNote(title, content)
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveNote(title: String, content: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val note = mapOf(
                "title" to title,
                "content" to content
            )

            if (noteId != null) {
                database.child("users").child(userId).child("notes").child(noteId!!).setValue(note)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Ошибка при обновлении заметки", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                val newNoteId = database.child("users").child(userId).child("notes").push().key
                database.child("users").child(userId).child("notes").child(newNoteId!!).setValue(note)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Ошибка при сохранении заметки", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}