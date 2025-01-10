package com.example.kursovoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onFavoriteClick: (Note) -> Unit,
    private val onTrashClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    fun updateList(newNotes: List<Note>) {
        this.notes = newNotes
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleTextView.text = note.title

        val content = if (note.content!!.length > 150) {
            note.content!!.substring(0, 150) + "..."
        } else {
            note.content
        }
        holder.contentTextView.text = content


        if (note.isInTrash) {
            holder.favoriteButton.visibility = View.GONE
        } else {
            holder.favoriteButton.visibility = View.VISIBLE
            if (note.isFavorite) {
                holder.favoriteButton.setImageResource(R.drawable.ic_star_filled)
            } else {
                holder.favoriteButton.setImageResource(R.drawable.ic_star_outline)
            }
        }

        holder.favoriteButton.setOnClickListener {
            onFavoriteClick(note)
        }

        holder.trashButton.setOnClickListener {
            onTrashClick(note)
        }

        holder.itemView.setOnClickListener {
            onNoteClick(note)
        }
    }

    override fun getItemCount() = notes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.noteTitleTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.noteContentTextView)
        val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteIcon)
        val trashButton: ImageView = itemView.findViewById(R.id.trashIcon)
    }
}