package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
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
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ClientException
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Lyric
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Audio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toVideoMedia
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.endpoints.EchoArtistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoArtistMoreEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoEditPlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLibraryEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLyricsEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoPlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSearchEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSearchSuggestionsEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongFeedEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongRelatedEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoVideoEndpoint
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.PlaylistEditor
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.LOW
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.headers
import io.ktor.http.takeFrom
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.security.MessageDigest

class YoutubeExtension : ExtensionClient, HomeFeedClient, TrackClient, SearchClient, RadioClient,
    AlbumClient, ArtistClient, UserClient, PlaylistClient, LoginClient.WebView.Cookie,
    TrackerClient,
    LibraryClient, ShareClient, LyricsClient, ArtistFollowClient {

    override val settingItems: List<Setting> = listOf(
        SettingSwitch(
            "High Thumbnail Quality",
            "high_quality",
            "Use high quality thumbnails, will cause more data usage.",
            false
        ), SettingSwitch(
            "Use MP4 Format",
            "use_mp4_format",
            "Use MP4 formats for audio streams, will turn off video & allow you to download music. ",
            false
        ), SettingSwitch(
            "Resolve Music for Videos",
            "resolve_music_for_videos",
            "Resolve actual music metadata for music videos, does slow down loading music videos.",
            true
        )
    )

    private lateinit var settings: Settings
    override fun setSettings(settings: Settings) {
        this.settings = settings
    }

    override suspend fun onExtensionSelected() {}

    val api = YoutubeiApi()
    private val thumbnailQuality
        get() = if (settings.getBoolean("high_quality") == true) HIGH else LOW

    private val useMp4Format
        get() = settings.getBoolean("use_mp4_format") ?: false

    private val resolveMusicForVideos
        get() = settings.getBoolean("resolve_music_for_videos") ?: true

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
    private val searchSuggestionsEndpoint = EchoSearchSuggestionsEndpoint(api)
    private val searchEndpoint = EchoSearchEndpoint(api)
    private val editorEndpoint = EchoEditPlaylistEndpoint(api)

    companion object {
        const val ENGLISH = "en-GB"
        const val SINGLES = "Singles"
        const val SONGS = "songs"
    }

    override suspend fun getHomeTabs() = listOf<Tab>()

    override fun getHomeFeed(tab: Tab?) = PagedData.Continuous {
        val continuation = it
        val result = songFeedEndPoint.getSongFeed(
            params = null, continuation = continuation
        ).getOrThrow()
        val data = result.layouts.map { itemLayout ->
            itemLayout.toMediaItemsContainer(api, SINGLES, thumbnailQuality)
        }
        Page(data, result.ctoken)
    }

    override suspend fun getStreamableMedia(streamable: Streamable) = when (streamable.mediaType) {
        Streamable.MediaType.Audio -> streamable.id.toAudio().toMedia()
        Streamable.MediaType.Video -> streamable.id.toVideoMedia()
        Streamable.MediaType.AudioVideo ->
            throw IllegalArgumentException("AudioVideo not supported")
    }


    private suspend fun searchSongForVideo(track: Track): Track? {
        val result = searchEndpoint.search(
            "${track.title} ${track.artists.joinToString(" ") { it.name }}",
            "EgWKAQIIAWoSEAMQBBAJEA4QChAFEBEQEBAV",
            false
        ).getOrThrow().categories.firstOrNull()?.first?.items?.firstOrNull() ?: return null
        val mediaItem =
            result.toEchoMediaItem(api, false, thumbnailQuality) as EchoMediaItem.TrackItem
        println("${mediaItem.title} : ${mediaItem.title != track.title}")
        if (mediaItem.title != track.title) return null
        val newTrack = songEndPoint.loadSong(mediaItem.id).getOrThrow().toTrack(HIGH)
        return newTrack
    }

    override suspend fun loadTrack(track: Track) = coroutineScope {
        val deferred = async { videoEndpoint.getVideo(track.id).getOrThrow() }
        var newTrack = songEndPoint.loadSong(track.id).getOrThrow().toTrack(HIGH)
        val video = deferred.await()
        val expiresAt =
            System.currentTimeMillis() + (video.streamingData.expiresInSeconds.toLong() * 1000)
        val isMusic = video.streamingData.aspectRatio == 1.0
        val streamables = if (useMp4Format) video.streamingData.adaptiveFormats.mapNotNull {
            if (!it.mimeType.contains("audio")) return@mapNotNull null
            Streamable.audio(it.url, it.bitrate)
        } else getHlsStreams(video.streamingData.hlsManifestUrl, isMusic)
        if (resolveMusicForVideos && !isMusic) {
            val result = searchSongForVideo(newTrack)
            newTrack = result ?: newTrack
        }
        newTrack.copy(
            id = video.videoDetails.videoId,
            artists = newTrack.artists.ifEmpty {
                video.videoDetails.run { listOf(Artist(channelId, author)) }
            },
            streamables = streamables,
            expiresAt = expiresAt,
            plays = video.videoDetails.viewCount?.toIntOrNull()
        )
    }

    private val audioRegex = Regex("#EXT-X-MEDIA:URI=\"(.*)\",TYPE=AUDIO,GROUP-ID=\"(.*)\",NAME")
    private val videoRegex = Regex("#EXT-X-STREAM-INF:.*,RESOLUTION=\\d+x(\\d+),.*\\n(.*)")
    private suspend fun getHlsStreams(hlsManifestUrl: String, isMusic: Boolean): List<Streamable> {
        val res = api.client.request { url.takeFrom(hlsManifestUrl) }.bodyAsText()
        val audio = audioRegex.findAll(res).map {
            Streamable.audio(it.groupValues[1], it.groupValues[2].toInt(), Streamable.MimeType.HLS)
        }.toList()
        val video = videoRegex.findAll(res).map {
            Streamable.video(it.groupValues[2], it.groupValues[1].toInt(), Streamable.MimeType.HLS)
        }.toList()
        return if (isMusic) audio else (audio + video)
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


    override suspend fun deleteSearchHistory(query: QuickSearchItem.SearchQueryItem) {
        searchSuggestionsEndpoint.delete(query.query)
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
    override fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer> =
        if (query != null) PagedData.Single {
            val old = oldSearch?.takeIf {
                it.first == query && (tab == null || tab.id == "All")
            }?.second
            if (old != null) return@Single old
            val search = api.Search.search(query, tab?.id).getOrThrow()
            val list = search.categories.map { (itemLayout, _) ->
                itemLayout.items.mapNotNull { item ->
                    item.toEchoMediaItem(api, false, thumbnailQuality)?.toMediaItemsContainer()
                }
            }.flatten()
            list
        } else if (tab != null) PagedData.Continuous {
            val params = tab.id
            val continuation = it
            val result = songFeedEndPoint.getSongFeed(
                params = params, continuation = continuation
            ).getOrThrow()
            val data = result.layouts.map { itemLayout ->
                itemLayout.toMediaItemsContainer(api, SINGLES, thumbnailQuality)
            }
            Page(data, result.ctoken)
        } else PagedData.Single { listOf() }

    override suspend fun searchTabs(query: String?): List<Tab> {
        if (query != null) {
            val search = api.Search.search(query, null).getOrThrow()
            oldSearch = query to search.categories.map { (itemLayout, _) ->
                itemLayout.toMediaItemsContainer(api, SINGLES, thumbnailQuality)
            }
            val tabs = search.categories.mapNotNull { (item, filter) ->
                filter?.let {
                    Tab(
                        it.params, item.title?.getString(language) ?: "???"
                    )
                }
            }
            return listOf(Tab("All", "All")) + tabs
        } else {
            val result = songFeedEndPoint.getSongFeed().getOrThrow()
            return result.filter_chips?.map {
                Tab(it.params, it.text.getString(language))
            } ?: emptyList()
        }
    }

    override suspend fun radio(album: Album): Playlist {
        val track =
            api.LoadPlaylist.loadPlaylist(album.id).getOrThrow().items?.lastOrNull()?.toTrack(HIGH)
                ?: throw Exception("No tracks found")
        return radio(track)
    }

    override suspend fun radio(artist: Artist): Playlist {
        val data = PagedData.Continuous {
            val result = api.ArtistRadio.getArtistRadio(artist.id, it).getOrThrow()
            val tracks = result.items.map { song -> song.toTrack(thumbnailQuality) }
            Page(tracks, result.continuation)
        }

        val id = "radio_${data.hashCode()}"
        trackMap[id] = data
        return Playlist(
            id = id,
            title = "${artist.name} Radio",
            isEditable = false,
            cover = artist.cover,
            authors = listOf(),
        )
    }


    override suspend fun radio(track: Track): Playlist {
        val data = PagedData.Continuous {
            val result = api.SongRadio.getSongRadio(track.id, it).getOrThrow()
            val tracks = result.items.map { song -> song.toTrack(thumbnailQuality) }
            Page(tracks, result.continuation)
        }

        val id = "radio_${data.hashCode()}"
        trackMap[id] = data
        return Playlist(
            id = id,
            title = "${track.title} Radio",
            isEditable = false,
            cover = track.cover,
            authors = listOf(),
        )
    }

    override suspend fun radio(user: User) = radio(user.toArtist())

    override suspend fun radio(playlist: Playlist): Playlist {
        val track = api.LoadPlaylist.loadPlaylist(playlist.id).getOrThrow().items?.lastOrNull()
            ?.toTrack(HIGH) ?: throw Exception("No tracks found")
        return radio(track)
    }

    private val trackMap = mutableMapOf<String, PagedData<Track>>()
    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> = PagedData.Single {
        trackMap[album.id]?.loadAll()?.lastOrNull()?.let { loadRelated(loadTrack(it)) }
            ?: emptyList()
    }

    override suspend fun loadAlbum(small: Album): Album {
        val (ytmPlaylist, _, data) = playlistEndPoint.loadFromPlaylist(
            small.id, null, thumbnailQuality
        )
        trackMap[ytmPlaylist.id] = data
        return ytmPlaylist.toAlbum(false, HIGH)
    }

    override fun loadTracks(album: Album): PagedData<Track> = trackMap[album.id]!!

    private suspend fun getArtistMediaItems(artist: Artist): List<MediaItemsContainer.Category> {
        val result =
            loadedArtist.takeIf { artist.id == it?.id } ?: api.LoadArtist.loadArtist(artist.id)
                .getOrThrow()

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

    override fun getMediaItems(it: User) = getMediaItems(it.toArtist())

    override suspend fun loadUser(user: User): User {
        loadArtist(user.toArtist())
        return loadedArtist!!.toUser(HIGH)
    }

    private var loadedArtist: YtmArtist? = null
    override suspend fun loadArtist(small: Artist): Artist {
        val result = artistEndPoint.loadArtist(small.id)
        loadedArtist = result
        return result.toArtist(HIGH)
    }

    override fun getMediaItems(playlist: Playlist) = PagedData.Single {
        val cont = playlist.extras["relatedId"] ?: throw Exception("No related id found.")
        if (cont.startsWith("id://")) {
            val id = cont.substring(5)
            getMediaItems(loadTrack(Track(id, ""))).loadFirst()
                .filterIsInstance<MediaItemsContainer.Category>()
        } else {
            val continuation = songRelatedEndpoint.loadFromPlaylist(cont).getOrThrow()
            continuation.map { it.toMediaItemsContainer(api, language, thumbnailQuality) }
        }
    }


    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val (ytmPlaylist, related, data) = playlistEndPoint.loadFromPlaylist(
            playlist.id,
            null,
            thumbnailQuality
        )
        trackMap[ytmPlaylist.id] = data
        return ytmPlaylist.toPlaylist(HIGH, related)
    }

    override fun loadTracks(playlist: Playlist): PagedData<Track> = trackMap[playlist.id]!!


    override val loginWebViewInitialUrl =
        "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin".toRequest()

    override val loginWebViewStopUrlRegex = "https://music\\.youtube\\.com/.*".toRegex()

    override suspend fun onLoginWebviewStop(url: String, data: String): List<User> {
        if (!data.contains("SAPISID")) throw Exception("Login Failed, could not load SAPISID")
        val auth = run {
            val currentTime = System.currentTimeMillis() / 1000
            val id = data.split("SAPISID=")[1].split(";")[0]
            val str = "$currentTime $id https://music.youtube.com"
            val idHash = MessageDigest.getInstance("SHA-1").digest(str.toByteArray())
                .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
            "SAPISIDHASH ${currentTime}_${idHash}"
        }
        val headersMap = mutableMapOf(
            "cookie" to data, "authorization" to auth
        )

        val headers = headers {
            headersMap.forEach { (t, u) -> append(t, u) }
        }
        return api.client.request("https://music.youtube.com/getAccountSwitcherEndpoint") {
            headers {
                append("referer", "https://music.youtube.com/")
                appendAll(headers)
            }
        }.getUsers(data, auth)
    }


    private var visitorId: String?
        get() = settings.getString("visitor_id")
        set(value) = settings.putString("visitor_id", value)

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

    override suspend fun getCurrentUser(): User? {
        val headers = api.user_auth_state?.headers ?: return null
        return api.client.request("https://music.youtube.com/getAccountSwitcherEndpoint") {
            headers {
                append("referer", "https://music.youtube.com/")
                appendAll(headers)
            }
        }.getUsers("", "").firstOrNull()
    }


    override suspend fun onMarkAsPlayed(clientId: String, context: EchoMediaItem?, track: Track) {
        api.user_auth_state?.MarkSongAsWatched?.markSongAsWatched(track.id)?.getOrThrow()
    }

    override suspend fun onStartedPlaying(
        clientId: String, context: EchoMediaItem?, track: Track
    ) {
    }

    override suspend fun onStoppedPlaying(
        clientId: String, context: EchoMediaItem?, track: Track
    ) {
    }

    override suspend fun getLibraryTabs() = listOf(
        Tab("FEmusic_library_landing", "All"),
        Tab("FEmusic_history", "History"),
        Tab("FEmusic_liked_playlists", "Playlists"),
//        Tab("FEmusic_listening_review", "Review"),
        Tab("FEmusic_liked_videos", "Songs"),
        Tab("FEmusic_library_corpus_track_artists", "Artists")
    )

    private suspend fun <T> withUserAuth(
        block: suspend (auth: YoutubeiAuthenticationState) -> T
    ): T {
        val state = api.user_auth_state
            ?: throw ClientException.LoginRequired()
        return runCatching { block(state) }.getOrElse {
            if (it is ClientRequestException) {
                if (it.response.status.value == 401) {
                    val user = state.own_channel_id
                        ?: throw ClientException.LoginRequired()
                    throw ClientException.Unauthorized(user)
                }
            }
            throw it
        }
    }

    override fun getLibraryFeed(tab: Tab?) = PagedData.Continuous<MediaItemsContainer> { cont ->
        val browseId = tab?.id ?: "FEmusic_library_landing"
        val (result, ctoken) = withUserAuth { libraryEndPoint.loadLibraryFeed(browseId, cont) }
        val data = result.mapNotNull { playlist ->
            playlist.toEchoMediaItem(api, false, thumbnailQuality)?.toMediaItemsContainer()
        }
        Page(data, ctoken)
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val auth = api.user_auth_state ?: throw ClientException.LoginRequired()
        val playlistId =
            withUserAuth {
                auth.CreateAccountPlaylist
                    .createAccountPlaylist(title, description ?: "")
                    .getOrThrow()
            }
        return loadPlaylist(Playlist(playlistId, "", true))
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        withUserAuth {
            it.DeleteAccountPlaylist.deleteAccountPlaylist(playlist.id).isSuccess
        }

    }

    override suspend fun likeTrack(track: Track, liked: Boolean): Boolean {
        val likeStatus = if (liked) SongLikedStatus.LIKED else SongLikedStatus.NEUTRAL
        withUserAuth { it.SetSongLiked.setSongLiked(track.id, likeStatus).getOrThrow() }
        return liked
    }

    override suspend fun listEditablePlaylists(): List<Playlist> = withUserAuth { auth ->
        auth.AccountPlaylists.getAccountPlaylists().getOrThrow().mapNotNull {
            if (it.id != "VLSE") it.toPlaylist(thumbnailQuality)
            else null
        }
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?
    ) {
        withUserAuth { auth ->
            val editor = auth.AccountPlaylistEditor.getEditor(playlist.id, listOf(), listOf())
            editor.performAndCommitActions(
                listOfNotNull(
                    PlaylistEditor.Action.SetTitle(title),
                    description?.let { PlaylistEditor.Action.SetDescription(it) }
                )
            )
        }
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist, tracks: List<Track>, indexes: List<Int>
    ) {
        val actions = indexes.map {
            val track = tracks[it]
            EchoEditPlaylistEndpoint.Action.Remove(track.id, track.extras["setId"]!!)
        }
        editorEndpoint.editPlaylist(playlist.id, actions)
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>
    ) {
        val actions = new.map { EchoEditPlaylistEndpoint.Action.Add(it.id) }
        val setIds = editorEndpoint.editPlaylist(playlist.id, actions).playlistEditResults!!.map {
            it.playlistEditVideoAddedResultData.setVideoId
        }
        val addBeforeTrack = tracks.getOrNull(index)?.extras?.get("setId") ?: return
        val moveActions = setIds.map { setId ->
            EchoEditPlaylistEndpoint.Action.Move(setId, addBeforeTrack)
        }
        editorEndpoint.editPlaylist(playlist.id, moveActions)
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int
    ) {
        val setId = tracks[fromIndex].extras["setId"]!!
        val before = if (fromIndex - toIndex > 0) 0 else 1
        val addBeforeTrack = tracks.getOrNull(toIndex + before)?.extras?.get("setId")
            ?: return
        editorEndpoint.editPlaylist(
            playlist.id, listOf(
                EchoEditPlaylistEndpoint.Action.Move(setId, addBeforeTrack)
            )
        )
    }

    override suspend fun onShare(album: Album) = "https://music.youtube.com/browse/${album.id}"

    override suspend fun onShare(artist: Artist) = "https://music.youtube.com/channel/${artist.id}"

    override suspend fun onShare(playlist: Playlist) =
        "https://music.youtube.com/playlist?list=${playlist.id}"

    override suspend fun onShare(track: Track) = "https://music.youtube.com/watch?v=${track.id}"

    override suspend fun onShare(user: User) = "https://music.youtube.com/channel/${user.id}"

    override fun searchTrackLyrics(clientId: String, track: Track) = PagedData.Single {
        val lyricsId = track.extras["lyricsId"] ?: return@Single listOf()
        val data = lyricsEndPoint.getLyrics(lyricsId) ?: return@Single listOf()
        val lyrics = data.first.map {
            it.cueRange.run {
                Lyric(it.lyricLine, startTimeMilliseconds.toLong(), endTimeMilliseconds.toLong())
            }
        }
        listOf(Lyrics(lyricsId, track.title, data.second, lyrics))
    }

    override suspend fun loadLyrics(small: Lyrics) = small
    override suspend fun followArtist(artist: Artist): Boolean {
        withUserAuth { it.SetSubscribedToArtist.setSubscribedToArtist(artist.id, true) }
        return true
    }

    override suspend fun unfollowArtist(artist: Artist): Boolean {
        withUserAuth { it.SetSubscribedToArtist.setSubscribedToArtist(artist.id, false) }
        return true
    }
}