package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.internal.YoutubeiBrowseResponse
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

class EchoRelatedEndPoint(override val api: YoutubeiApi) : ApiEndpoint() {
    suspend fun loadRelated(hl: String, relatedId: String?) = run {
        val response: HttpResponse = api.client.request {
            endpointPath("browse")
            addApiHeadersWithAuthenticated()
            postWithBody {
                put("browseId", relatedId)
            }
        }
        val data: Related = response.body()
        EchoSongFeedEndpoint.processRows(data.contents.sectionListRenderer.contents!!, api, hl)
    }

    @Serializable
    data class Related (val contents: Contents)
    @Serializable
    data class Contents (val sectionListRenderer: YoutubeiBrowseResponse.SectionListRenderer)
}