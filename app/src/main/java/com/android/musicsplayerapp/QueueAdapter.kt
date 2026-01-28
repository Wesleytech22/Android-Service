package com.android.musicsplayerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class QueueAdapter(
    private var songs: List<Song> = emptyList(),
    private var currentPlayingIndex: Int = -1,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val positionTextView: TextView = view.findViewById(R.id.queue_position)
        val titleTextView: TextView = view.findViewById(R.id.queue_song_title)
        val artistTextView: TextView = view.findViewById(R.id.queue_song_artist)
        val durationTextView: TextView = view.findViewById(R.id.queue_song_duration)
        val removeButton: ImageButton = view.findViewById(R.id.btn_remove_from_queue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_queue_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        // Posição na fila (começa em 1)
        holder.positionTextView.text = (position + 1).toString()
        holder.titleTextView.text = song.title
        holder.artistTextView.text = song.artist
        holder.durationTextView.text = song.getFormattedDuration()

        // Destacar música atual que está tocando
        if (position == currentPlayingIndex) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.playing_song_bg)
            )
            holder.positionTextView.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.playing_song_text)
            )
            holder.titleTextView.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.playing_song_text)
            )
            holder.artistTextView.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.playing_song_text)
            )
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
            )
            holder.positionTextView.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.black)
            )
            holder.titleTextView.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.black)
            )
            holder.artistTextView.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gray)
            )
        }

        // Botão para remover da fila
        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }

        // Efeito visual ao tocar
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (position != currentPlayingIndex) {
                        v.alpha = 0.7f
                    }
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = songs.size

    fun updateData(newSongs: List<Song>, newCurrentIndex: Int) {
        songs = newSongs
        currentPlayingIndex = newCurrentIndex
        notifyDataSetChanged()
    }
}