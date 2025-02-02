package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

class EchoVisitorEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {
    suspend fun getVisitorId(): String {
        val response: HttpResponse = api.client.request {
            endpointPath("visitor_id")
            addApiHeadersWithAuthenticated()
            postWithBody()
        }

        val data: VisitorIdResponse = response.body()
        return data.responseContext.visitorData
    }

    @Serializable
    private data class VisitorIdResponse(val responseContext: ResponseContext) {
        @Serializable
        data class ResponseContext(val visitorData: String)
    }
}