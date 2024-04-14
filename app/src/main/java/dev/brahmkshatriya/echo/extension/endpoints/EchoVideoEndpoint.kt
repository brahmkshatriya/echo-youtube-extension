@file:Suppress("unused", "SpellCheckingInspection")

package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiRequestData
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.YtmApi
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

class EchoVideoEndpoint(override val api: YtmApi) : ApiEndpoint() {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getVideo(id: String) = runCatching {
        val response: HttpResponse = api.client.request {
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
    val streamingData: StreamingData,
    val videoDetails: VideoDetails,
)

@Serializable
data class Attestation(
    val playerAttestationRenderer: PlayerAttestationRenderer? = null
)

@Serializable
data class PlayerAttestationRenderer(
    val challenge: String? = null
)

@Serializable
data class PlayabilityStatus(
    val status: String? = null,
    val playableInEmbed: Boolean? = null,
    val offlineability: Offlineability? = null,
    val backgroundability: Backgroundability? = null,
    val audioOnlyPlayability: AudioOnlyPlayability? = null,
    val miniplayer: Miniplayer? = null,
    val contextParams: String? = null
)

@Serializable
data class AudioOnlyPlayability(
    val audioOnlyPlayabilityRenderer: AudioOnlyPlayabilityRenderer? = null
)

@Serializable
data class AudioOnlyPlayabilityRenderer(
    val trackingParams: String? = null,
    val audioOnlyAvailability: String? = null
)

@Serializable
data class Backgroundability(
    val backgroundabilityRenderer: BackgroundabilityRenderer? = null
)

@Serializable
data class BackgroundabilityRenderer(
    val backgroundable: Boolean? = null
)

@Serializable
data class Miniplayer(
    val miniplayerRenderer: MiniplayerRenderer? = null
)

@Serializable
data class MiniplayerRenderer(
    val playbackMode: String? = null
)

@Serializable
data class Offlineability(
    val offlineabilityRenderer: OfflineabilityRenderer? = null
)

@Serializable
data class OfflineabilityRenderer(
    val offlineable: Boolean? = null,
    val clickTrackingParams: String? = null
)

@Serializable
data class PlaybackTracking(
    val videostatsPlaybackUrl: URL? = null,
    val videostatsDelayplayUrl: URL? = null,
    val videostatsWatchtimeUrl: URL? = null,
    val ptrackingUrl: URL? = null,
    val qoeUrl: URL? = null,
    val atrUrl: AtrUrl? = null,
    val videostatsScheduledFlushWalltimeSeconds: List<Long>? = null,
    val videostatsDefaultFlushIntervalSeconds: Long? = null
)

@Serializable
data class AtrUrl(
    val baseUrl: String? = null,
    val elapsedMediaTimeSeconds: Long? = null,
    val headers: List<Header>? = null
)

@Serializable
data class Header(
    val headerType: HeaderType? = null
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
data class URL(
    val baseUrl: String? = null,
    val headers: List<Header>? = null
)

@Serializable
data class PlayerConfig(
    val audioConfig: AudioConfig? = null,
    val exoPlayerConfig: ExoPlayerConfig? = null,
    val adRequestConfig: AdRequestConfig? = null,
    val networkProtocolConfig: NetworkProtocolConfig? = null,
    val androidNetworkStackConfig: AndroidNetworkStackConfig? = null,
    val lidarSdkConfig: LidarSdkConfig? = null,
    val androidMedialibConfig: AndroidMedialibConfig? = null,
    val variableSpeedConfig: VariableSpeedConfig? = null,
    val decodeQualityConfig: DecodeQualityConfig? = null,
    val playerRestorationConfig: PlayerRestorationConfig? = null,
    val androidPlayerStatsConfig: AndroidPlayerStatsConfig? = null,
    val retryConfig: RetryConfig? = null,
    val cmsPathProbeConfig: CMSPathProbeConfig? = null,
    val mediaCommonConfig: MediaCommonConfig? = null,
    val taskCoordinatorConfig: TaskCoordinatorConfig? = null
)

@Serializable
data class AdRequestConfig(
    val useCriticalExecOnAdsPrep: Boolean? = null,
    val userCriticalExecOnAdsProcessing: Boolean? = null
)

@Serializable
data class AndroidMedialibConfig(
    val isItag18MainProfile: Boolean? = null,
    val viewportSizeFraction: Double? = null
)

@Serializable
data class AndroidNetworkStackConfig(
    val networkStack: String? = null,
    val androidMetadataNetworkConfig: AndroidMetadataNetworkConfig? = null
)

@Serializable
data class AndroidMetadataNetworkConfig(
    val coalesceRequests: Boolean? = null
)

@Serializable
data class AndroidPlayerStatsConfig(
    val usePblForAttestationReporting: Boolean? = null,
    val usePblForHeartbeatReporting: Boolean? = null,
    val usePblForPlaybacktrackingReporting: Boolean? = null,
    val usePblForQoeReporting: Boolean? = null,
    val changeCpnOnFatalPlaybackError: Boolean? = null
)

@Serializable
data class AudioConfig(
    val loudnessDb: Double? = null,
    val perceptualLoudnessDb: Double? = null,
    val playAudioOnly: Boolean? = null,
    val enablePerFormatLoudness: Boolean? = null
)

@Serializable
data class CMSPathProbeConfig(
    val cmsPathProbeDelayMs: Long? = null
)

@Serializable
data class DecodeQualityConfig(
    val maximumVideoDecodeVerticalResolution: Long? = null
)

@Serializable
data class ExoPlayerConfig(
    val useExoPlayer: Boolean? = null,
    val useAdaptiveBitrate: Boolean? = null,
    val maxInitialByteRate: Long? = null,
    val minDurationForQualityIncreaseMs: Long? = null,
    val maxDurationForQualityDecreaseMs: Long? = null,
    val minDurationToRetainAfterDiscardMs: Long? = null,
    val lowWatermarkMs: Long? = null,
    val highWatermarkMs: Long? = null,
    val lowPoolLoad: Double? = null,
    val highPoolLoad: Double? = null,
    val sufficientBandwidthOverhead: Double? = null,
    val bufferChunkSizeKb: Long? = null,
    val httpConnectTimeoutMs: Long? = null,
    val httpReadTimeoutMs: Long? = null,
    val numAudioSegmentsPerFetch: Long? = null,
    val numVideoSegmentsPerFetch: Long? = null,
    val minDurationForPlaybackStartMs: Long? = null,
    val enableExoplayerReuse: Boolean? = null,
    val useRadioTypeForInitialQualitySelection: Boolean? = null,
    val blacklistFormatOnError: Boolean? = null,
    val enableBandaidHttpDataSource: Boolean? = null,
    val httpLoadTimeoutMs: Long? = null,
    val canPlayHdDrm: Boolean? = null,
    val videoBufferSegmentCount: Long? = null,
    val audioBufferSegmentCount: Long? = null,
    val useAbruptSplicing: Boolean? = null,
    val minRetryCount: Long? = null,
    val minChunksNeededToPreferOffline: Long? = null,
    val secondsToMaxAggressiveness: Long? = null,
    val enableSurfaceviewResizeWorkaround: Boolean? = null,
    val enableVp9IfThresholdsPass: Boolean? = null,
    val matchQualityToViewportOnUnfullscreen: Boolean? = null,
    val lowAudioQualityConnTypes: List<String>? = null,
    val useDashForLiveStreams: Boolean? = null,
    val enableLibvpxVideoTrackRenderer: Boolean? = null,
    val lowAudioQualityBandwidthThresholdBps: Long? = null,
    val enableVariableSpeedPlayback: Boolean? = null,
    val preferOnesieBufferedFormat: Boolean? = null,
    val minimumBandwidthSampleBytes: Long? = null,
    val useDashForOtfAndCompletedLiveStreams: Boolean? = null,
    val disableCacheAwareVideoFormatEvaluation: Boolean? = null,
    val useLiveDvrForDashLiveStreams: Boolean? = null,
    val cronetResetTimeoutOnRedirects: Boolean? = null,
    val emitVideoDecoderChangeEvents: Boolean? = null,
    val onesieVideoBufferLoadTimeoutMs: String? = null,
    val onesieVideoBufferReadTimeoutMs: String? = null,
    val libvpxEnableGl: Boolean? = null,
    val enableVp9EncryptedIfThresholdsPass: Boolean? = null,
    val enableOpus: Boolean? = null,
    val usePredictedBuffer: Boolean? = null,
    val maxReadAheadMediaTimeMs: Long? = null,
    val useMediaTimeCappedLoadControl: Boolean? = null,
    val allowCacheOverrideToLowerQualitiesWithinRange: Long? = null,
    val allowDroppingUndecodedFrames: Boolean? = null,
    val minDurationForPlaybackRestartMs: Long? = null,
    val serverProvidedBandwidthHeader: String? = null,
    val liveOnlyPegStrategy: String? = null,
    val enableRedirectorHostFallback: Boolean? = null,
    val enableHighlyAvailableFormatFallbackOnPcr: Boolean? = null,
    val recordTrackRendererTimingEvents: Boolean? = null,
    val minErrorsForRedirectorHostFallback: Long? = null,
    val nonHardwareMediaCodecNames: List<String>? = null,
    val enableVp9IfInHardware: Boolean? = null,
    val enableVp9EncryptedIfInHardware: Boolean? = null,
    val useOpusMedAsLowQualityAudio: Boolean? = null,
    val minErrorsForPcrFallback: Long? = null,
    val useStickyRedirectHttpDataSource: Boolean? = null,
    val onlyVideoBandwidth: Boolean? = null,
    val useRedirectorOnNetworkChange: Boolean? = null,
    val enableMaxReadaheadAbrThreshold: Boolean? = null,
    val cacheCheckDirectoryWritabilityOnce: Boolean? = null,
    val predictorType: String? = null,
    val slidingPercentile: Double? = null,
    val slidingWindowSize: Long? = null,
    val maxFrameDropIntervalMs: Long? = null,
    val ignoreLoadTimeoutForFallback: Boolean? = null,
    val serverBweMultiplier: Long? = null,
    val drmMaxKeyfetchDelayMs: Long? = null,
    val maxResolutionForWhiteNoise: Long? = null,
    val whiteNoiseRenderEffectMode: String? = null,
    val enableLibvpxHdr: Boolean? = null,
    val enableCacheAwareStreamSelection: Boolean? = null,
    val useExoCronetDataSource: Boolean? = null,
    val whiteNoiseScale: Long? = null,
    val whiteNoiseOffset: Long? = null,
    val preventVideoFrameLaggingWithLibvpx: Boolean? = null,
    val enableMediaCodecHdr: Boolean? = null,
    val enableMediaCodecSwHdr: Boolean? = null,
    val liveOnlyWindowChunks: Long? = null,
    val bearerMinDurationToRetainAfterDiscardMs: List<Long>? = null,
    val forceWidevineL3: Boolean? = null,
    val useAverageBitrate: Boolean? = null,
    val useMedialibAudioTrackRendererForLive: Boolean? = null,
    val useExoPlayerV2: Boolean? = null,
    val logMediaRequestEventsToCsi: Boolean? = null,
    val onesieFixNonZeroStartTimeFormatSelection: Boolean? = null,
    val liveOnlyReadaheadStepSizeChunks: Long? = null,
    val liveOnlyBufferHealthHalfLifeSeconds: Long? = null,
    val liveOnlyMinBufferHealthRatio: Double? = null,
    val liveOnlyMinLatencyToSeekRatio: Long? = null,
    val manifestlessPartialChunkStrategy: String? = null,
    val ignoreViewportSizeWhenSticky: Boolean? = null,
    val enableLibvpxFallback: Boolean? = null,
    val disableLibvpxLoopFilter: Boolean? = null,
    val enableVpxMediaView: Boolean? = null,
    val hdrMinScreenBrightness: Long? = null,
    val hdrMaxScreenBrightnessThreshold: Long? = null,
    val onesieDataSourceAboveCacheDataSource: Boolean? = null,
    val httpNonplayerLoadTimeoutMs: Long? = null,
    val numVideoSegmentsPerFetchStrategy: String? = null,
    val maxVideoDurationPerFetchMs: Long? = null,
    val maxVideoEstimatedLoadDurationMs: Long? = null,
    val estimatedServerClockHalfLife: Long? = null,
    val estimatedServerClockStrictOffset: Boolean? = null,
    val minReadAheadMediaTimeMs: Long? = null,
    val readAheadGrowthRate: Long? = null,
    val useDynamicReadAhead: Boolean? = null,
    val useYtVodMediaSourceForV2: Boolean? = null,
    val enableV2Gapless: Boolean? = null,
    val useLiveHeadTimeMillis: Boolean? = null,
    val allowTrackSelectionWithUpdatedVideoItagsForExoV2: Boolean? = null,
    val maxAllowableTimeBeforeMediaTimeUpdateSec: Long? = null,
    val enableDynamicHdr: Boolean? = null,
    val v2PerformEarlyStreamSelection: Boolean? = null,
    val v2UsePlaybackStreamSelectionResult: Boolean? = null,
    val v2MinTimeBetweenAbrReevaluationMs: Long? = null,
    val avoidReusePlaybackAcrossLoadvideos: Boolean? = null,
    val enableInfiniteNetworkLoadingRetries: Boolean? = null,
    val reportExoPlayerStateOnTransition: Boolean? = null,
    val manifestlessSequenceMethod: String? = null,
    val useLiveHeadWindow: Boolean? = null,
    val enableDynamicHdrInHardware: Boolean? = null,
    val ultralowAudioQualityBandwidthThresholdBps: Long? = null,
    val retryLiveNetNocontentWithDelay: Boolean? = null,
    val ignoreUnneededSeeksToLiveHead: Boolean? = null,
    val drmMetricsQoeLoggingFraction: Double? = null,
    val liveNetNocontentMaximumErrors: Long? = null,
    val slidingPercentileScalar: Long? = null,
    val minAdaptiveVideoQuality: Long? = null,
    val platypusBackBufferDurationMs: Long? = null
)

@Serializable
data class LidarSdkConfig(
    val enableActiveViewReporter: Boolean? = null,
    val useMediaTime: Boolean? = null,
    val sendTosMetrics: Boolean? = null,
    val usePlayerState: Boolean? = null,
    val enableIosAppStateCheck: Boolean? = null,
    val enableIsAndroidVideoAlwaysMeasurable: Boolean? = null,
    val enableActiveViewAudioMeasurementAndroid: Boolean? = null
)

@Serializable
data class MediaCommonConfig(
    val dynamicReadaheadConfig: DynamicReadaheadConfig? = null,
    val mediaUstreamerRequestConfig: MediaUstreamerRequestConfig? = null,
    val predictedReadaheadConfig: PredictedReadaheadConfig? = null,
    val mediaFetchRetryConfig: MediaFetchRetryConfig? = null,
    val mediaFetchMaximumServerErrors: Long? = null,
    val mediaFetchMaximumNetworkErrors: Long? = null,
    val mediaFetchMaximumErrors: Long? = null,
    val serverReadaheadConfig: ServerReadaheadConfig? = null,
    val useServerDrivenAbr: Boolean? = null
)

@Serializable
data class DynamicReadaheadConfig(
    val maxReadAheadMediaTimeMs: Long? = null,
    val minReadAheadMediaTimeMs: Long? = null,
    val readAheadGrowthRateMs: Long? = null,
    val readAheadWatermarkMarginRatio: Long? = null,
    val minReadAheadWatermarkMarginMs: Long? = null,
    val maxReadAheadWatermarkMarginMs: Long? = null,
    val shouldIncorporateNetworkActiveState: Boolean? = null
)

@Serializable
data class MediaFetchRetryConfig(
    val initialDelayMs: Long? = null,
    val backoffFactor: Double? = null,
    val maximumDelayMs: Long? = null,
    val jitterFactor: Double? = null
)

@Serializable
data class MediaUstreamerRequestConfig(
    val enableVideoPlaybackRequest: Boolean? = null,
    val videoPlaybackUstreamerConfig: String? = null,
    val videoPlaybackPostEmptyBody: Boolean? = null,
    val isVideoPlaybackRequestIdempotent: Boolean? = null
)

@Serializable
data class PredictedReadaheadConfig(
    val minReadaheadMs: Long? = null,
    val maxReadaheadMs: Long? = null
)

@Serializable
data class ServerReadaheadConfig(
    val enable: Boolean? = null,
    val nextRequestPolicy: NextRequestPolicy? = null
)

@Serializable
data class NextRequestPolicy(
    val targetAudioReadaheadMs: Long? = null,
    val targetVideoReadaheadMs: Long? = null
)

@Serializable
data class NetworkProtocolConfig(
    val useQuic: Boolean? = null
)

@Serializable
data class PlayerRestorationConfig(
    val restoreIntoStoppedState: Boolean? = null
)

@Serializable
data class RetryConfig(
    val retryEligibleErrors: List<String>? = null,
    val retryUnderSameConditionAttempts: Long? = null,
    val retryWithNewSurfaceAttempts: Long? = null,
    val progressiveFallbackOnNonNetworkErrors: Boolean? = null,
    val l3FallbackOnDrmErrors: Boolean? = null,
    val retryAfterCacheRemoval: Boolean? = null,
    val widevineL3EnforcedFallbackOnDrmErrors: Boolean? = null,
    val exoProxyableFormatFallback: Boolean? = null,
    val maxPlayerRetriesWhenNetworkUnavailable: Long? = null,
    val suppressFatalErrorAfterStop: Boolean? = null,
    val fallbackToSwDecoderOnFormatDecodeError: Boolean? = null
)

@Serializable
data class TaskCoordinatorConfig(
    val prefetchCoordinatorBufferedPositionMillisRelease: Long? = null
)

@Serializable
data class VariableSpeedConfig(
    val showVariableSpeedDisabledDialog: Boolean? = null
)

@Serializable
data class ResponseContext(
    val visitorData: String? = null,
    val serviceTrackingParams: List<ServiceTrackingParam>? = null,
    val maxAgeSeconds: Long? = null
)

@Serializable
data class ServiceTrackingParam(
    val service: String? = null,
    val params: List<Param>? = null
)

@Serializable
data class Param(
    val key: String? = null,
    val value: String? = null
)

@Serializable
data class Storyboards(
    val playerStoryboardSpecRenderer: PlayerStoryboardSpecRenderer? = null
)

@Serializable
data class PlayerStoryboardSpecRenderer(
    val spec: String? = null,
    val recommendedLevel: Long? = null
)

@Serializable
data class StreamingData(
    val expiresInSeconds: String,
    val formats: List<Format>,
    val adaptiveFormats: List<AdaptiveFormat>
)

@Serializable
data class AdaptiveFormat(
    val itag: Long? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val bitrate: Int,
    val width: Long? = null,
    val height: Long? = null,
    val initRange: Range? = null,
    val indexRange: Range? = null,
    val lastModified: String? = null,
    val contentLength: String? = null,
    val quality: String? = null,
    val fps: Long? = null,
    val qualityLabel: String? = null,
    val projectionType: ProjectionType? = null,
    val averageBitrate: Long? = null,
    val approxDurationMs: String? = null,
    val colorInfo: ColorInfo? = null,
    val highReplication: Boolean? = null,
    val audioQuality: String? = null,
    val audioSampleRate: String? = null,
    val audioChannels: Long? = null,
    val loudnessDb: Double? = null
)

@Serializable
data class ColorInfo(
    val primaries: String? = null,
    val transferCharacteristics: String? = null,
    val matrixCoefficients: String? = null
)

@Serializable
data class Range(
    val start: String? = null,
    val end: String? = null
)

@Serializable
enum class ProjectionType(val value: String) {
    @SerialName("RECTANGULAR")
    Rectangular("RECTANGULAR");
}

@Serializable
data class Format(
    val itag: Long? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val bitrate: Int,
    val width: Long? = null,
    val height: Long? = null,
    val lastModified: String? = null,
    val quality: String? = null,
    val fps: Long? = null,
    val qualityLabel: String? = null,
    val projectionType: ProjectionType? = null,
    val audioQuality: String? = null,
    val approxDurationMs: String? = null,
    val audioSampleRate: String? = null,
    val audioChannels: Long? = null
)

@Serializable
data class VideoDetails(
    val videoId: String? = null,
    val title: String? = null,
    val lengthSeconds: String? = null,
    val channelId: String? = null,
    val isOwnerViewing: Boolean? = null,
    val isCrawlable: Boolean? = null,
    val thumbnail: VideoDetailsThumbnail? = null,
    val allowRatings: Boolean? = null,
    val viewCount: String? = null,
    val author: String? = null,
    val isPrivate: Boolean? = null,
    val isUnpluggedCorpus: Boolean? = null,
    val musicVideoType: String? = null,
    val isLiveContent: Boolean? = null
)

@Serializable
data class VideoDetailsThumbnail(
    val thumbnails: List<ThumbnailElement>? = null
)

@Serializable
data class ThumbnailElement(
    val url: String? = null,
    val width: Long? = null,
    val height: Long? = null
)