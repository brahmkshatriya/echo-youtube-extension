package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiPostBody
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylistBuilder
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.radio.RadioContinuation
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

class EchoPlaylistEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    private val continuationEndpoint = EchoPlaylistContinuationEndpoint(api)

    private fun formatBrowseId(browseId: String) =
        if (!browseId.startsWith("VL") && !browseId.startsWith("MPREb_")) "VL$browseId"
        else browseId

    private fun cleanId(playlistId: String) =
        if (playlistId.startsWith("VL")) playlistId.substring(2)
        else playlistId

    suspend fun loadFromPlaylist(
        playlistId: String,
        params: String? = null
    ): Pair<YtmPlaylist, String?> = run {

        val endpoint = if (!playlistId.startsWith("MPREb_"))
            api.client.request {
                endpointPath("navigation/resolve_url")
                addApiHeadersWithAuthenticated()
                postWithBody {
                    put("url", "https://music.youtube.com/playlist?list=${cleanId(playlistId)}")
                }
            }.body<PlaylistEndpointResponse>().endpoint?.browseEndpoint?.takeIf {
                val pageType =
                    it.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                pageType == "MUSIC_PAGE_TYPE_PLAYLIST" || pageType == "MUSIC_PAGE_TYPE_ALBUM"
            }
        else null

        val id = endpoint?.browseId ?: formatBrowseId(playlistId)
        val param = endpoint?.params ?: params

        val res = api.client.request {
            endpointPath("browse")
            addApiHeadersWithAuthenticated()
            postWithBody(YoutubeiPostBody.ANDROID_MUSIC.getPostBody(api)) {
                put("browseId", id)
                if (param != null) {
                    put("params", param)
                }
            }
        }
        val items = mutableListOf<YtmSong>()
        val ids = mutableListOf<String>()
        val (playlist, relation) =
            parsePlaylistResponse(cleanId(id), res, api.data_language, api)
        playlist.items?.let { items.addAll(it) }
        playlist.item_set_ids?.let { ids.addAll(it) }
        var continuation = playlist.continuation?.token
        while (continuation != null) {
            val (songs, setIds, cont) = continuationEndpoint.load(continuation)
            songs?.let { items.addAll(it) }
            setIds?.let { ids.addAll(it) }
            continuation = cont
        }
        playlist.copy(items = items) to relation
    }


    @Serializable
    data class PlaylistEndpointResponse(
        val endpoint: Endpoint? = null
    ) {

        @Serializable
        data class Endpoint(
            val browseEndpoint: BrowseEndpoint? = null
        )

        @Serializable
        data class BrowseEndpoint(
            val browseId: String? = null,
            val params: String? = null,
            val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null
        )

        @Serializable
        data class BrowseEndpointContextSupportedConfigs(
            val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig? = null
        )

        @Serializable
        data class BrowseEndpointContextMusicConfig(
            val pageType: String? = null
        )
    }

    companion object {

        suspend fun parsePlaylistResponse(
            playlistId: String,
            response: HttpResponse,
            hl: String,
            api: YtmApi
        ) = run {
            val parsed: YoutubeiBrowseResponse = response.body()

            val builder = YtmPlaylistBuilder(playlistId)
            val playlistData = parsed.header?.getPlaylistData()
            if (playlistData != null) {
                builder.name = playlistData.title
                builder.description = playlistData.description
                builder.thumbnail_provider = playlistData.thumbnail
                builder.artists = playlistData.artists
                builder.year = playlistData.year
                builder.owner_id =
                    if (playlistData.isEditable) api.user_auth_state?.own_channel_id else null
            }

            val sectionListRenderer = parsed.contents?.run {
                singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                    ?: twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
            }
            val relatedId =
                sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

            val row = sectionListRenderer?.contents?.firstOrNull()
            if (row != null) {
                val rowItems = row.getMediaItemsAndSetIds(hl, api)
                builder.items = rowItems?.map { it.first }?.filterIsInstance<YtmSong>()
                val continuationToken: String? =
                    row.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation
                builder.continuation = continuationToken?.let {
                    RadioContinuation(it, RadioContinuation.Type.PLAYLIST)
                }
                builder.item_set_ids = rowItems?.let { pairs ->
                    if (pairs.all { it.second != null }) pairs.map { it.second!! } else null
                }
            }

            val continuationItems =
                parsed.continuationContents?.musicPlaylistShelfContinuation?.contents?.map {
                    it.toMediaItemData(hl, api)
                }
            if (continuationItems != null) {
                builder.items = continuationItems.map { it?.first }.filterIsInstance<YtmSong>()
                builder.item_set_ids = continuationItems.mapNotNull { it?.second }
                val cont =
                    parsed.continuationContents.musicPlaylistShelfContinuation.continuations?.firstOrNull()?.nextContinuationData?.continuation
                builder.continuation =
                    cont?.let { RadioContinuation(it, RadioContinuation.Type.PLAYLIST) }
            }

            builder.build() to relatedId
        }
    }
}