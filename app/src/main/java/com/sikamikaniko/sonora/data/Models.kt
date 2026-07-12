package com.sikamikaniko.sonora.data

/**
 * Gson models for the subset of the Subsonic / OpenSubsonic API that Sonora uses.
 * Every response is wrapped in a top-level "subsonic-response" object.
 */

import com.google.gson.annotations.SerializedName

data class SubsonicEnvelope(
    @SerializedName("subsonic-response") val response: SubsonicResponse?
)

data class SubsonicResponse(
    val status: String?,
    val version: String?,
    val error: SubsonicError?,
    val albumList2: AlbumList2?,
    val album: AlbumWithSongs?,
    val searchResult3: SearchResult3?,
    val artists: ArtistsContainer?,
    val artist: ArtistWithAlbums?,
    val randomSongs: RandomSongs?
)

data class SubsonicError(val code: Int?, val message: String?)

data class AlbumList2(val album: List<Album>?)

data class AlbumWithSongs(
    val id: String?,
    val name: String?,
    val artist: String?,
    val coverArt: String?,
    val songCount: Int?,
    val song: List<Song>?
)

data class SearchResult3(
    val artist: List<Artist>?,
    val album: List<Album>?,
    val song: List<Song>?
)

data class ArtistsContainer(val index: List<ArtistIndex>?)
data class ArtistIndex(val name: String?, val artist: List<Artist>?)
data class ArtistWithAlbums(val id: String?, val name: String?, val album: List<Album>?)
data class RandomSongs(val song: List<Song>?)

data class Album(
    val id: String,
    val name: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val year: Int? = null
)

data class Artist(
    val id: String,
    val name: String? = null,
    val albumCount: Int? = null,
    val coverArt: String? = null
)

data class Song(
    val id: String,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val coverArt: String? = null,
    val duration: Int? = null,
    val track: Int? = null,
    val year: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null
)
