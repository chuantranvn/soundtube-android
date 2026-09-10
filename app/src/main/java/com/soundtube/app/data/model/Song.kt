package com.soundtube.app.data.model

data class Song(
    val id: String,
    val title: String,
    val uploaderName: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val streamUrl: String? = null,
    val channelAvatarUrl: String? = null,
    val viewCountText: String? = null,
    val publishedTimeText: String? = null,
    val durationText: String? = null
) {
    val formattedDuration: String
        get() {
            if (!durationText.isNullOrBlank()) return durationText
            if (durationSeconds <= 0) return "--:--"
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
