package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.extension.endpoints.EchoPlaylistEndpoint.Companion.parsePlaylistResponse
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiPostBody
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import io.ktor.client.request.request

class EchoPlaylistContinuationEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {
    suspend fun load(
        ctoken: String?
    ): Triple<List<YtmSong>?, List<String>?, String?> = run {
        val response = api.client.request {
            endpointPath("browse")
            addApiHeadersWithAuthenticated()
            if (ctoken != null) {
                url.parameters.append("ctoken", ctoken)
                url.parameters.append("continuation", ctoken)
                url.parameters.append("type", "next")
            }
            postWithBody(YoutubeiPostBody.ANDROID_MUSIC.getPostBody(api))
        }
        val parsed = parsePlaylistResponse("", response, api.data_language, api).first
        Triple(parsed.items, parsed.item_set_ids, parsed.continuation?.token)
    }
}