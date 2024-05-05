package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Lyric
import dev.brahmkshatriya.echo.common.models.LyricsItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.extension.endpoints.EchoArtistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoArtistMoreEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLibraryEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLyricsEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoPlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongFeedEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongRelatedEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoVideoEndpoint
import dev.toastbits.ytmkt.endpoint.SongFeedLoadResult
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.PlaylistEditor
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.LOW
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.headers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.security.MessageDigest

class YoutubeExtension : ExtensionClient(), HomeFeedClient, TrackClient, SearchClient, RadioClient,
    AlbumClient, ArtistClient, PlaylistClient, LoginClient.WebView, TrackerClient, LibraryClient,
    ShareClient, LyricsClient {
    override val metadata = ExtensionMetadata(
        id = "youtube-music",
        name = "Youtube Music",
        version = "1.0.0",
        description = "Youtube Music Extension for Echo, with the help of YTM-kt library.",
        author = "Echo",
        iconUrl = "https://music.youtube.com/img/favicon_144.png".toImageHolder()
    )
    override val settings: List<Setting> = listOf(
        SettingSwitch(
            "High Thumbnail Quality",
            "high_quality",
            "Use high quality thumbnails, will cause more data usage.",
            false
        ), SettingSwitch(
            "Use MP4 Format",
            "use_mp4_format",
            "Use MP4 formats for audio streams, turning it on may cause source errors.",
            false
        )
    )

    private val api = YoutubeiApi()
    private val thumbnailQuality
        get() = if (preferences.getBoolean("high_quality", false)) HIGH else LOW
    private val language = ENGLISH

    private val songFeedEndPoint = EchoSongFeedEndpoint(api)
    private val artistEndPoint = EchoArtistEndpoint(api)
    private val artistMoreEndpoint = EchoArtistMoreEndpoint(api)
    private val libraryEndPoint = EchoLibraryEndPoint(api)
    private val songEndPoint = EchoSongEndPoint(api)
    private val songRelatedEndpoint = EchoSongRelatedEndpoint(api)
    private val videoEndpoint = EchoVideoEndpoint(api)
    private val playlistEndPoint = EchoPlaylistEndpoint(api)
    private val lyricsEndPoint = EchoLyricsEndPoint(api)

    companion object {
        const val ENGLISH = "en-GB"
        const val SINGLES = "Singles"
    }

    private var oldGenre: Genre? = null
    private var feed: SongFeedLoadResult? = null

    override suspend fun getHomeGenres(): List<Genre> {
        val result = songFeedEndPoint.getSongFeed().getOrThrow()
        feed = result
        val genres = result.filter_chips?.map {
            Genre(it.params, it.text.getString(language))
        } ?: return emptyList()
        return listOf(listOf(Genre("null", "All")), genres).flatten()
    }

    override fun getHomeFeed(genre: Genre?) = continuationFlow {
        val params = genre?.id?.takeIf { id -> id != "null" }
        val continuation = if (oldGenre == genre) it else null
        val result = feed?.also {
            feed = null
        } ?: songFeedEndPoint.getSongFeed(
            params = params, continuation = continuation
        ).getOrThrow()

        val data = result.layouts.map { itemLayout ->
            itemLayout.toMediaItemsContainer(api, SINGLES, thumbnailQuality)
        }
        oldGenre = genre
        Page(data, result.ctoken)
    }


    override suspend fun getStreamableAudio(streamable: Streamable) = streamable.id.toAudio()
    override suspend fun getStreamableVideo(streamable: Streamable) =
        StreamableVideo(Request(streamable.id), looping = false, crop = false)

    private val useMp4Format
        get() = preferences.getBoolean("use_mp4_format", false)

    override suspend fun loadTrack(track: Track) = coroutineScope {
        val deferred = async {
            songEndPoint.loadSong(track.id).getOrThrow()
        }
        val video = videoEndpoint.getVideo(track.id).getOrThrow()

        val expiresAt =
            System.currentTimeMillis() + (video.streamingData.expiresInSeconds.toLong() * 1000)

        val formats = if (useMp4Format) video.streamingData.formats?.mapNotNull {
            it.url ?: return@mapNotNull null
            Streamable(it.url, it.bitrate)
        } ?: listOf() else listOf()
        val adaptiveAudio = video.streamingData.adaptiveFormats.mapNotNull {
            if (!it.mimeType.contains("audio")) return@mapNotNull null
            Streamable(it.url, it.bitrate)
        }
        val adaptiveVideo = video.streamingData.adaptiveFormats.mapNotNull {
            if (!it.mimeType.contains("video")) return@mapNotNull null
            Streamable(it.url, it.bitrate)
        }
        val newTrack = deferred.await().toTrack(HIGH)
        newTrack.copy(
            artists = newTrack.artists.ifEmpty {
                video.videoDetails.run { listOf(Artist(channelId, author)) }
            },
            audioStreamables = formats + adaptiveAudio,
            videoStreamable = formats + adaptiveVideo,
            expiresAt = expiresAt,
            plays = video.videoDetails.viewCount?.toIntOrNull()
        )
    }

    private suspend fun loadRelated(track: Track) = track.run {
        val relatedId = extras["relatedId"] ?: throw Exception("No related id found.")
        songFeedEndPoint.getSongFeed(browseId = relatedId).getOrThrow().layouts.map {
            it.toMediaItemsContainer(api, SINGLES, thumbnailQuality)
        }
    }


    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> = PagedData.Single {
        coroutineScope {
            val album = track.album?.let {
                async { listOf(loadAlbum(it).toMediaItem().toMediaItemsContainer()) }
            } ?: async { listOf() }
            val artists = async {
                track.artists.map { loadArtist(it).toMediaItem().toMediaItemsContainer() }
            }
            val related = loadRelated(track)
            album.await() + artists.await() + related
        }
    }

    override suspend fun quickSearch(query: String?) = query?.run {
        try {
            api.SearchSuggestions.getSearchSuggestions(this).getOrThrow()
                .map { QuickSearchItem.SearchQueryItem(it.text, it.is_from_history) }
        } catch (e: NullPointerException) {
            null
        } catch (e: ConnectTimeoutException) {
            null
        }
    } ?: listOf()


    private var oldSearch: Pair<String, List<MediaItemsContainer>>? = null
    override fun search(query: String?, genre: Genre?) = PagedData.Single {
        query ?: return@Single emptyList()
        val old = oldSearch?.takeIf {
            it.first == query && (genre == null || genre.id == "All")
        }?.second
        if (old != null) return@Single old
        val search = api.Search.searchMusic(query, genre?.id).getOrThrow()
        val list = search.categories.map { (itemLayout, _) ->
            itemLayout.items.mapNotNull { item ->
                item.toEchoMediaItem(api, false, thumbnailQuality)?.toMediaItemsContainer()
            }
        }.flatten()
        list
    }

    override suspend fun searchGenres(query: String?): List<Genre> {
        query ?: return emptyList()
        val search = api.Search.searchMusic(query, null).getOrThrow()
        oldSearch = query to search.categories.map { (itemLayout, _) ->
            itemLayout.toMediaItemsContainer(api, SINGLES, thumbnailQuality)
        }
        val genres = search.categories.mapNotNull { (item, filter) ->
            filter?.let {
                Genre(
                    it.params, item.title?.getString(language) ?: "???"
                )
            }
        }
        return listOf(Genre("All", "All")) + genres
    }

    override suspend fun radio(album: Album): Playlist {
        val full =
            api.LoadPlaylist.loadPlaylist(album.id).getOrThrow().toAlbum(false, thumbnailQuality)
        val track = full.tracks.firstOrNull()?.id ?: throw Exception("No tracks found")
        val result = api.SongRadio.getSongRadio(track, null).getOrThrow()
        val tracks = result.items.map {
            it.toTrack(thumbnailQuality)
        }
        return Playlist(
            id = result.continuation ?: album.id,
            title = full.title,
            isEditable = false,
            cover = full.cover,
            authors = listOf(),
            tracks = tracks,
            subtitle = full.description,
            duration = tracks.sumOf { it.duration ?: 0 },
            creationDate = null,
        )
    }

    override suspend fun radio(artist: Artist): Playlist {
        val result = api.ArtistRadio.getArtistRadio(artist.id, null).getOrThrow()
        val tracks = result.items.map {
            it.toTrack(thumbnailQuality)
        }
        return Playlist(
            id = result.continuation ?: artist.id,
            title = artist.name,
            isEditable = false,
            cover = tracks.firstOrNull()?.cover,
            authors = listOf(),
            tracks = tracks,
            subtitle = "Radio based on ${artist.name}",
            duration = tracks.sumOf { it.duration ?: 0 },
            creationDate = null,
        )
    }


    override suspend fun radio(track: Track): Playlist {
        val result = api.SongRadio.getSongRadio(track.id, null).getOrThrow()
        val tracks = result.items.map {
            it.toTrack(thumbnailQuality)
        }
        return Playlist(
            id = result.continuation ?: track.id,
            title = track.title,
            isEditable = false,
            cover = track.cover,
            authors = track.artists,
            tracks = tracks,
            subtitle = "Radio based on ${track.title}",
            duration = tracks.sumOf { it.duration ?: 0 },
            creationDate = null,
        )
    }

    override suspend fun radio(playlist: Playlist): Playlist {
        val channelId = api.user_auth_state?.own_channel_id
        val full = api.LoadPlaylist.loadPlaylist(playlist.id).getOrThrow()
            .toPlaylist(channelId, thumbnailQuality)
        val track = full.tracks.lastOrNull() ?: throw Exception("No tracks")
        return radio(track)
    }

    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> = PagedData.Single {
        album.tracks.lastOrNull()?.let { loadRelated(loadTrack(it)) } ?: emptyList()
    }

    override suspend fun loadAlbum(small: Album): Album {
        val (ytmPlaylist, _) = playlistEndPoint.loadFromPlaylist(small.id)
        return ytmPlaylist.toAlbum(false, HIGH)
    }

    private suspend fun getArtistMediaItems(artist: Artist): List<MediaItemsContainer.Category> {
        val result = loadedArtist.takeIf { artist.id == it?.id }
            ?: api.LoadArtist.loadArtist(artist.id).getOrThrow()

        return result.layouts?.map {
            val title = it.title?.getString(ENGLISH)
            val single = title == SINGLES
            MediaItemsContainer.Category(title = it.title?.getString(language) ?: "Unknown",
                subtitle = it.subtitle?.getString(language),
                list = it.items?.mapNotNull { item ->
                    item.toEchoMediaItem(api, single, thumbnailQuality)
                } ?: emptyList(),
                more = it.view_more?.getBrowseParamsData()?.let { param ->
                    PagedData.Single {
                        val data = artistMoreEndpoint.load(param)
                        data.map { row ->
                            row.items.mapNotNull { item ->
                                item.toEchoMediaItem(api, single, thumbnailQuality)
                            }
                        }.flatten()
                    }
                })
        } ?: emptyList()
    }

    override fun getMediaItems(artist: Artist) = PagedData.Single<MediaItemsContainer> {
        getArtistMediaItems(artist)
    }


    private var loadedArtist: YtmArtist? = null
    override suspend fun loadArtist(small: Artist): Artist {
        val result = artistEndPoint.loadArtist(small.id)
        loadedArtist = result
        return result.toArtist(HIGH)
    }

    override fun getMediaItems(playlist: Playlist) = PagedData.Single {
        val cont = playlist.extras["relatedId"]
            ?: throw Exception("No related id found.")
        val continuation = songRelatedEndpoint.loadFromPlaylist(cont).getOrThrow()
        continuation.map { it.toMediaItemsContainer(api, language, thumbnailQuality) }
    }

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val channelId = api.user_auth_state?.own_channel_id
        val (ytmPlaylist, related) = playlistEndPoint.loadFromPlaylist(playlist.id)
        return ytmPlaylist.toPlaylist(channelId, HIGH, related)
    }

    override val loginWebViewInitialUrl =
        "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin".toRequest()

    override val loginWebViewStopUrlRegex = "https://music\\.youtube\\.com/.*".toRegex()
    override suspend fun onLoginWebviewStop(url: String, cookie: String): List<User> {
        if (!cookie.contains("SAPISID")) throw Exception("Login Failed, could not load SAPISID")
        val auth = run {
            val currentTime = System.currentTimeMillis() / 1000
            val id = cookie.split("SAPISID=")[1].split(";")[0]
            val str = "$currentTime $id https://music.youtube.com"
            val idHash = MessageDigest.getInstance("SHA-1").digest(str.toByteArray())
                .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
            "SAPISIDHASH ${currentTime}_${idHash}"
        }
        val headersMap = mutableMapOf(
            "cookie" to cookie, "authorization" to auth
        )

        val headers = headers {
            headersMap.forEach { (t, u) -> append(t, u) }
        }
        return api.client.request("https://music.youtube.com/getAccountSwitcherEndpoint") {
            headers {
                append("referer", "https://music.youtube.com/")
                appendAll(headers)
            }
        }.getArtists(cookie, auth)
    }


    private var visitorId: String?
        get() = preferences.getString("visitor_id", null)
        set(value) = preferences.edit().putString("visitor_id", value).apply()

    override suspend fun onSetLoginUser(user: User?) {
        if (user == null) {
            api.user_auth_state = null
            api.visitor_id =
                visitorId ?: api.GetVisitorId.getVisitorId().getOrThrow().also { visitorId = it }
            return
        }
        val cookie = user.extras["cookie"] ?: throw Exception("No cookie")
        val auth = user.extras["auth"] ?: throw Exception("No auth")

        val headers = headers {
            append("cookie", cookie)
            append("authorization", auth)
        }
        val authenticationState =
            YoutubeiAuthenticationState(api, headers, user.id.ifEmpty { null })
        api.user_auth_state = authenticationState
    }

    override suspend fun onMarkAsPlayed(clientId: String, track: Track) {
        api.user_auth_state?.MarkSongAsWatched?.markSongAsWatched(track.id)?.getOrThrow()
    }

    override suspend fun onStartedPlaying(clientId: String, track: Track) {}
    override suspend fun onStoppedPlaying(clientId: String, track: Track) {}

    override suspend fun getLibraryGenres() = listOf(
        Genre("FEmusic_library_landing", "All"),
        Genre("FEmusic_history", "History"),
        Genre("FEmusic_liked_playlists", "Playlists"),
        Genre("FEmusic_listening_review", "Review"),
        Genre("FEmusic_liked_videos", "Songs"),
        Genre("FEmusic_library_corpus_track_artists", "Artists")
    )

    suspend fun getFeed(genre: Genre?, it: String?) = run {
        if (api.user_auth_state == null) throw LoginRequiredException.from(this)
        val browseId = genre?.id ?: "FEmusic_library_landing"
        val (result, ctoken) = libraryEndPoint.loadLibraryFeed(browseId, it)
        val data = result.mapNotNull { playlist ->
            playlist.toEchoMediaItem(api, false, thumbnailQuality)?.toMediaItemsContainer()
        }
        Page<MediaItemsContainer>(data, ctoken)
    }

    override fun getLibraryFeed(genre: Genre?) = continuationFlow {
        getFeed(genre, it)
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val auth = api.user_auth_state
            ?: throw LoginRequiredException.from(this)
        val playlistId =
            auth.CreateAccountPlaylist.createAccountPlaylist(title, description ?: "").getOrThrow()
        val playlist = api.LoadPlaylist.loadPlaylist(playlistId).getOrThrow()
        return playlist.toPlaylist(auth.own_channel_id, HIGH)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        val auth = api.user_auth_state
            ?: throw LoginRequiredException.from(this)
        auth.DeleteAccountPlaylist.deleteAccountPlaylist(playlist.id).isSuccess
    }

    override suspend fun likeTrack(track: Track, liked: Boolean): Boolean {
        val likeStatus = if (liked) SongLikedStatus.LIKED else SongLikedStatus.NEUTRAL
        val auth = api.user_auth_state
            ?: throw LoginRequiredException.from(this)
        auth.SetSongLiked.setSongLiked(track.id, likeStatus).getOrThrow()
        return liked
    }

    override suspend fun listEditablePlaylists(): List<Playlist> {
        val auth = api.user_auth_state
            ?: throw LoginRequiredException.from(this)
        return auth.AccountPlaylists.getAccountPlaylists().getOrThrow().map {
            it.toPlaylist(auth.own_channel_id, thumbnailQuality)
        }
    }

    private suspend fun performAction(
        playlist: Playlist,
        actions: List<PlaylistEditor.Action>
    ): Boolean {
        val auth = api.user_auth_state
            ?: throw LoginRequiredException.from(this)
        val sets = playlist.extras["item_set_ids"]?.split(",")
            ?: throw Exception("No item set ids found")
        val editor =
            auth.AccountPlaylistEditor.getEditor(playlist.id, playlist.tracks.map { it.id }, sets)
        val res = editor.performAndCommitActions(actions)
        return res.isSuccess
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?
    ) {
        performAction(playlist, listOfNotNull(
            PlaylistEditor.Action.SetTitle(title),
            description?.let { PlaylistEditor.Action.SetDescription(it) }
        ))
    }

    override suspend fun removeTracksFromPlaylist(playlist: Playlist, tracks: List<Track>) {
        performAction(
            playlist,
            tracks.map { PlaylistEditor.Action.Remove(playlist.tracks.indexOf(it)) }
        )
    }

    override suspend fun addTracksToPlaylist(playlist: Playlist, index: Int?, tracks: List<Track>) {
        performAction(playlist, tracks.map { PlaylistEditor.Action.Add(it.id, index) })
    }

    override suspend fun moveTrackInPlaylist(playlist: Playlist, fromIndex: Int, toIndex: Int) {
        performAction(playlist, listOf(PlaylistEditor.Action.Move(fromIndex, toIndex)))
    }

    override suspend fun onShare(album: Album) =
        "https://music.youtube.com/browse/${album.id}"

    override suspend fun onShare(artist: Artist) =
        "https://music.youtube.com/channel/${artist.id}"

    override suspend fun onShare(playlist: Playlist) =
        "https://music.youtube.com/playlist?list=${playlist.id}"

    override suspend fun onShare(track: Track) =
        "https://music.youtube.com/watch?v=${track.id}"

    override suspend fun onShare(user: User) =
        throw IllegalAccessException()

    override suspend fun getLyrics(item: LyricsItem) = lyricsCache[item.id]!!
    private val lyricsCache = mutableMapOf<String, List<Lyric>>()

    override suspend fun searchTrackLyrics(clientId: String, track: Track) = PagedData.Single {
        val lyricsId = track.extras["lyricsId"] ?: return@Single listOf()
        val data = lyricsEndPoint.getLyrics(lyricsId) ?: return@Single listOf()
        val lyrics = data.first.map {
            it.cueRange.run {
                Lyric(it.lyricLine, startTimeMilliseconds.toLong(), endTimeMilliseconds.toLong())
            }
        }
        lyricsCache[lyricsId] = lyrics
        listOf(LyricsItem(lyricsId, track.title, data.second))
    }
}