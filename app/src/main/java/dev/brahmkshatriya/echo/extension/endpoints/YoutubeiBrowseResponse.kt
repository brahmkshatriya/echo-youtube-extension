package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import dev.toastbits.ytmkt.impl.youtubei.endpoint.ChipCloudRendererHeader
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.Thumbnail
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.internal.HeaderRenderer
import dev.toastbits.ytmkt.model.internal.MusicMultiRowListItemRenderer
import dev.toastbits.ytmkt.model.internal.MusicResponsiveListItemRenderer
import dev.toastbits.ytmkt.model.internal.MusicTwoRowItemRenderer
import dev.toastbits.ytmkt.model.internal.TextRun
import dev.toastbits.ytmkt.model.internal.TextRuns
import dev.toastbits.ytmkt.radio.YoutubeiNextResponse
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
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
        return if (hasContinuation) continuationContents?.sectionListContinuation?.contents
            ?: emptyList()
        else contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: contents?.sectionListRenderer?.contents ?: emptyList()
    }

    fun getHeaderChips(dataLanguage: String): List<SongFeedFilterChip>? =
        contents?.singleColumnBrowseResultsRenderer?.tabs?.first()?.tabRenderer?.content?.sectionListRenderer?.header?.chipCloudRenderer?.chips?.map {
            SongFeedFilterChip(
                YoutubeUiString.Type.FILTER_CHIP.createFromKey(
                    it.chipCloudChipRenderer.text!!.first_text,
                    dataLanguage
                ),
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
    data class SectionListRenderer(
        val contents: List<YoutubeiShelf>?,
        val header: ChipCloudRendererHeader?,
        val continuations: List<YoutubeiNextResponse.Continuation>?
    )

    @Serializable
    data class TwoColumnBrowseResultsRenderer(
        val tabs: List<Tab>,
        val secondaryContents: SecondaryContents
    ) {
        @Serializable
        data class SecondaryContents(val sectionListRenderer: SectionListRenderer)
    }

    @Serializable
    data class YoutubeiShelf(
        val musicShelfRenderer: MusicShelfRenderer?,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val musicCardShelfRenderer: MusicCardShelfRenderer?,
        val gridRenderer: GridRenderer?,
        val itemSectionRenderer: ItemSectionRenderer?
    ) {
        val title: TextRun?
            get() =
                if (musicShelfRenderer != null) musicShelfRenderer.title?.runs?.firstOrNull()
                else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.getRenderer()?.title?.runs?.firstOrNull()
                else if (musicDescriptionShelfRenderer != null) musicDescriptionShelfRenderer.header?.runs?.firstOrNull()
                else if (musicCardShelfRenderer != null) musicCardShelfRenderer.title.runs?.firstOrNull()
                else if (gridRenderer != null) gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()
                else null

        fun getMediaItems(hl: String, api: YtmApi) = run {
            val contents = musicShelfRenderer?.contents
                ?: musicPlaylistShelfRenderer?.contents
                ?: musicCarouselShelfRenderer?.contents
                ?: gridRenderer?.items
            println("media item contents : $contents")
            contents?.mapNotNull { it.toMediaItemData(hl, api)?.first }
        }

        @Serializable
        data class MusicShelfRenderer(
            val title: TextRuns?,
            val contents: List<YoutubeiShelfContentsItem>? = null,
            val continuations: List<YoutubeiNextResponse.Continuation>? = null,
            val bottomEndpoint: dev.toastbits.ytmkt.model.internal.NavigationEndpoint?
        )

        @Serializable
        data class YoutubeiShelfContentsItem(
            val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?,
            val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
            val musicMultiRowListItemRenderer: MusicMultiRowListItemRenderer?,
            val musicTwoColumnItemRenderer: MusicTwoColumnItemRenderer?
        ) {

            // Pair(item, playlistSetVideoId)
            fun toMediaItemData(hl: String, api: YtmApi): Pair<YtmMediaItem, String?>? {
                if (musicTwoRowItemRenderer != null) {
                    return musicTwoRowItemRenderer.toYtmMediaItem(api)?.let { Pair(it, null) }
                } else if (musicResponsiveListItemRenderer != null) {
                    return musicResponsiveListItemRenderer.toMediaItemAndPlaylistSetVideoId(hl)
                } else if (musicMultiRowListItemRenderer != null) {
                    return Pair(musicMultiRowListItemRenderer.toMediaItem(hl), null)
                } else if (musicTwoColumnItemRenderer != null) {
                    return musicTwoColumnItemRenderer.toMediaItemData(hl)
                }

                throw NotImplementedError()
            }
        }

        fun getMediaItemsAndSetIds(hl: String, api: YtmApi) = run {
            val renderer = musicShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents
            renderer?.mapNotNull { it.toMediaItemData(hl, api) }
        }

        fun getRenderer() =
            musicShelfRenderer ?: musicPlaylistShelfRenderer ?: musicCarouselShelfRenderer ?: musicDescriptionShelfRenderer ?: musicCardShelfRenderer ?: gridRenderer
    }

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListRenderer?,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfRenderer?,
        val musicShelfContinuation: YoutubeiShelf.MusicShelfRenderer?
    )

    data class PlaylistData(
        val playlistId: String?,
        val privacy: String?,
        val title: String?,
        val description: String?,
        val thumbnail: ThumbnailProvider?,
        val artists: List<YtmArtist>,
        val year: Int?,
        val explicit: Boolean = false,
        val isEditable: Boolean = false,
    )

    @Serializable
    data class Header(
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer? = null,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer? = null,
        val musicElementHeaderRenderer: MusicElementHeaderRenderer? = null,
        val musicCarouselShelfBasicHeaderRenderer: HeaderRenderer?,
        val musicImmersiveHeaderRenderer: HeaderRenderer?,
        val musicVisualHeaderRenderer: HeaderRenderer?,
        val musicCardShelfHeaderBasicRenderer: HeaderRenderer?
    ) {
        fun getRenderer(): HeaderRenderer? {
            return musicCarouselShelfBasicHeaderRenderer
                ?: musicImmersiveHeaderRenderer
                ?: musicVisualHeaderRenderer
        }

        fun getPlaylistData() =
            musicElementHeaderRenderer?.elementRenderer?.elementRenderer?.newElement?.type?.componentType?.model?.musicBlurredBackgroundHeaderModel?.data?.let {
                PlaylistData(
                    playlistId = null,
                    privacy = null,
                    title = it.title,
                    description = it.formattedDescription?.content,
                    thumbnail = it.primaryImage?.sources?.let { thumbnails ->
                        ThumbnailProvider.fromThumbnails(
                            thumbnails
                        )
                    },
                    artists = listOfNotNull(
                        it.straplineData?.headerOnTapCommand?.innertubeCommand?.browseEndpoint?.browseId?.let { id ->
                            YtmArtist(id, it.straplineData.textLine1?.content)
                        }
                    ),
                    year = it.straplineData?.textLine2?.content?.takeLast(4)?.toIntOrNull(),
                    isEditable = false
                )
            } ?: musicDetailHeaderRenderer?.let {
                PlaylistData(
                    playlistId = null,
                    privacy = null,
                    title = it.title?.runs?.firstOrNull()?.text,
                    description = it.byline?.musicDetailHeaderButtonsBylineRenderer?.description?.runs?.firstOrNull()?.text,
                    thumbnail = it.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.let { thumbnails ->
                        ThumbnailProvider.fromThumbnails(thumbnails)
                    },
                    artists = it.secondTitle?.runs?.mapNotNull { run ->
                        val browseEndpoint =
                            run.navigationEndpoint?.browseEndpoint ?: return@mapNotNull null
                        return@mapNotNull YtmArtist(
                            browseEndpoint.browseId!!,
                            name = run.text
                        )
                    } ?: emptyList(),
                    year = it.subtitle?.runs?.find { run -> run.text?.length == 4 }?.text?.toIntOrNull(),
                    explicit = it.subtitleBadges?.any { badge -> badge.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true
                )
            }
            ?: musicEditablePlaylistDetailHeaderRenderer?.editHeader?.musicPlaylistEditHeaderRenderer?.let {
                val (thumbnails, artist) = musicEditablePlaylistDetailHeaderRenderer.header?.musicElementHeaderRenderer?.elementRenderer?.elementRenderer?.newElement?.type?.componentType?.model?.musicBlurredBackgroundHeaderModel?.data?.let { data ->
                    val artist =
                        data.straplineData?.headerOnTapCommand?.innertubeCommand?.browseEndpoint?.browseId?.let { id ->
                            YtmArtist(id, data.straplineData.textLine1?.content)
                        }
                    data.primaryImage?.sources to artist
                } ?: (null to null)
                PlaylistData(
                    playlistId = it.playlistId,
                    privacy = it.privacy,
                    title = it.title?.runs?.firstOrNull()?.text,
                    description = it.description?.runs?.firstOrNull()?.text,
                    thumbnail = thumbnails?.let { it1 -> ThumbnailProvider.fromThumbnails(it1) },
                    artists = listOfNotNull(artist),
                    year = null,
                    isEditable = true
                )
            }

    }


    @Serializable
    data class MusicDetailHeaderRenderer(
        val title: Subtitle? = null,
        val subtitle: Subtitle? = null,
        val byline: Byline? = null,
        val menu: Menu? = null,
        val thumbnail: MusicDetailHeaderRendererThumbnail? = null,
        val subtitleBadges: List<SubtitleBadge>? = null,
        val secondTitle: SecondTitle? = null
    )

    @Serializable
    data class Byline(
        val musicDetailHeaderButtonsBylineRenderer: MusicDetailHeaderButtonsBylineRenderer? = null
    )

    @Serializable
    data class MusicDetailHeaderButtonsBylineRenderer(
        val description: Subtitle? = null
    )

    @Serializable
    data class Subtitle(
        val runs: List<SubtitleRun>? = null
    )

    @Serializable
    data class SubtitleRun(
        val text: String? = null
    )

    @Serializable
    data class Menu(
        val menuRenderer: MenuRenderer? = null
    )

    @Serializable
    data class MenuRenderer(
        val trackingParams: String? = null,
        val title: Title? = null
    )

    @Serializable
    data class Title(
        val musicMenuTitleRenderer: MusicMenuTitleRenderer? = null
    )

    @Serializable
    data class MusicMenuTitleRenderer(
        val primaryText: Subtitle? = null,
        val secondaryText: SecondaryText? = null,
        val thumbnail: MusicMenuTitleRendererThumbnail? = null
    )

    @Serializable
    data class SecondaryText(
        val runs: List<SubtitleRun>? = null,
        val accessibility: Accessibility? = null
    )

    @Serializable
    data class Accessibility(
        val accessibilityData: AccessibilityData? = null
    )

    @Serializable
    data class AccessibilityData(
        val label: String? = null
    )

    @Serializable
    data class MusicMenuTitleRendererThumbnail(
        val musicThumbnailRenderer: MusicThumbnailRenderer? = null
    )

    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: MusicThumbnailRendererThumbnail? = null,
        val thumbnailCrop: String? = null,
        val thumbnailScale: String? = null,
        val trackingParams: String? = null
    )

    @Serializable
    data class MusicThumbnailRendererThumbnail(
        val thumbnails: List<Thumbnail>? = null
    )

    @Serializable
    data class SecondTitle(
        val runs: List<SecondTitleRun>? = null
    )

    @Serializable
    data class SecondTitleRun(
        val text: String? = null,
        val navigationEndpoint: NavigationEndpoint? = null
    )

    @Serializable
    data class NavigationEndpoint(
        val clickTrackingParams: String? = null,
        val browseEndpoint: BrowseEndpoint? = null
    )

    @Serializable
    data class BrowseEndpoint(
        val browseId: String? = null,
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

    @Serializable
    data class SubtitleBadge(
        val musicInlineBadgeRenderer: MusicInlineBadgeRenderer? = null
    )

    @Serializable
    data class MusicInlineBadgeRenderer(
        val trackingParams: String? = null,
        val icon: Icon? = null,
        val accessibilityData: Accessibility? = null
    )

    @Serializable
    data class Icon(
        val iconType: String? = null
    )

    @Serializable
    data class MusicDetailHeaderRendererThumbnail(
        val croppedSquareThumbnailRenderer: CroppedSquareThumbnailRenderer? = null
    )

    @Serializable
    data class CroppedSquareThumbnailRenderer(
        val thumbnail: MusicThumbnailRendererThumbnail? = null
    )

    @Serializable
    data class MusicEditablePlaylistDetailHeaderRenderer(
        val header: Header? = null,
        val editHeader: EditHeader? = null,
        val trackingParams: String? = null,
        val playlistId: String? = null
    )

    @Serializable
    data class EditHeader(
        val musicPlaylistEditHeaderRenderer: MusicPlaylistEditHeaderRenderer? = null
    )

    @Serializable
    data class MusicPlaylistEditHeaderRenderer(
        val title: Subtitle? = null,
        val editTitle: Subtitle? = null,
        val description: Subtitle? = null,
        val editDescription: Subtitle? = null,
        val privacy: String? = null,
        val playlistId: String? = null
    )

    @Serializable
    data class MusicElementHeaderRenderer(
        val elementRenderer: MusicElementHeaderRendererElementRenderer? = null,
        val useSplitScreenLayoutOnCompatibleScreenSizes: Boolean? = null
    )

    @Serializable
    data class MusicElementHeaderRendererElementRenderer(
        val elementRenderer: ElementRendererElementRenderer? = null
    )

    @Serializable
    data class ElementRendererElementRenderer(
        val newElement: NewElement? = null
    )

    @Serializable
    data class NewElement(
        val type: Type? = null
    )

    @Serializable
    data class Type(
        val componentType: ComponentType? = null
    )

    @Serializable
    data class ComponentType(
        val model: Model? = null
    )

    @Serializable
    data class Model(
        val musicBlurredBackgroundHeaderModel: MusicBlurredBackgroundHeaderModel? = null
    )

    @Serializable
    data class MusicBlurredBackgroundHeaderModel(
        val data: Data? = null
    )

    @Serializable
    data class Data(
        val title: String? = null,
        val primaryImage: Image? = null,
        val backgroundImage: Image? = null,
        val formattedDescription: FormattedDescription? = null,
        val straplineData: StraplineData? = null,
        val titleMaxLines: Long? = null,
        val formattedTitle: FormattedTitle? = null
    )

    @Serializable
    data class Image(
        val sources: List<Thumbnail>? = null
    )

    @Serializable
    data class FormattedDescription(
        val content: String? = null,
        val lineSpacing: Long? = null,
        val styleRuns: List<StyleRun>? = null
    )

    @Serializable
    data class StyleRun(
        val startIndex: Long? = null,
        val length: Long? = null,
        val fontSize: Long? = null,
        val fontColor: Long? = null
    )

    @Serializable
    data class FormattedTitle(
        val content: String? = null
    )

    @Serializable
    data class StraplineData(
        val textLine1: TextLine? = null,
        val textLine2: TextLine? = null,
        val avatarImage: Image? = null,
        val headerOnTapCommand: HeaderOnTapCommand? = null
    )

    @Serializable
    data class HeaderOnTapCommand(
        val innertubeCommand: NavigationEndpoint? = null
    )

    @Serializable
    data class TextLine(
        val content: String? = null,
        val styleRuns: List<StyleRun>? = null
    )

}
