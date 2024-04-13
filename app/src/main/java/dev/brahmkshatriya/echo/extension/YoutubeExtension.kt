package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.extension.endpoints.EchoLoadAlbumEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLoadSongEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoPlaylistSectionListEndpoint
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsRow
import dev.toastbits.ytmkt.endpoint.SongFeedLoadResult
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.LOW
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.headers
import io.ktor.util.flattenEntries
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.security.MessageDigest

class YoutubeExtension : ExtensionClient(), HomeFeedClient, TrackClient, SearchClient, RadioClient,
    AlbumClient, ArtistClient, PlaylistClient, LoginClient.WebView {
    override val metadata = ExtensionMetadata(
        id = "youtube-music",
        name = "Youtube Music",
        version = "1.0.0",
        description = "Youtube Music Extension for Echo, with the help of YTM-kt library.",
        author = "Echo",
        iconUrl = "https://music.youtube.com/img/favicon_144.png".toImageHolder()
    )
    override val settings: List<Setting> = listOf()

    private val api = YoutubeiApi()
    private val loadSongEndPoint = EchoLoadSongEndPoint(api)
    private val loadAlbumEndPoint = EchoLoadAlbumEndPoint(api)
    private val loadPlaylistSectionListEndpoint = EchoPlaylistSectionListEndpoint(api)

    private val thumbnailQuality = LOW
    private val language = english

    companion object {
        const val english = "en-GB"
        const val singles = "Singles"
    }


    private var initialized = false
    private var visitorId: String?
        get() = preferences.getString("visitor_id", null)
        set(value) = preferences.edit().putString("visitor_id", value).apply()


    private var oldGenre: Genre? = null
    private var feed: SongFeedLoadResult? = null

    override suspend fun getHomeGenres(): List<Genre> {
        if (!initialized) {
            api.visitor_id =
                visitorId ?: api.GetVisitorId.getVisitorId().getOrThrow().also { visitorId = it }
            initialized = true
        }

        val result = api.SongFeed.getSongFeed().getOrThrow()
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
        } ?: api.SongFeed.getSongFeed(
            params = params,
            continuation = continuation
        ).getOrThrow()

        val data = result.layouts.map { itemLayout ->
            itemLayout.toMediaItemsContainer(api, singles, thumbnailQuality)
        }
        oldGenre = genre
        Page(data, result.ctoken)
    }


    override suspend fun getStreamableAudio(streamable: Streamable) = streamable.id.toAudio()

    override suspend fun loadTrack(track: Track) = coroutineScope {
        val newTrack = async {
            loadSongEndPoint.loadSong(track.id).getOrThrow()
        }
        val video = async {
            api.VideoFormats.getVideoFormats(track.id).getOrThrow()
        }
        newTrack.await().toTrack(HIGH).copy(
            streamables = video.await().mapNotNull {
                val url = it.url ?: return@mapNotNull null
                if (!it.mimeType.contains("audio")) return@mapNotNull null
                Streamable(url, it.bitrate)
            }
        )
    }

    private suspend fun loadRelated(track: Track) =
        api.SongRelatedContent.getSongRelated(track.id).getOrThrow()
            .mapNotNull { it.toMediaItemsContainer(thumbnailQuality) }


    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> = PagedData.Single {
        val album = track.album?.let {
            loadAlbum(it).toMediaItem().toMediaItemsContainer()
        }
        val albumItem = listOfNotNull(album)
        val artists = track.artists.map {
            loadArtist(it).toMediaItem().toMediaItemsContainer()
        }
        val related = loadRelated(track)
        albumItem + artists + related
    }

    override suspend fun quickSearch(query: String?) = query?.run {
        try {
            api.SearchSuggestions.getSearchSuggestions(this).getOrThrow()
                .map { QuickSearchItem.SearchQueryItem(it.text, it.is_from_history) }
        } catch (e: NullPointerException) {
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
                item.toEchoMediaItem(false, thumbnailQuality)?.toMediaItemsContainer()
            }
        }.flatten()
        list
    }

    override suspend fun searchGenres(query: String?): List<Genre> {
        query ?: return emptyList()
        val search = api.Search.searchMusic(query, null).getOrThrow()
        oldSearch = query to search.categories.map { (itemLayout, _) ->
            itemLayout.toMediaItemsContainer(api, singles, thumbnailQuality)
        }
        val genres = search.categories.mapNotNull { (item, filter) ->
            filter?.let {
                Genre(
                    it.params,
                    item.title?.getString(language) ?: "???"
                )
            }
        }
        return listOf(Genre("All", "All")) + genres
    }

    override suspend fun radio(album: Album): Playlist {
        val full = api.LoadPlaylist.loadPlaylist(album.id).getOrThrow()
            .toAlbum(false, thumbnailQuality)
        val track = full.tracks.firstOrNull()?.id ?: throw Exception("No tracks found")
        val result = api.SongRadio.getSongRadio(track, null).getOrThrow()
        val tracks = result.items.map {
            it.toTrack(thumbnailQuality)
        }
        return Playlist(
            id = result.continuation ?: album.id,
            title = full.title,
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
            cover = track.cover,
            authors = track.artists,
            tracks = tracks,
            subtitle = "Radio based on ${track.title}",
            duration = tracks.sumOf { it.duration ?: 0 },
            creationDate = null,
        )
    }

    override suspend fun radio(playlist: Playlist): Playlist {
        val full = api.LoadPlaylist.loadPlaylist(playlist.id).getOrThrow()
            .toPlaylist(thumbnailQuality)
        val track = full.tracks.lastOrNull() ?: throw Exception("No tracks")
        return radio(track)
    }

    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> = PagedData.Single {
        album.tracks.lastOrNull()?.let { loadRelated(it) } ?: emptyList()
    }

    override suspend fun loadAlbum(small: Album): Album {
        return loadAlbumEndPoint.loadPlaylist(small.id).getOrThrow()
            .toAlbum(false, HIGH)
    }

    private suspend fun getArtistMediaItems(artist: Artist): List<MediaItemsContainer.Category> {
        val result = api.LoadArtist.loadArtist(artist.id).getOrThrow()
        val list = result.layouts?.map {
            val title = it.title?.getString(english)
            val single = title == singles
            MediaItemsContainer.Category(
                title = it.title?.getString(language) ?: "Unknown",
                subtitle = it.subtitle?.getString(language),
                list = it.items?.mapNotNull { item ->
                    item.toEchoMediaItem(single, thumbnailQuality)
                } ?: emptyList(),
                more = it.view_more?.getBrowseParamsData()?.let { param ->
                    continuationFlow {
                        val rows =
                            api.ArtistWithParams.loadArtistWithParams(param).getOrThrow()
                        val data = rows.map { itemLayout ->
                            itemLayout.toMediaItems()
                        }.flatten()
                        Page(data, null)
                    }
                }
            )
        } ?: emptyList()
        return list
    }

    override fun getMediaItems(artist: Artist) = PagedData.Single<MediaItemsContainer> {
        getArtistMediaItems(artist)
    }


    override suspend fun loadArtist(small: Artist): Artist {
        val result = api.LoadArtist.loadArtist(small.id).getOrThrow()
        return result.toArtist(HIGH)
    }


    private fun ArtistWithParamsRow.toMediaItems(): List<EchoMediaItem> {
        return items.mapNotNull { item ->
            item.toEchoMediaItem(title == singles, thumbnailQuality)
        }
    }

    override fun getMediaItems(playlist: Playlist) = PagedData.Single<MediaItemsContainer> {
        val cont = playlist.extras["cont"] ?: return@Single emptyList()
        val continuation = loadPlaylistSectionListEndpoint.loadFromPlaylist(cont).getOrThrow()
        continuation.mapNotNull { it.toMediaItemsContainer(thumbnailQuality) }
    }

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return api.LoadPlaylist.loadPlaylist(playlist.id).getOrThrow().toPlaylist(HIGH)
    }

    override val loginWebViewInitialUrl =
        "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
            .toRequest()

    override val loginWebViewStopUrlRegex = "https://music\\.youtube\\.com/.*".toRegex()
    override suspend fun onLoginWebviewStop(url: String, cookie: String): List<User> {
        if (!cookie.contains("SAPISID"))
            throw Exception("Login Failed, could not load SAPISID")
        val auth = run {
            val currentTime = System.currentTimeMillis() / 1000
            val id = cookie.split("SAPISID=")[1].split(";")[0]
            val idHash =
                sha1("$currentTime $id https://music.youtube.com")
            "SAPISIDHASH ${currentTime}_${idHash}"
        }
        val headersMap = mutableMapOf(
            "cookie" to cookie,
            "authorization" to auth
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

    override suspend fun onSetLoginUser(user: User) {
        val cookie = user.extras["cookie"]
            ?: throw Exception("No cookie")
        val auth = user.extras["auth"]
            ?: throw Exception("No auth")
        val signInUrl = user.extras["signInUrl"]
            ?: throw Exception("No sign in url")

        val response = api.client.get("https://music.youtube.com/$signInUrl") {
            expectSuccess = true
            headers {
                append("cookie", cookie)
                append("authorization", auth)
            }
        }

        val newCookies = response.headers.flattenEntries().mapNotNull { header ->
            if (header.first.lowercase() == "set-cookie") header.second
            else null
        }
        val headers = headers {
            append("cookie", replaceCookiesInString(cookie, newCookies))
            append("authorization", auth)
        }
        val authenticationState =
            YoutubeiAuthenticationState(api, headers, user.id.ifEmpty { null })
        api.user_auth_state = authenticationState
    }

    private fun replaceCookiesInString(baseCookies: String, newCookies: List<String>): String {
        var cookieString: String = baseCookies

        for (cookie in newCookies) {
            val split: List<String> = cookie.split('=', limit = 2)

            val name: String = split[0]
            val newValue: String = split[1].split(';', limit = 2)[0]

            val cookieStart: Int = cookieString.indexOf("$name=") + name.length + 1
            if (cookieStart != -1) {
                val cookieEnd: Int = cookieString.indexOf(';', cookieStart)
                val end = if (cookieEnd != -1)
                    cookieString.substring(cookieEnd, cookieString.length)
                else ""
                cookieString = (cookieString.substring(0, cookieStart) + newValue + end)
            } else cookieString += "; $name=$newValue"
        }

        return cookieString

    }

    private fun sha1(str: String) =
        MessageDigest.getInstance("SHA-1").digest(str.toByteArray())
            .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

}