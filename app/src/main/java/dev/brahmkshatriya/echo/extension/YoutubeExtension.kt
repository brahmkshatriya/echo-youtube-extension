package dev.brahmkshatriya.echo.extension

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class YoutubeExtension : ExtensionClient(), HomeFeedClient, TrackClient, SearchClient {
    override val metadata = ExtensionMetadata(
        id = "youtube",
        name = "Youtube",
        version = "1.0.0",
        description = "Youtube Music Extension for Echo, with the help of YTM-kt library.",
        author = "Echo",
        iconUrl = null
    )
    override val settings: List<Setting> = listOf()

    private val api = YoutubeiApi()
    private val songFeed = api.SongFeed
    private val language = "en-GB"

    private val thumbnailQuality = ThumbnailProvider.Quality.HIGH

    override suspend fun getHomeFeed(genre: StateFlow<String?>): Flow<PagingData<MediaItemsContainer>> {
        val homeFeed = songFeed.getSongFeed().getOrThrow()
        val cont = homeFeed.layouts.map { itemLayout ->
            itemLayout.toMediaItemsContainer()
        }
        return flow { emit(PagingData.from(cont)) }
    }

    override suspend fun getHomeGenres(): List<String> = listOf()
    override suspend fun getStreamableAudio(streamable: Streamable): StreamableAudio {
        val id = streamable.id
        return api.VideoFormats.getVideoFormats(id).getOrNull()
            ?.sortedBy { it.bitrate }
            ?.find { format ->
                (format.url != null) && format.mimeType.startsWith("audio/mp4")
            }?.url?.toAudio() ?: throw Exception("No audio found")
    }

    override suspend fun getTrack(id: String): Track? {
        TODO("Not yet implemented")
    }

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String): Flow<PagingData<MediaItemsContainer>> {
        val search = api.Search.searchMusic(query, null).getOrThrow()
        val list = search.categories.map { (itemLayout, _) ->
            itemLayout.toMediaItemsContainer()
        }
        return flow { emit(PagingData.from(list)) }
    }

    private fun MediaItemLayout.toMediaItemsContainer(): MediaItemsContainer {
        return MediaItemsContainer.Category(
            title = title?.getString(language) ?: "Unknown",
            subtitle = subtitle?.getString(language),
            list = items.mapNotNull { item ->
                when (item) {
                    is YtmSong -> {

                        EchoMediaItem.TrackItem(
                            Track(
                                id = item.id,
                                title = item.name ?: "Unknown",
                                artists = item.artist?.let {
                                    listOf(Artist.Small(it.id, it.name ?: "Unknown"))
                                } ?: emptyList(),
                                streamable = Streamable(item.id),
                                cover = item.thumbnail_provider?.getThumbnailUrl(
                                    thumbnailQuality
                                )?.toImageHolder(mapOf()),
                                album = item.album?.let { album ->
                                    Album.Small(
                                        album.id,
                                        item.album?.name ?: "Unknown"
                                    )
                                },
                                duration = item.duration,
                                plays = null,
                                releaseDate = null,
                                liked = false,
                            )
                        )
                    }

                    is YtmPlaylist -> {
                        EchoMediaItem.AlbumItem(
                            Album.WithCover(
                                id = item.id,
                                title = item.name ?: "Unknown",
                                artist = Artist.Small(
                                    item.artist?.id ?: "echo://uh",
                                    item.artist?.name ?: "Unknown",
                                ),
                                cover = item.thumbnail_provider?.getThumbnailUrl(
                                    thumbnailQuality
                                )?.toImageHolder(mapOf()),
                                numberOfTracks = item.item_count ?: 0,
                            )
                        )
                    }

                    is YtmArtist -> EchoMediaItem.ArtistItem(
                        Artist.WithCover(
                            id = item.id,
                            name = item.name ?: "Unknown",
                            cover = item.thumbnail_provider?.getThumbnailUrl(
                                thumbnailQuality
                            )?.toImageHolder(mapOf()),
                            subtitle = item.description,
                        )
                    )

                    else -> null
                }
            }
        )
    }
}