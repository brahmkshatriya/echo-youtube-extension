package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import dev.toastbits.ytmkt.endpoint.SongFeedLoadResult
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMGetSongFeedEndpoint
import dev.toastbits.ytmkt.itemcache.MediaItemCache
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.MediaItemYoutubePage
import dev.toastbits.ytmkt.model.external.PlainYoutubePage
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.model.internal.BrowseEndpoint
import dev.toastbits.ytmkt.model.internal.MusicTwoRowItemRenderer
import dev.toastbits.ytmkt.model.internal.YoutubeiHeaderContainer
import dev.toastbits.ytmkt.model.internal.YoutubeiShelf
import dev.toastbits.ytmkt.model.internal.YoutubeiShelfContentsItem
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.put

val PLAIN_HEADERS: List<String> =
    listOf("accept-language", "user-agent", "accept-encoding", "content-encoding", "origin")

open class EchoSongFeedEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    suspend fun getSongFeed(
        minRows: Int = -1,
        params: String? = null,
        continuation: String? = null,
        browseId: String? = null
    ) = runCatching {
        val hl: String = api.data_language

        suspend fun performRequest(ctoken: String?): YoutubeiBrowseResponse {
            val response: HttpResponse = api.client.request {
                endpointPath("browse")

                if (ctoken != null) {
                    url.parameters.append("ctoken", ctoken)
                    url.parameters.append("continuation", ctoken)
                    url.parameters.append("type", "next")
                }

                addApiHeadersWithAuthenticated()
                addApiHeadersWithoutAuthentication(PLAIN_HEADERS)
                postWithBody {
                    if (params != null) {
                        put("params", params)
                    }
                    if (browseId != null) {
                        put("browseId", browseId)
                    }
                }
            }
            println(response.bodyAsText())
            return response.body()
        }

        var data: YoutubeiBrowseResponse = performRequest(continuation)
        val headerChips: List<SongFeedFilterChip>? = data.getHeaderChips(hl)

        val rows: MutableList<MediaItemLayout> = processRows(
            data.getShelves(continuation != null), api
        ).toMutableList()

        var ctoken: String? = data.ctoken
        while (ctoken != null && minRows >= 1 && rows.size < minRows) {
            data = performRequest(ctoken)
            ctoken = data.ctoken

            val shelves = data.getShelves(true)
            if (shelves.isEmpty()) {
                break
            }

            rows.addAll(processRows(shelves, api))
        }

        return@runCatching SongFeedLoadResult(rows, ctoken, headerChips)
    }

    companion object {
        fun processRows(
            rows: List<YoutubeiShelf>, api: YoutubeiApi
        ): List<MediaItemLayout> {
            val hl = api.data_language
            fun String.createUiString() =
                YoutubeUiString.Type.HOME_FEED.createFromKey(this, api.data_language)

            return rows.mapNotNull { row ->
                when (val renderer = row.getRenderer()) {
                    is YTMGetSongFeedEndpoint.MusicShelfRenderer -> {
                        val mediaItem = renderer.bottomEndpoint?.getMediaItem()
                        MediaItemLayout(
                            row.getYtmMediaItems(hl, api),
                            (renderer.title?.first_text ?: "").createUiString(),
                            null,
                            null,
                            mediaItem?.let { renderer.bottomEndpoint?.getViewMore(it) }
                        )
                    }

                    is YoutubeiHeaderContainer -> {
                        val header = renderer.header?.header_renderer ?: return@mapNotNull null
                        val titleTextRun = header.title ?: return@mapNotNull null
                        val browseEndpoint =
                            titleTextRun.runs?.first()?.navigationEndpoint?.browseEndpoint
                        val browseId = browseEndpoint?.browseId
                        val pageType = browseEndpoint?.browseEndpointContextSupportedConfigs
                                ?.browseEndpointContextMusicConfig?.pageType
                        val title = titleTextRun.first_text.createUiString()

                        val subtitle = (header.subtitle ?: header.strapline)?.first_text?.let {
                            RawUiString(
                                it.lowercase().replaceFirstChar { char -> char.uppercase() })
                        }
                        println("$title browseId: $browseId, pageType: $pageType")
                        val page = when {
                            browseId?.startsWith("FEmusic_") == true ->
                                PlainYoutubePage(browseId)

                            pageType != null && browseId != null -> {
                                val mediaItem =
                                    YtmMediaItem.Type.fromBrowseEndpointType(pageType)
                                        .itemFromId(browseId)
                                MediaItemYoutubePage(mediaItem, browseEndpoint.params)
                            }

                            else -> null
                        }
                        println("page : $page")
                        val items = row.getYtmMediaItems(hl, api)
                        MediaItemLayout(items, title, subtitle, null, page)
                    }

                    else -> {
                        println("Unknown shelf type: $renderer")
                        null
                    }
                }
            }
        }


        private fun YoutubeiShelf.getYtmMediaItems(hl: String, api: YtmApi) = run {
            val renderer = musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents
            ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer!!.items
            renderer.mapNotNull { it.toYtmMediaItemData(hl, api)?.first }
        }

        private fun YoutubeiShelfContentsItem.toYtmMediaItemData(
            hl: String, api: YtmApi
        ): Pair<YtmMediaItem, String?>? {
            if (musicTwoRowItemRenderer != null) {
                return musicTwoRowItemRenderer!!.toYtmMediaItem(api)?.let { Pair(it, null) }
            } else if (musicResponsiveListItemRenderer != null) {
                return musicResponsiveListItemRenderer!!.toMediaItemAndPlaylistSetVideoId(hl)
            } else if (musicMultiRowListItemRenderer != null) {
                return Pair(musicMultiRowListItemRenderer!!.toMediaItem(hl), null)
            }

            throw NotImplementedError()
        }

        private fun MusicTwoRowItemRenderer.toYtmMediaItem(api: YtmApi): YtmMediaItem? {

            fun getArtists(
                hostItem: YtmMediaItem,
                api: YtmApi
            ): List<YtmArtist>? {
                val artists: List<YtmArtist>? = subtitle?.runs?.mapNotNull { run ->
                    val browseEndpoint: BrowseEndpoint? = run.navigationEndpoint?.browseEndpoint
                    if (browseEndpoint?.browseId == null || browseEndpoint.getMediaItemType() != YtmMediaItem.Type.ARTIST) {
                        return@mapNotNull null
                    }

                    return@mapNotNull YtmArtist(
                        browseEndpoint.browseId!!,
                        name = run.text
                    )
                }

                if (!artists.isNullOrEmpty()) {
                    return artists
                }

                if (hostItem is YtmSong) {
                    val songType: YtmSong.Type? = api.item_cache.getSong(
                        hostItem.id,
                        setOf(MediaItemCache.SongKey.TYPE)
                    )?.type

                    val index: Int = if (songType == YtmSong.Type.VIDEO) 0 else 1
                    subtitle?.runs?.getOrNull(index)?.also {
                        return listOf(
                            YtmArtist(YtmArtist.getForItemId(hostItem)).copy(
                                name = it.text
                            )
                        )
                    }
                }

                return null
            }

            // Video
            val watchEndpoint = navigationEndpoint.watchEndpoint
            val playlistEndpoint = navigationEndpoint.watchPlaylistEndpoint
            return if (watchEndpoint?.videoId != null) {
                val album: YtmPlaylist? = menu?.menuRenderer?.items?.find {
                    it.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.getMediaItemType() == YtmMediaItem.Type.PLAYLIST
                }?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId?.let {
                    YtmPlaylist(YtmPlaylist.cleanId(it))
                }
                val thumbnail =
                    thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
                val songId: String = YtmSong.cleanId(watchEndpoint.videoId!!)
                YtmSong(
                    id = songId,
                    type =
                    if (thumbnail?.height == thumbnail?.width) YtmSong.Type.SONG
                    else YtmSong.Type.VIDEO,
                    name = this.title.first_text,
                    thumbnail_provider = thumbnailRenderer.toThumbnailProvider(),
                    artists = getArtists(YtmSong(songId), api),
                    is_explicit = subtitleBadges?.any { it.isExplicit() } == true,
                    album = album
                )
            } else if (playlistEndpoint != null) {
                YtmPlaylist(
                    id = YtmPlaylist.cleanId(playlistEndpoint.playlistId),
                    type = YtmPlaylist.Type.RADIO,
                    name = title.first_text,
                    thumbnail_provider = thumbnailRenderer.toThumbnailProvider()
                )
            } else {
                val endpoint = navigationEndpoint.browseEndpoint
                // Playlist or artist
                val browseId: String = endpoint?.browseId ?: return null
                val pageType: String = endpoint.getPageType() ?: return null

                val title: String = title.first_text
                val thumbnailProvider = thumbnailRenderer.toThumbnailProvider()

                when (YtmMediaItem.Type.fromBrowseEndpointType(pageType)) {
                    YtmMediaItem.Type.SONG -> {
                        val songId: String = YtmSong.cleanId(browseId)
                        YtmSong(
                            songId,
                            name = title,
                            thumbnail_provider = thumbnailProvider,
                            artists = getArtists(YtmSong(songId), api)
                        )
                    }

                    YtmMediaItem.Type.ARTIST -> YtmArtist(
                        browseId,
                        name = title,
                        thumbnail_provider = thumbnailProvider
                    )

                    YtmMediaItem.Type.PLAYLIST -> {
                        val playlistId: String = YtmPlaylist.cleanId(browseId)
                        YtmPlaylist(
                            playlistId,
                            type = YtmPlaylist.Type.fromBrowseEndpointType(pageType),
                            artists = getArtists(YtmPlaylist(playlistId), api),
                            name = title,
                            thumbnail_provider = thumbnailProvider,
                            year = subtitle?.runs?.find { it.text.length == 4 }?.text?.toIntOrNull()
                        )
                    }
                }
            }
        }
    }
}
