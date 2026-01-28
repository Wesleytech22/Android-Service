package com.android.musicsplayerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(
    private val songs: List<Song>,
    private val onItemClick: (Song) -> Unit,
    private val onAddToQueueClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.song_title)
        val artistTextView: TextView = view.findViewById(R.id.song_artist)
        val durationTextView: TextView = view.findViewById(R.id.song_duration)
        val addButton: ImageButton = view.findViewById(R.id.btn_add_to_queue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        holder.titleTextView.text = song.title
        holder.artistTextView.text = song.artist
        holder.durationTextView.text = song.getFormattedDuration()

        // Clique na música para tocar agora
        holder.itemView.setOnClickListener {
            onItemClick(song)
        }

        // Botão para adicionar à fila
        holder.addButton.setOnClickListener {
            onAddToQueueClick(song)
        }

        // Feedback visual ao clicar
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.7f
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = songs.size
}