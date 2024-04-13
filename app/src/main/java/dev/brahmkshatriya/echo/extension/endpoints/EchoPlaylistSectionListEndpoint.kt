package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.external.RelatedGroup
import dev.toastbits.ytmkt.model.internal.YoutubeiBrowseResponse
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse

open class EchoPlaylistSectionListEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    suspend fun loadFromPlaylist(token: String) = runCatching {
        val hl: String = api.data_language
        val response: HttpResponse = api.client.request {
            endpointPath("browse")
            url {
                parameters.append("ctoken", token)
                parameters.append("continuation", token)
                parameters.append("type", "next")
            }
            addApiHeadersWithAuthenticated()
            postWithBody()
        }

        val data: YoutubeiBrowseResponse = response.body()
        val shelf =
            data.continuationContents?.sectionListContinuation
        shelf?.contents?.map { group ->
            RelatedGroup(
                title = group.title?.text,
                items = group.getMediaItemsOrNull(hl, api),
                description = group.description
            )
        } ?: emptyList()
    }

}
