package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.internal.YoutubeiBrowseResponse
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.put

class EchoLibraryEndPoint(override val api: YoutubeiApi) : ApiEndpoint() {
    suspend fun loadLibraryFeed(
        id: String,
        ctoken: String? = null
    ): Pair<List<YtmMediaItem>, String?> = run {
        val hl: String = api.data_language
        val response: HttpResponse = api.client.request {
            endpointPath("browse")
            if (ctoken != null) {
                url.parameters.append("ctoken", ctoken)
                url.parameters.append("continuation", ctoken)
                url.parameters.append("type", "next")
            }
            addApiHeadersWithAuthenticated()
            postWithBody {
                put("browseId", id)
            }
        }

        println(response.bodyAsText())

        val data: YoutubeiBrowseResponse = response.body()
        val contents = data.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
        val musicShelf = contents?.musicShelfRenderer
        val continuation = musicShelf?.continuations?.firstOrNull()?.nextContinuationData?.continuation
        val grid = contents?.gridRenderer?.items
        val items = musicShelf?.contents ?: grid ?: emptyList()

        val list = items.mapNotNull {
            val item: YtmMediaItem? = it.toMediaItemData(hl, api)?.first
            if (item is YtmPlaylist) {
                // Skip 'New playlist' item
                if (it.musicTwoRowItemRenderer?.navigationEndpoint?.browseEndpoint == null) {
                    return@mapNotNull null
                }
                for (menuItem in it.musicTwoRowItemRenderer?.menu?.menuRenderer?.items?.asReversed()
                    ?: emptyList()) {
                    if (item.id == "VLLM" || menuItem.menuNavigationItemRenderer?.icon?.iconType == "DELETE") {
                        return@mapNotNull item.copy(
                            owner_id = api.user_auth_state?.own_channel_id
                        )
                    }
                }
            }
            return@mapNotNull item
        }
        return list to continuation
    }
}