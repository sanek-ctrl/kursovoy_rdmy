package com.example.kursovoy

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    private val notesList = mutableListOf<Note>()
    private lateinit var folderNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        val addNoteButton = findViewById<MaterialButton>(R.id.addNoteButton)
        val logoutButton = findViewById<MaterialButton>(R.id.logoutButton)
        val userEmailTextView = findViewById<TextView>(R.id.userEmailTextView)
        val folderMenuButton = findViewById<MaterialButton>(R.id.folderMenuButton)
        folderNameTextView = findViewById(R.id.folderNameTextView)
        val deleteAllButton = findViewById<MaterialButton>(R.id.deleteAllButton)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)

        userEmailTextView.text = auth.currentUser?.email ?: "Гость"

        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesAdapter = NotesAdapter(
            notesList,
            onNoteClick = { note ->
                val intent = Intent(this, EditNoteActivity::class.java)
                intent.putExtra("noteId", note.id)
                intent.putExtra("noteTitle", note.title)
                intent.putExtra("noteContent", note.content)
                startActivity(intent)
            },
            onFavoriteClick = { note ->
                note.isFavorite = !note.isFavorite
                updateNoteInDatabase(note)
            },
            onTrashClick = { note ->
                if (note.isInTrash) {
                    deleteNotePermanently(note)
                } else {
                    note.isInTrash = true
                    updateNoteInDatabase(note)
                }
            }
        )
        notesRecyclerView.adapter = notesAdapter

        addNoteButton.setOnClickListener {
            val intent = Intent(this, EditNoteActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        folderMenuButton.setOnClickListener { view ->
            showFolderMenu(view)
        }

        deleteAllButton.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        loadNotes(false, false)
    }

    private fun filterNotes(query: String) {
        val filteredNotes = if (query.isEmpty()) {
            notesList
        } else {
            notesList.filter { note ->
                note.title?.contains(query, ignoreCase = true) == true
            }
        }
        notesAdapter.updateList(filteredNotes)
    }

    private fun updateUIForFolder(isTrashFolder: Boolean) {
        val addNoteButton = findViewById<MaterialButton>(R.id.addNoteButton)
        val deleteAllButton = findViewById<MaterialButton>(R.id.deleteAllButton)

        if (isTrashFolder) {
            addNoteButton.visibility = View.GONE
            deleteAllButton.visibility = View.VISIBLE
        } else {
            addNoteButton.visibility = View.VISIBLE
            deleteAllButton.visibility = View.GONE
        }
    }

    private fun deleteNotePermanently(note: Note) {
        val userId = auth.currentUser?.uid
        if (userId != null && note.id != null) {
            database.child("users").child(userId).child("notes").child(note.id!!)
                .removeValue()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MainActivity", "Note deleted permanently: ${note.title}")
                    } else {
                        Log.e("MainActivity", "Failed to delete note: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удалить всё")
            .setMessage("Вы уверены, что хотите удалить все заметки в корзине?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteAllNotesInTrash()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteAllNotesInTrash() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("notes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (noteSnapshot in snapshot.children) {
                            val note = noteSnapshot.getValue(Note::class.java)
                            if (note != null && note.isInTrash) {
                                noteSnapshot.ref.removeValue()
                            }
                        }
                        notesList.clear()
                        notesAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainActivity", "Failed to delete notes: ${error.message}")
                    }
                })
        }
    }

    private fun loadNotes(showFavorites: Boolean, showTrash: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("notes")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        notesList.clear()
                        for (noteSnapshot in snapshot.children) {
                            val note = noteSnapshot.getValue(Note::class.java)
                            if (note != null) {
                                // Фильтрация заметок
                                if (showFavorites && !note.isFavorite) continue
                                if (showTrash && !note.isInTrash) continue
                                if (!showTrash && note.isInTrash) continue

                                note.id = noteSnapshot.key
                                notesList.add(note)
                                Log.d("MainActivity", "Note loaded: ${note.title}")
                            }
                        }
                        notesAdapter.notifyDataSetChanged()

                        updateUIForFolder(showTrash)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainActivity", "Failed to load notes: ${error.message}")
                    }
                })
        }
    }

    private fun updateNoteInDatabase(note: Note) {
        val userId = auth.currentUser?.uid
        if (userId != null && note.id != null) {
            database.child("users").child(userId).child("notes").child(note.id!!)
                .setValue(note)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MainActivity", "Note updated: ${note.title}")
                    } else {
                        Log.e("MainActivity", "Failed to update note: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun showFolderMenu(view: android.view.View) {
        val popupMenu = android.widget.PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.allNotes -> {
                    folderNameTextView.text = "Все заметки"
                    loadNotes(false, false)
                }
                R.id.favorites -> {
                    folderNameTextView.text = "Избранное"
                    loadNotes(true, false)
                }
                R.id.trash -> {
                    folderNameTextView.text = "Корзина"
                    loadNotes(false, true)
                }
            }
            true
        }

        popupMenu.show()
    }
}