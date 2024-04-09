package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsRow
import dev.toastbits.ytmkt.endpoint.SongFeedLoadResult
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong

class YoutubeExtension : ExtensionClient(), HomeFeedClient, TrackClient, SearchClient, RadioClient,
    AlbumClient, ArtistClient, PlaylistClient {
    override val metadata = ExtensionMetadata(
        id = "youtube",
        name = "Youtube",
        version = "1.0.0",
        description = "Youtube Music Extension for Echo, with the help of YTM-kt library.",
        author = "Echo",
        iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/Youtube_Music_icon.svg/128px-Youtube_Music_icon.svg.png".toImageHolder()
    )
    override val settings: List<Setting> = listOf()

    private val api = YoutubeiApi()
    private var initialized = false

    private val english = "en-GB"
    private val singles = "Singles"

    private val thumbnailQuality = ThumbnailProvider.Quality.HIGH
    private val language = english

    private var oldGenre: Genre? = null
    private var feed: SongFeedLoadResult? = null
    override suspend fun getHomeGenres(): List<Genre> {
        if (!initialized) {
            api.getNewVisitorId()
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
            itemLayout.toMediaItemsContainer()
        }
        oldGenre = genre
        Page(data, result.ctoken)
    }

    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> {
        TODO("Not yet implemented")
    }

    override suspend fun getStreamableAudio(streamable: Streamable): StreamableAudio {
        val id = streamable.id
        return api.VideoFormats.getVideoFormats(id).getOrNull()
            ?.sortedBy { it.bitrate }
            ?.find { format ->
                (format.url != null) && format.mimeType.startsWith("audio/mp4")
            }?.url?.toAudio() ?: throw Exception("No audio found")
    }

    override suspend fun loadTrack(track: Track): Track {
        val song = api.LoadSong.loadSong(track.id).getOrThrow()
        return song.toTrack()
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
                item.toEchoMediaItem(false)?.toMediaItemsContainer()
            }
        }.flatten()
        list
    }

    override suspend fun searchGenres(query: String?): List<Genre> {
        query ?: return emptyList()
        val search = api.Search.searchMusic(query, null).getOrThrow()
        oldSearch = query to search.categories.map { (itemLayout, _) ->
            itemLayout.toMediaItemsContainer()
        }
        val genres = search.categories.mapNotNull { (_, filter) ->
            filter?.let {
                Genre(
                    it.params,
                    it.type.name.lowercase().replaceFirstChar { char -> char.uppercase() })
            }
        }
        return listOf(Genre("All", "All")) + genres
    }

    private fun MediaItemLayout.toMediaItemsContainer(): MediaItemsContainer {
        val s = title?.getString(english)
        val single = s == singles
        return MediaItemsContainer.Category(
            title = title?.getString(language) ?: "Unknown",
            subtitle = subtitle?.getString(language),
            list = items.mapNotNull { item ->
                item.toEchoMediaItem(single)
            },
            more = continuationFlow { _ ->
                val id = view_more?.getBrowseParamsData()?.browse_id
                val rows =
                    id?.let {
                        api.GenericFeedViewMorePage.getGenericFeedViewMorePage(id).getOrThrow()
                    }
                val data = rows?.mapNotNull { itemLayout ->
                    itemLayout.toEchoMediaItem(false)
                } ?: emptyList()
                Page(data, null)
            }
        )
    }

    private fun YtmMediaItem.toEchoMediaItem(single: Boolean): EchoMediaItem? {
        return when (this) {
            is YtmSong -> EchoMediaItem.TrackItem(toTrack())
            is YtmPlaylist -> when (type) {
                YtmPlaylist.Type.ALBUM -> EchoMediaItem.Lists.AlbumItem(toAlbum(single))
                else -> EchoMediaItem.Lists.PlaylistItem(toPlaylist())
            }

            is YtmArtist -> EchoMediaItem.Profile.ArtistItem(toArtist())
            else -> null
        }
    }

    private fun YtmPlaylist.toPlaylist(): Playlist {
        return Playlist(
            id = id,
            title = name ?: "Unknown",
            cover = thumbnail_provider?.getThumbnailUrl(
                thumbnailQuality
            )?.toImageHolder(mapOf()),
            authors = artists?.map {
                it.toArtist()
            } ?: emptyList(),
            tracks = items?.map { it.toTrack() } ?: emptyList(),
            subtitle = description,
            duration = total_duration,
            creationDate = year?.toString(),
        )
    }

    private fun YtmSong.toTrack(): Track {
        val album = album?.toAlbum()
        return Track(
            id = id,
            title = name ?: "Unknown",
            artists = artists?.map {
                it.toArtist()
            } ?: emptyList(),
            streamable = Streamable(id),
            cover = getCover(id),
            album = album,
            duration = duration,
            plays = null,
            releaseDate = album?.releaseDate,
            liked = false,
        )
    }

    private fun YtmArtist.toArtist(): Artist {
        return Artist(
            id = id,
            name = name ?: "Unknown",
            cover = thumbnail_provider?.getThumbnailUrl(
                thumbnailQuality
            )?.toImageHolder(mapOf()),
            description = description,
            followers = subscriber_count,
        )
    }

    private fun YtmPlaylist.toAlbum(single: Boolean = false): Album {
        return Album(
            id = id,
            title = name ?: "Unknown",
            cover = thumbnail_provider?.getThumbnailUrl(
                thumbnailQuality
            )?.toImageHolder(mapOf()),
            artists = artists?.map {
                it.toArtist()
            } ?: emptyList(),
            numberOfTracks = item_count ?: if (single) 1 else null,
            releaseDate = year?.toString(),
            tracks = items?.map { it.toTrack() } ?: emptyList(),
            publisher = null,
            duration = total_duration,
            description = description,
            subtitle = null,
        )
    }

    override suspend fun radio(album: Album): Playlist {
        val full = api.LoadPlaylist.loadPlaylist(album.id).getOrThrow().toAlbum()
        val track = full.tracks.firstOrNull()?.id ?: throw Exception("No tracks found")
        val result = api.SongRadio.getSongRadio(track, null).getOrThrow()
        val tracks = result.items.map {
            it.toTrack()
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
            it.toTrack()
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
            it.toTrack()
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
        val full = api.LoadPlaylist.loadPlaylist(playlist.id).getOrThrow().toPlaylist()
        val track = full.tracks.firstOrNull() ?: throw Exception("No tracks found")
        return radio(track)
    }

    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> = PagedData.Single {
        album.artists.mapNotNull {
            val artist = try {
                loadArtist(it)
            } catch (e: Throwable) {
                null
            }
            if (artist != null) {
                getArtistMediaItems(artist)
            } else null
        }.flatten()
    }

    override suspend fun loadAlbum(small: Album): Album {
        return api.LoadPlaylist.loadPlaylist(small.id).getOrThrow().toAlbum()
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
                    item.toEchoMediaItem(single)
                } ?: emptyList(),
                more = continuationFlow { _ ->
                    val param = it.view_more?.getBrowseParamsData()
                    val rows =
                        param?.let { it1 ->
                            api.ArtistWithParams.loadArtistWithParams(it1).getOrThrow()
                        }
                    val data = rows?.map { itemLayout ->
                        itemLayout.toMediaItems()
                    }?.flatten() ?: emptyList()
                    Page(data, null)
                }
            )
        } ?: emptyList()
        return list
    }

    override fun getMediaItems(artist: Artist): PagedData<MediaItemsContainer> = PagedData.Single {
        getArtistMediaItems(artist)
    }


    override suspend fun loadArtist(small: Artist): Artist {
        val result = api.LoadArtist.loadArtist(small.id).getOrThrow()
        return result.toArtist()
    }


    private fun ArtistWithParamsRow.toMediaItems(): List<EchoMediaItem> {
        return items.mapNotNull { item ->
            item.toEchoMediaItem(title == singles)
        }
    }

    private fun getCover(id: String): ImageHolder.UrlHolder {
        return SongThumbnailProvider(id).getThumbnailUrl(thumbnailQuality)
            .toImageHolder(crop = true)
    }

    private data class SongThumbnailProvider(val id: String) : ThumbnailProvider {
        override fun getThumbnailUrl(quality: ThumbnailProvider.Quality): String =
            when (quality) {
                ThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
                ThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
            }
    }


    override fun getMediaItems(playlist: Playlist): PagedData<MediaItemsContainer> =
        continuationFlow {
            TODO("Will add tomorrow")
        }

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return api.LoadPlaylist.loadPlaylist(playlist.id).getOrThrow().toPlaylist()
    }
}