package com.sikamikaniko.sonora.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans the device (internal storage + every mounted volume incl. SD card) for audio via MediaStore. */
object LocalMedia {

    private val ALBUM_ART = Uri.parse("content://media/external/audio/albumart")

    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = ArrayList<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val volumes: List<Uri> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context).map { MediaStore.Audio.Media.getContentUri(it) }
        } else {
            listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        }

        for (collection in volumes) {
            try {
                context.contentResolver.query(collection, projection, selection, null, order)?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val albumId = c.getLong(albumIdCol)
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val artUri = ContentUris.withAppendedId(ALBUM_ART, albumId)
                        songs.add(
                            Song(
                                id = "local:$id",
                                title = c.getString(titleCol) ?: "Unknown",
                                artist = c.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                                album = c.getString(albumCol),
                                albumId = albumId.toString(),
                                coverArt = artUri.toString(),
                                duration = (c.getLong(durCol) / 1000L).toInt(),
                                localUri = contentUri.toString(),
                                artUri = artUri.toString()
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // ignore an unreadable volume, keep scanning others
            }
        }
        songs
    }
}
