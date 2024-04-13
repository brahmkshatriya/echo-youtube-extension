package dev.brahmkshatriya.echo.extension

import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.endpoint.LoadPlaylistEndpoint
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.loadmediaitem.parsePlaylistResponse
import dev.toastbits.ytmkt.model.internal.Header
import dev.toastbits.ytmkt.radio.RadioContinuation
import io.ktor.client.call.body
import io.ktor.client.request.request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

class EchoLoadAlbumEndPoint(override val api: YoutubeiApi) : LoadPlaylistEndpoint() {
    override suspend fun loadPlaylist(
        playlist_id: String,
        continuation: RadioContinuation?,
        browse_params: String?,
        playlist_url: String?
    ): Result<YtmPlaylist> = runCatching {
        val id: String = formatBrowseId(playlist_id)

        val hl: String = api.data_language
        val response = browse(id)
        var playlist = parsePlaylistResponse(playlist_id, response, hl, api)
            .getOrThrow()
        if (playlist.items.isNullOrEmpty()) {
            val parsed = response.body<PlaylistUrlResponse>()
            val url = parsed.microformat?.microformatDataRenderer?.urlCanonical
            if (url != null) {
                val browseId: String = url.toId()
                val newResponse = browse(browseId)
                val newPlaylist = parsePlaylistResponse(playlist_id, newResponse, hl, api)
                    .getOrThrow()
                playlist = playlist.copy(
                    items = newPlaylist.items,
                    continuation = newPlaylist.continuation
                )
            }
        }
        playlist
    }

    private suspend fun browse(id: String) = api.client.request {
        endpointPath("browse")
        addApiHeadersWithAuthenticated()
        postWithBody {
            put("browseId", id)
        }
    }

    private fun String.toId(): String {
        val start: Int = indexOf("?list=") + 6
        var end: Int = indexOf("&", start)
        if (end == -1) {
            end = length
        }
        return formatBrowseId(substring(start, end))
    }
}

@Serializable
private data class PlaylistUrlResponse(
    val microformat: Microformat?,
    val header: Header?
) {
    @Serializable
    data class Microformat(val microformatDataRenderer: MicroformatDataRenderer)

    @Serializable
    data class MicroformatDataRenderer(val urlCanonical: String?)
}

private fun formatBrowseId(browse_id: String): String =
    if (
        !browse_id.startsWith("VL")
        && !browse_id.startsWith("MPREb_")
    ) "VL$browse_id"
    else browse_id

