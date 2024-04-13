@file:Suppress("unused", "SpellCheckingInspection")

package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiRequestData
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.Thumbnail
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class EchoVideoEndpoint(override val api: YtmApi) : ApiEndpoint() {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getVideo(id: String) = runCatching {
        val response: HttpResponse =
            api.client.request {
                endpointPath("player")
                addApiHeadersWithAuthenticated()
                postWithBody(YoutubeiRequestData.getYtmContextAndroidMusic(YoutubeiRequestData.default_hl)) {
                    put("videoId", id)
                    put("playlistId", null)
                }
            }
        return@runCatching response.body<YoutubeFormatResponse>()
    }
}


@Serializable
data class YoutubeFormatResponse(
    val responseContext: ResponseContext,
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData,
    val playerAds: List<PlayerAd>,
    val playbackTracking: PlaybackTracking,
    val videoDetails: YoutubeFormatResponseVideoDetails,
    val playerConfig: PlayerConfig,
    val storyboards: Storyboards,
    val microformat: Microformat,
    val trackingParams: String,
    val attestation: Attestation,
    val adPlacements: List<AdPlacement>,
    val adSlots: List<AdSlot>,
    val adBreakHeartbeatParams: String
)

@Serializable
data class AdPlacement(
    val adPlacementRenderer: AdPlacementRenderer
)

@Serializable
data class AdPlacementRenderer(
    val config: Config,
    val renderer: Renderer,
    val adSlotLoggingData: AdSlotLoggingData
)

@Serializable
data class AdSlotLoggingData(
    val serializedSlotAdServingDataEntry: String
)

@Serializable
data class Config(
    val adPlacementConfig: AdPlacementConfig
)

@Serializable
data class AdPlacementConfig(
    val kind: String,
    val adTimeOffset: AdTimeOffset,
    val hideCueRangeMarker: Boolean
)

@Serializable
data class AdTimeOffset(
    val offsetStartMilliseconds: String,
    val offsetEndMilliseconds: String
)

@Serializable
data class Renderer(
    val linearAdSequenceRenderer: LinearAdSequenceRenderer? = null,
    val adBreakServiceRenderer: AdBreakServiceRenderer? = null
)

@Serializable
data class AdBreakServiceRenderer(
    val prefetchMilliseconds: String,
    val getAdBreakUrl: String
)

@Serializable
data class LinearAdSequenceRenderer(
    val linearAds: List<LinearAd>
)

@Serializable
data class LinearAd(
    val instreamVideoAdRenderer: LinearAdInstreamVideoAdRenderer
)

@Serializable
data class LinearAdInstreamVideoAdRenderer(
    val playerOverlay: PlayerOverlay,
    val trackingParams: String,
    val layoutId: String,
    val associatedPlayerBytesLayoutId: String
)

@Serializable
data class PlayerOverlay(
    val instreamAdPlayerOverlayRenderer: InstreamAdPlayerOverlayRenderer
)

@Serializable
data class InstreamAdPlayerOverlayRenderer(
    val skipOrPreviewRenderer: SkipOrPreviewRenderer,
    val trackingParams: String,
    val visitAdvertiserRenderer: VisitAdvertiserRenderer,
    val adBadgeRenderer: AdBadgeRenderer,
    val adDurationRemaining: AdDurationRemaining,
    val adInfoRenderer: AdInfoRenderer,
    val adLayoutLoggingData: AdLayoutLoggingData,
    val elementId: String,
    val inPlayerSlotId: String,
    val inPlayerLayoutId: String
)

@Serializable
data class AdBadgeRenderer(
    val simpleAdBadgeRenderer: SimpleAdBadgeRenderer
)

@Serializable
data class SimpleAdBadgeRenderer(
    val text: StaticPreviewClass,
    val trackingParams: String
)

@Serializable
data class StaticPreviewClass(
    val text: String,
    val isTemplated: Boolean,
    val trackingParams: String
)

@Serializable
data class AdDurationRemaining(
    val adDurationRemainingRenderer: AdDurationRemainingRenderer
)

@Serializable
data class AdDurationRemainingRenderer(
    val templatedCountdown: TemplatedCountdown,
    val trackingParams: String
)

@Serializable
data class TemplatedCountdown(
    val templatedAdText: StaticPreviewClass
)

@Serializable
data class AdInfoRenderer(
    val adHoverTextButtonRenderer: AdHoverTextButtonRenderer
)

@Serializable
data class AdHoverTextButtonRenderer(
    val button: Button,
    val hoverText: HoverTextClass,
    val trackingParams: String
)

@Serializable
data class Button(
    val buttonRenderer: ButtonButtonRenderer
)

@Serializable
data class ButtonButtonRenderer(
    val style: String,
    val size: String,
    val isDisabled: Boolean,
    val serviceEndpoint: ServiceEndpoint,
    val icon: Icon,
    val trackingParams: String,
    val accessibilityData: ButtonRendererAccessibilityData
)

@Serializable
data class ButtonRendererAccessibilityData(
    val accessibilityData: AccessibilityDataAccessibilityData
)

@Serializable
data class AccessibilityDataAccessibilityData(
    val label: String
)

@Serializable
data class Icon(
    val iconType: String
)

@Serializable
data class ServiceEndpoint(
    val clickTrackingParams: String,
    val openPopupAction: OpenPopupAction
)

@Serializable
data class OpenPopupAction(
    val popup: Popup,
    val popupType: String
)

@Serializable
data class Popup(
    val aboutThisAdRenderer: AboutThisAdRenderer
)

@Serializable
data class AboutThisAdRenderer(
    val url: URL,
    val trackingParams: String
)

@Serializable
data class URL(
    val privateDoNotAccessOrElseTrustedResourceUrlWrappedValue: String
)

@Serializable
data class HoverTextClass(
    val runs: List<Run>
)

@Serializable
data class Run(
    val text: String
)

@Serializable
data class AdLayoutLoggingData(
    val serializedAdServingDataEntry: String
)

@Serializable
data class SkipOrPreviewRenderer(
    val adPreviewRenderer: SkipOrPreviewRendererAdPreviewRenderer? = null,
    val skipAdRenderer: SkipAdRenderer? = null
)

@Serializable
data class SkipOrPreviewRendererAdPreviewRenderer(
    val trackingParams: String,
    val staticPreview: StaticPreviewClass
)

@Serializable
data class SkipAdRenderer(
    val preskipRenderer: PreskipRenderer,
    val skippableRenderer: SkippableRenderer,
    val trackingParams: String,
    val skipOffsetMilliseconds: Long
)

@Serializable
data class PreskipRenderer(
    val adPreviewRenderer: PreskipRendererAdPreviewRenderer
)

@Serializable
data class PreskipRendererAdPreviewRenderer(
    val thumbnail: AdPreviewRendererThumbnail,
    val trackingParams: String,
    val templatedCountdown: TemplatedCountdown,
    val durationMilliseconds: Long
)

@Serializable
data class AdPreviewRendererThumbnail(
    val thumbnail: MicroformatDataRendererThumbnail,
    val trackingParams: String
)

@Serializable
data class MicroformatDataRendererThumbnail(
    val thumbnails: List<Thumbnail>
){
    fun provider() = ThumbnailProvider.fromThumbnails(thumbnails)
}

@Serializable
data class SkippableRenderer(
    val skipButtonRenderer: SkipButtonRenderer
)

@Serializable
data class SkipButtonRenderer(
    val message: StaticPreviewClass,
    val trackingParams: String
)

@Serializable
data class VisitAdvertiserRenderer(
    val buttonRenderer: VisitAdvertiserRendererButtonRenderer
)

@Serializable
data class VisitAdvertiserRendererButtonRenderer(
    val style: String,
    val text: HoverTextClass,
    val icon: Icon,
    val navigationEndpoint: Endpoint,
    val trackingParams: String
)

@Serializable
data class Endpoint(
    val clickTrackingParams: String,
    val urlEndpoint: URLEndpoint
)

@Serializable
data class URLEndpoint(
    val url: String,
    val target: String,
    val attributionSrcMode: String
)

@Serializable
data class AdSlot(
    val adSlotRenderer: AdSlotRenderer
)

@Serializable
data class AdSlotRenderer(
    val adSlotMetadata: AdSlotMetadata,
    val fulfillmentContent: FulfillmentContent,
    val slotEntryTrigger: SlotEntryTrigger,
    val slotFulfillmentTriggers: List<SlotFulfillmentTrigger>,
    val slotExpirationTriggers: List<SlotExpirationTrigger>
)

@Serializable
data class AdSlotMetadata(
    val slotId: String,
    val slotType: String,
    val adSlotLoggingData: AdSlotLoggingData,
    val triggerEvent: String
)

@Serializable
data class FulfillmentContent(
    val fulfilledLayout: FulfilledLayout
)

@Serializable
data class FulfilledLayout(
    val playerBytesAdLayoutRenderer: FulfilledLayoutPlayerBytesAdLayoutRenderer
)

@Serializable
data class FulfilledLayoutPlayerBytesAdLayoutRenderer(
    val adLayoutMetadata: AdLayoutMetadata,
    val renderingContent: PurpleRenderingContent,
    val layoutExitNormalTriggers: List<LayoutExitNormalTrigger>,
    val layoutExitSkipTriggers: List<LayoutExitTrigger>,
    val layoutExitMuteTriggers: List<LayoutExitTrigger>
)

@Serializable
data class AdLayoutMetadata(
    val layoutId: String,
    val layoutType: String,
    val adLayoutLoggingData: AdLayoutLoggingData
)

@Serializable
data class LayoutExitTrigger(
    val id: String,
    val skipRequestedTrigger: RequestedTrigger
)

@Serializable
data class RequestedTrigger(
    val triggeringLayoutId: String
)

@Serializable
data class LayoutExitNormalTrigger(
    val id: String,
    val onLayoutSelfExitRequestedTrigger: RequestedTrigger
)

@Serializable
data class PurpleRenderingContent(
    val playerBytesSequentialLayoutRenderer: PlayerBytesSequentialLayoutRenderer
)

@Serializable
data class PlayerBytesSequentialLayoutRenderer(
    val sequentialLayouts: List<SequentialLayout>
)

@Serializable
data class SequentialLayout(
    val playerBytesAdLayoutRenderer: SequentialLayoutPlayerBytesAdLayoutRenderer
)

@Serializable
data class SequentialLayoutPlayerBytesAdLayoutRenderer(
    val adLayoutMetadata: AdLayoutMetadata,
    val renderingContent: FluffyRenderingContent
)

@Serializable
data class FluffyRenderingContent(
    val instreamVideoAdRenderer: RenderingContentInstreamVideoAdRenderer
)

@Serializable
data class RenderingContentInstreamVideoAdRenderer(
    val pings: Pings,
    val clickthroughEndpoint: Endpoint,
    val csiParameters: List<Param>,
    val playerVars: String,
    val elementId: String,
    val trackingParams: String,
    val legacyInfoCardVastExtension: String,
    val sodarExtensionData: SodarExtensionData,
    val externalVideoId: String,
    val adLayoutLoggingData: AdLayoutLoggingData,
    val layoutId: String,
    val skipOffsetMilliseconds: Long? = null
)

@Serializable
data class Param(
    val key: String,
    val value: String
)

@Serializable
data class Pings(
    val impressionPings: List<ImpressionPing>,
    val errorPings: List<Ping>,
    val mutePings: List<Ping>,
    val unmutePings: List<Ping>,
    val pausePings: List<Ping>,
    val rewindPings: List<Ping>,
    val resumePings: List<Ping>,
    val closePings: List<Ping>,
    val clickthroughPings: List<ClickthroughPing>,
    val fullscreenPings: List<Ping>,
    val activeViewViewablePings: List<Ping>,
    val activeViewMeasurablePings: List<Ping>,
    val abandonPings: List<Ping>,
    val activeViewFullyViewableAudibleHalfDurationPings: List<Ping>,
    val startPings: List<Ping>,
    val firstQuartilePings: List<Ping>,
    val secondQuartilePings: List<Ping>,
    val thirdQuartilePings: List<Ping>,
    val completePings: List<Ping>,
    val activeViewTracking: ActiveViewTracking,
    val skipPings: List<Ping>? = null,
    val progressPings: List<ProgressPing>? = null
)

@Serializable
data class Ping(
    val baseUrl: String
)

@Serializable
data class ActiveViewTracking(
    val trafficType: String,
    val identifier: String
)

@Serializable
data class ClickthroughPing(
    val baseUrl: String,
    val attributionSrcMode: String
)

@Serializable
data class ImpressionPing(
    val baseUrl: String,
    val attributionSrcMode: String? = null,
    val headers: List<Header>? = null
)

@Serializable
data class Header(
    val headerType: HeaderType
)

@Serializable
enum class HeaderType(val value: String) {
    @SerialName("PLUS_PAGE_ID")
    PlusPageID("PLUS_PAGE_ID"),
    @SerialName("USER_AUTH")
    UserAuth("USER_AUTH"),
    @SerialName("VISITOR_ID")
    VisitorID("VISITOR_ID");
}

@Serializable
data class ProgressPing(
    val baseUrl: String,
    val offsetMilliseconds: Long,
    val attributionSrcMode: String? = null
)

@Serializable
data class SodarExtensionData(
    val siub: String,
    val bgub: String,
    val scs: String,
    val bgp: String
)

@Serializable
data class SlotEntryTrigger(
    val id: String,
    val beforeContentVideoIdStartedTrigger: DTrigger
)

@Serializable
class DTrigger

@Serializable
data class SlotExpirationTrigger(
    val id: String,
    val slotIdExitedTrigger: SlotidEedTrigger? = null,
    val onNewPlaybackAfterContentVideoIdTrigger: DTrigger? = null
)

@Serializable
data class SlotidEedTrigger(
    val triggeringSlotId: String
)

@Serializable
data class SlotFulfillmentTrigger(
    val id: String,
    val slotIdEnteredTrigger: SlotidEedTrigger
)

@Serializable
data class Attestation(
    val playerAttestationRenderer: PlayerAttestationRenderer
)

@Serializable
data class PlayerAttestationRenderer(
    val challenge: String,
    val botguardData: BotguardData
)

@Serializable
data class BotguardData(
    val program: String,
    val interpreterSafeUrl: URL,
    val serverEnvironment: Long
)

@Serializable
data class Microformat(
    val microformatDataRenderer: MicroformatDataRenderer
)

@Serializable
data class MicroformatDataRenderer(
    val urlCanonical: String,
    val title: String,
    val description: String,
    val thumbnail: MicroformatDataRendererThumbnail,
    val siteName: String,
    val appName: String,
    val androidPackage: String,
    val iosAppStoreId: String,
    val iosAppArguments: String,
    val ogType: String,
    val urlApplinksIos: String,
    val urlApplinksAndroid: String,
    val urlTwitterIos: String,
    val urlTwitterAndroid: String,
    val twitterCardType: String,
    val twitterSiteHandle: String,
    val schemaDotOrgType: String,
    val noindex: Boolean,
    val unlisted: Boolean,
    val paid: Boolean,
    val familySafe: Boolean,
    val tags: List<String>,
    val availableCountries: List<String>,
    val pageOwnerDetails: PageOwnerDetails,
    val videoDetails: MicroformatDataRendererVideoDetails,
    val linkAlternates: List<LinkAlternate>,
    val viewCount: String,
    val publishDate: String,
    val category: String,
    val uploadDate: String
)

@Serializable
data class LinkAlternate(
    val hrefUrl: String,
    val title: String? = null,
    val alternateType: String? = null
)

@Serializable
data class PageOwnerDetails(
    val name: String,
    val externalChannelId: String,
    val youtubeProfileUrl: String
)

@Serializable
data class MicroformatDataRendererVideoDetails(
    val externalVideoId: String,
    val durationSeconds: String,
    val durationIso8601: String
)

@Serializable
data class PlayabilityStatus(
    val status: String,
    val playableInEmbed: Boolean,
    val audioOnlyPlayability: AudioOnlyPlayability,
    val miniplayer: Miniplayer,
    val contextParams: String
)

@Serializable
data class AudioOnlyPlayability(
    val audioOnlyPlayabilityRenderer: AudioOnlyPlayabilityRenderer
)

@Serializable
data class AudioOnlyPlayabilityRenderer(
    val trackingParams: String,
    val audioOnlyAvailability: String
)

@Serializable
data class Miniplayer(
    val miniplayerRenderer: MiniplayerRenderer
)

@Serializable
data class MiniplayerRenderer(
    val playbackMode: String
)

@Serializable
data class PlaybackTracking(
    val videostatsPlaybackUrl: PtrackingUrlClass,
    val videostatsDelayplayUrl: PtrackingUrlClass,
    val videostatsWatchtimeUrl: PtrackingUrlClass,
    val ptrackingUrl: PtrackingUrlClass,
    val qoeUrl: PtrackingUrlClass,
    val atrUrl: AtrUrl,
    val videostatsScheduledFlushWalltimeSeconds: List<Long>,
    val videostatsDefaultFlushIntervalSeconds: Long
)

@Serializable
data class AtrUrl(
    val baseUrl: String,
    val elapsedMediaTimeSeconds: Long,
    val headers: List<Header>
)

@Serializable
data class PtrackingUrlClass(
    val baseUrl: String,
    val headers: List<Header>
)

@Serializable
data class PlayerAd(
    val playerLegacyDesktopWatchAdsRenderer: PlayerLegacyDesktopWatchAdsRenderer
)

@Serializable
data class PlayerLegacyDesktopWatchAdsRenderer(
    val playerAdParams: PlayerAdParams,
    val gutParams: GutParams,
    val showCompanion: Boolean,
    val showInstream: Boolean,
    val useGut: Boolean
)

@Serializable
data class GutParams(
    val tag: String
)

@Serializable
data class PlayerAdParams(
    val showContentThumbnail: Boolean,
    val enabledEngageTypes: String
)

@Serializable
data class PlayerConfig(
    val audioConfig: AudioConfig,
    val streamSelectionConfig: StreamSelectionConfig,
    val mediaCommonConfig: MediaCommonConfig,
    val webPlayerConfig: WebPlayerConfig
)

@Serializable
data class AudioConfig(
    val loudnessDb: Double,
    val perceptualLoudnessDb: Double,
    val enablePerFormatLoudness: Boolean
)

@Serializable
data class MediaCommonConfig(
    val dynamicReadaheadConfig: DynamicReadaheadConfig
)

@Serializable
data class DynamicReadaheadConfig(
    val maxReadAheadMediaTimeMs: Long,
    val minReadAheadMediaTimeMs: Long,
    val readAheadGrowthRateMs: Long
)

@Serializable
data class StreamSelectionConfig(
    val maxBitrate: String
)

@Serializable
data class WebPlayerConfig(
    val useCobaltTvosDash: Boolean,
    val webPlayerActionsPorting: WebPlayerActionsPorting
)

@Serializable
data class WebPlayerActionsPorting(
    val subscribeCommand: SubscribeCommand,
    val unsubscribeCommand: UnsubscribeCommand,
    val addToWatchLaterCommand: AddToWatchLaterCommand,
    val removeFromWatchLaterCommand: RemoveFromWatchLaterCommand
)

@Serializable
data class AddToWatchLaterCommand(
    val clickTrackingParams: String,
    val playlistEditEndpoint: AddToWatchLaterCommandPlaylistEditEndpoint
)

@Serializable
data class AddToWatchLaterCommandPlaylistEditEndpoint(
    val playlistId: String,
    val actions: List<PurpleAction>
)

@Serializable
data class PurpleAction(
    val addedVideoId: String,
    val action: String
)

@Serializable
data class RemoveFromWatchLaterCommand(
    val clickTrackingParams: String,
    val playlistEditEndpoint: RemoveFromWatchLaterCommandPlaylistEditEndpoint
)

@Serializable
data class RemoveFromWatchLaterCommandPlaylistEditEndpoint(
    val playlistId: String,
    val actions: List<FluffyAction>
)

@Serializable
data class FluffyAction(
    val action: String,
    val removedVideoId: String
)

@Serializable
data class SubscribeCommand(
    val clickTrackingParams: String,
    val subscribeEndpoint: SubscribeEndpoint
)

@Serializable
data class SubscribeEndpoint(
    val channelIds: List<String>,
    val params: String
)

@Serializable
data class UnsubscribeCommand(
    val clickTrackingParams: String,
    val unsubscribeEndpoint: SubscribeEndpoint
)

@Serializable
data class ResponseContext(
    val serviceTrackingParams: List<ServiceTrackingParam>,
    val maxAgeSeconds: Long
)

@Serializable
data class ServiceTrackingParam(
    val service: String,
    val params: List<Param>
)

@Serializable
data class Storyboards(
    val playerStoryboardSpecRenderer: PlayerStoryboardSpecRenderer
)

@Serializable
data class PlayerStoryboardSpecRenderer(
    val spec: String,
    val recommendedLevel: Long
)

@Serializable
data class StreamingData(
    val expiresInSeconds: String,
    val formats: List<Format>,
    val adaptiveFormats: List<AdaptiveFormat>
)

@Serializable
data class AdaptiveFormat(
    val itag: Long,
    val mimeType: String,
    val bitrate: Long,
    val width: Long? = null,
    val height: Long? = null,
    val initRange: Range,
    val indexRange: Range,
    val lastModified: String,
    val contentLength: String,
    val quality: String,
    val fps: Long? = null,
    val qualityLabel: String? = null,
    val projectionType: ProjectionType,
    val averageBitrate: Long,
    val approxDurationMs: String,
    val signatureCipher: String,
    val colorInfo: ColorInfo? = null,
    val highReplication: Boolean? = null,
    val audioQuality: String? = null,
    val audioSampleRate: String? = null,
    val audioChannels: Long? = null,
    val loudnessDb: Double? = null
)

@Serializable
data class ColorInfo(
    val primaries: String,
    val transferCharacteristics: String,
    val matrixCoefficients: String
)

@Serializable
data class Range(
    val start: String,
    val end: String
)

@Serializable
enum class ProjectionType(val value: String) {
    @SerialName("RECTANGULAR")
    Rectangular("RECTANGULAR");
}

@Serializable
data class Format(
    val itag: Long,
    val mimeType: String,
    val bitrate: Long,
    val width: Long,
    val height: Long,
    val lastModified: String,
    val quality: String,
    val fps: Long,
    val qualityLabel: String,
    val projectionType: ProjectionType,
    val audioQuality: String,
    val approxDurationMs: String,
    val audioSampleRate: String,
    val audioChannels: Long,
    val signatureCipher: String
)

@Serializable
data class YoutubeFormatResponseVideoDetails(
    val videoId: String,
    val title: String,
    val lengthSeconds: String,
    val channelId: String,
    val isOwnerViewing: Boolean,
    val isCrawlable: Boolean,
    val thumbnail: MicroformatDataRendererThumbnail,
    val allowRatings: Boolean,
    val viewCount: String,
    val author: String,
    val isPrivate: Boolean,
    val isUnpluggedCorpus: Boolean,
    val musicVideoType: String,
    val isLiveContent: Boolean
)

