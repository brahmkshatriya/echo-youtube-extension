package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import dev.toastbits.ytmkt.endpoint.SongFeedLoadResult
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.model.external.MediaItemYoutubePage
import dev.toastbits.ytmkt.model.external.PlainYoutubePage
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.copyWithName
import dev.toastbits.ytmkt.model.internal.BrowseEndpoint
import dev.toastbits.ytmkt.model.internal.YoutubeiBrowseResponse
import dev.toastbits.ytmkt.model.internal.YoutubeiHeaderContainer
import dev.toastbits.ytmkt.model.internal.YoutubeiShelf
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.YoutubeUILocalisation
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.put

private val PLAIN_HEADERS: List<String> =
    listOf("accept-language", "user-agent", "accept-encoding", "content-encoding", "origin")

open class EchoSongFeedEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    suspend fun getSongFeed(
        minRows: Int = -1,
        params: String? = null,
        continuation: String? = null,
        browseId: String? = null
    ): Result<SongFeedLoadResult> = runCatching {
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

            return response.body()
        }

        var data: YoutubeiBrowseResponse = performRequest(continuation)
        val headerChips: List<SongFeedFilterChip>? = data.getHeaderChips(hl)

        val rows: MutableList<MediaItemLayout> = processRows(
            data.getShelves(continuation != null), api, hl
        ).toMutableList()

        var ctoken: String? = data.ctoken
        while (ctoken != null && minRows >= 1 && rows.size < minRows) {
            data = performRequest(ctoken)
            ctoken = data.ctoken

            val shelves = data.getShelves(true)
            if (shelves.isEmpty()) {
                break
            }

            rows.addAll(processRows(shelves, api, hl))
        }

        return@runCatching SongFeedLoadResult(rows, ctoken, headerChips)
    }

    companion object {
        fun processRows(
            rows: List<YoutubeiShelf>, api: YoutubeiApi, hl: String
        ): List<MediaItemLayout> {
            val ret: MutableList<MediaItemLayout> = mutableListOf()
            fun String.createUiString() =
                YoutubeUiString.Type.HOME_FEED.createFromKey(this, api.data_language)

            for (row in rows) {
                val renderer = row.getRenderer()
                if (renderer !is YoutubeiHeaderContainer) {
                    continue
                }

                val header = renderer.header?.header_renderer ?: continue
                val title = header.title?.first_text?.createUiString() ?: continue
                val subtitle = (header.subtitle ?: header.strapline)?.first_text?.let {
                    RawUiString(it.lowercase().replaceFirstChar { char -> char.uppercase() })
                }

                fun add(
                    viewMore: YoutubePage? = null,
                    type: ItemLayoutType? = null,
                    items: List<YtmMediaItem> = row.getMediaItems(hl, api)
                ) {
                    ret.add(MediaItemLayout(items, title, subtitle, type, viewMore))
                }

                val titleTextRun = header.title ?: continue
                val browseEndpoint: BrowseEndpoint? =
                    titleTextRun.runs?.first()?.navigationEndpoint?.browseEndpoint
                val browseId = browseEndpoint?.browseId
                if (browseEndpoint == null) {
                    val items: List<YtmMediaItem> =
                        if (title is YoutubeUiString && title.getYoutubeStringId() == YoutubeUILocalisation.StringID.FEED_ROW_RADIOS) {
                            row.getMediaItems(hl, api).map { item ->
                                if (item is YtmPlaylist) {
                                    item.copy(type = YtmPlaylist.Type.RADIO)
                                } else item
                            }
                        } else {
                            row.getMediaItems(hl, api)
                        }
                    add(items = items)
                    continue
                }

                if (browseId?.startsWith("FEmusic_") == true) {
                    add(PlainYoutubePage(browseId))
                    continue
                }

                val pageType: String? =
                    browseEndpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType

                val mediaItem: YtmMediaItem? =
                    if (pageType != null && browseId != null) YtmMediaItem.Type.fromBrowseEndpointType(
                        pageType
                    ).itemFromId(browseId)
                        .copyWithName(name = titleTextRun.runs?.getOrNull(0)?.text)
                    else null

                add(mediaItem?.let { MediaItemYoutubePage(it, null) })
            }

            return ret
        }
    }

}
