package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.endpoint.ChipCloudRendererHeader
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMGetSongFeedEndpoint
import dev.toastbits.ytmkt.radio.YoutubeiNextResponse
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import dev.toastbits.ytmkt.model.internal.Header
import dev.toastbits.ytmkt.model.internal.YoutubeiShelf
import kotlinx.serialization.Serializable

@Serializable
data class YoutubeiBrowseResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
    val header: Header?
) {
    val ctoken: String?
        get() = continuationContents?.sectionListContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation
            ?: contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

    fun getShelves(hasContinuation: Boolean): List<YoutubeiShelf> {
        return if (hasContinuation) continuationContents?.sectionListContinuation?.contents ?: emptyList()
        else contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: contents?.sectionListRenderer?.contents ?: emptyList()
    }

    fun getHeaderChips(dataLanguage: String): List<SongFeedFilterChip>? =
        contents?.singleColumnBrowseResultsRenderer?.tabs?.first()?.tabRenderer?.content?.sectionListRenderer?.header?.chipCloudRenderer?.chips?.map {
            SongFeedFilterChip(
                YoutubeUiString.Type.FILTER_CHIP.createFromKey(it.chipCloudChipRenderer.text!!.first_text, dataLanguage),
                it.chipCloudChipRenderer.navigationEndpoint.browseEndpoint!!.params!!
            )
        }

    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
        val sectionListRenderer: SectionListRenderer?
    )
    @Serializable
    data class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    @Serializable
    data class Tab(val tabRenderer: TabRenderer)
    @Serializable
    data class TabRenderer(val content: Content?)
    @Serializable
    data class Content(val sectionListRenderer: SectionListRenderer?)

    @Serializable
    data class SectionListRenderer(val contents: List<YoutubeiShelf>?, val header: ChipCloudRendererHeader?, val continuations: List<YoutubeiNextResponse.Continuation>?)
    @Serializable
    data class TwoColumnBrowseResultsRenderer(val tabs: List<Tab>, val secondaryContents: SecondaryContents) {
        @Serializable
        data class SecondaryContents(val sectionListRenderer: SectionListRenderer)
    }

    @Serializable
    data class ContinuationContents(val sectionListContinuation: SectionListRenderer?, val musicPlaylistShelfContinuation: YTMGetSongFeedEndpoint.MusicShelfRenderer?, val musicShelfContinuation: YTMGetSongFeedEndpoint.MusicShelfRenderer?)
}
