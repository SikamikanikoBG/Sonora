package com.sikamikaniko.sonora.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the Subsonic REST API. Authentication query params
 * (u, t, s, v, c, f) are injected by an OkHttp interceptor, see [Subsonic].
 */
interface SubsonicApi {

    @GET("ping.view")
    suspend fun ping(): SubsonicEnvelope

    @GET("getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String = "alphabeticalByName",
        @Query("size") size: Int = 100,
        @Query("offset") offset: Int = 0
    ): SubsonicEnvelope

    @GET("getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope

    @GET("search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("songCount") songCount: Int = 40,
        @Query("albumCount") albumCount: Int = 40,
        @Query("artistCount") artistCount: Int = 40
    ): SubsonicEnvelope

    @GET("getArtists.view")
    suspend fun getArtists(): SubsonicEnvelope

    @GET("getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope

    @GET("getRandomSongs.view")
    suspend fun getRandomSongs(@Query("size") size: Int = 50): SubsonicEnvelope

    @GET("getPlaylists.view")
    suspend fun getPlaylists(): SubsonicEnvelope

    @GET("getPlaylist.view")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicEnvelope

    @GET("getStarred2.view")
    suspend fun getStarred2(): SubsonicEnvelope

    @GET("star.view")
    suspend fun star(@Query("id") id: String): SubsonicEnvelope

    @GET("unstar.view")
    suspend fun unstar(@Query("id") id: String): SubsonicEnvelope

    @GET("scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("submission") submission: Boolean = true
    ): SubsonicEnvelope
}
