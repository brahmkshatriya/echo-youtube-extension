package dev.brahmkshatriya.echo.extension

import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.endpoint.LoadPlaylistEndpoint
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.loadmediaitem.parsePlaylistResponse
import dev.toastbits.ytmkt.radio.RadioContinuation
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.put

class EchoLoadAlbumEndPoint(override val api: YoutubeiApi): LoadPlaylistEndpoint() {
    override suspend fun loadPlaylist(
        playlist_id: String,
        continuation: RadioContinuation?,
        browse_params: String?,
        playlist_url: String?
    ): Result<YtmPlaylist> = runCatching {
        val browse_id: String = formatBrowseId(playlist_id)

        val hl: String = api.data_language
        val response: HttpResponse = api.client.request {
            endpointPath("browse")
            addApiHeadersWithAuthenticated()
            postWithBody {
                put("browseId", browse_id)
            }
        }

        return@runCatching parsePlaylistResponse(playlist_id, response, hl, api)
            .getOrThrow<YtmPlaylist>()
    }
}

private fun formatBrowseId(browse_id: String): String =
    if (
        !browse_id.startsWith("VL")
        && !browse_id.startsWith("MPREb_")
    ) "VL$browse_id"
    else browse_id

