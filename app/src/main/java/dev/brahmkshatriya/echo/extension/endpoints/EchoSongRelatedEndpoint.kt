package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse

open class EchoSongRelatedEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    suspend fun loadFromPlaylist(token: String) = runCatching {
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
        val contents =
            data.continuationContents?.sectionListContinuation?.contents
        contents?.let { EchoSongFeedEndpoint.processRows(it, api) } ?: emptyList()
    }

}
