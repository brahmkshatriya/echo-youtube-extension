package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiPostBody
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylistBuilder
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.model.internal.Header
import dev.toastbits.ytmkt.model.internal.YoutubeiBrowseResponse
import dev.toastbits.ytmkt.radio.RadioContinuation
import dev.toastbits.ytmkt.uistrings.parseYoutubeDurationString
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

class EchoPlaylistEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    private fun formatBrowseId(browseId: String) =
        if (!browseId.startsWith("VL") && !browseId.startsWith("MPREb_")) "VL$browseId"
        else browseId

    suspend fun loadFromPlaylist(
        playlistId: String,
        params: String? = null
    ): Pair<YtmPlaylist, String?> = run {
        val resolveUrl = if (!playlistId.startsWith("MPREb_"))
            api.client.request {
                endpointPath("navigation/resolve_url")
                addApiHeadersWithAuthenticated()
                postWithBody {
                    put("url", "https://music.youtube.com/playlist?list=$playlistId")
                }
            }.body<PlaylistEndpointResponse>().endpoint?.browseEndpoint
        else null

        val id = resolveUrl?.browseId ?: formatBrowseId(playlistId)
        val param = resolveUrl?.params ?: params

        val res = api.client.request {
            endpointPath("browse")
            addApiHeadersWithAuthenticated()
            postWithBody(YoutubeiPostBody.MOBILE.getPostBody(api)) {
                put("browseId", id)
                if (param != null) {
                    put("params", param)
                }
            }
        }

        val parsed =
            parsePlaylistResponse(playlistId, res, api.data_language, api).getOrThrow()

        if (parsed.first.items == null) {
            val data: PlaylistUrlResponse = res.body()
            val loadedPlaylistUrl = data.microformat?.microformatDataRenderer?.urlCanonical
            if (loadedPlaylistUrl != null) {
                val newId = loadedPlaylistUrl.substringAfter("?list=").substringBefore("&")
                return loadFromPlaylist(newId, params)
            }
        }
        parsed
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


    @Serializable
    private data class PlaylistUrlResponse(val microformat: Microformat?) {
        @Serializable
        data class Microformat(val microformatDataRenderer: MicroformatDataRenderer)

        @Serializable
        data class MicroformatDataRenderer(val urlCanonical: String?)
    }


    private suspend fun parsePlaylistResponse(
        playlistId: String,
        response: HttpResponse,
        hl: String,
        api: YtmApi
    ): Result<Pair<YtmPlaylist, String?>> = runCatching {
        println(response.bodyAsText())
        val parsed: YoutubeiBrowseResponse = response.body()
        val builder = YtmPlaylistBuilder(playlistId)
        val headerRenderer = parsed.header?.getRenderer()
        if (headerRenderer != null) {
            builder.name = headerRenderer.title!!.first_text
            builder.description = headerRenderer.description?.first_text
            builder.thumbnail_provider =
                ThumbnailProvider.fromThumbnails(headerRenderer.getThumbnails())

            builder.artists = headerRenderer.subtitle?.runs?.mapNotNull { run ->
                val browseEndpoint = run.navigationEndpoint?.browseEndpoint
                if (browseEndpoint?.browseId == null) {
                    if (run.text.all { it.isDigit() }) {
                        builder.year = run.text.toInt()
                    }
                    return@mapNotNull null
                } else if (browseEndpoint.getMediaItemType() != YtmMediaItem.Type.ARTIST) {
                    return@mapNotNull null
                } else return@mapNotNull YtmArtist(
                    id = browseEndpoint.browseId!!,
                    name = run.text
                )
            }?.distinctBy { it.id }?.ifEmpty { null }

            headerRenderer.secondSubtitle?.runs?.also { secondSubtitle ->
                for (run in secondSubtitle.reversed().withIndex()) {
                    when (run.index) {
                        0 -> builder.total_duration = parseYoutubeDurationString(run.value.text, hl)
                        1 -> builder.item_count = run.value.text.filter { it.isDigit() }.toInt()
                    }
                }
            }
        }

        val menuButtons: List<Header.TopLevelButton>? =
            parsed.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons

        if (menuButtons?.any { it.buttonRenderer?.icon?.iconType == "EDIT" } == true) {
            builder.owner_id = api.user_auth_state?.own_channel_id
            builder.type = YtmPlaylist.Type.PLAYLIST
        }

        val sectionListRenderer = with(parsed.contents!!) {
            singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                ?: twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
        }

        val relatedId =
            sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation
        for (row in sectionListRenderer?.contents.orEmpty().withIndex()) {
            val description: String? = row.value.description
            if (description != null) {
                builder.description = description
                if (builder.items != null) break
                continue
            }

            val rowItems = row.value.getMediaItemsAndSetIds(hl, api)
            builder.items = rowItems.map { it.first }.filterIsInstance<YtmSong>()

            val continuationToken: String? =
                row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

            builder.continuation = continuationToken?.let {
                RadioContinuation(it, RadioContinuation.Type.PLAYLIST)
            }
            builder.item_set_ids =
                if (rowItems.all { it.second != null }) rowItems.map { it.second!! } else null

            // Playlists don't display indices
            if (row.value.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.index != null) {
                builder.type = YtmPlaylist.Type.ALBUM
            }

            if (builder.description != null) {
                break
            }
        }

        return@runCatching builder.build() to relatedId
    }

}