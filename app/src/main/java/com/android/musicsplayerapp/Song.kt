package com.android.musicsplayerapp

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Int, // em milissegundos
    val resourceId: Int
) {
    fun getFormattedDuration(): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    companion object {
        // Músicas de exemplo para demonstração
        fun getSampleSongs(): List<Song> {
            return listOf(
                Song(1, "Bohemian Rhapsody", "Queen", 354000, R.raw.sample1),
                Song(2, "Imagine", "John Lennon", 183000, R.raw.sample2),
                Song(3, "Billie Jean", "Michael Jackson", 294000, R.raw.sample3),
                Song(4, "Smells Like Teen Spirit", "Nirvana", 301000, R.raw.sample4),
                Song(5, "Like a Rolling Stone", "Bob Dylan", 373000, R.raw.sample5),
                Song(6, "Hey Jude", "The Beatles", 431000, R.raw.sample6),
                Song(7, "Stairway to Heaven", "Led Zeppelin", 482000, R.raw.sample7),
                Song(8, "Hotel California", "Eagles", 391000, R.raw.sample8),
                Song(9, "Blinding Lights", "The Weeknd", 200000, R.raw.sample9),
                Song(10, "Shape of You", "Ed Sheeran", 233000, R.raw.sample10)
            )
        }
    }
}