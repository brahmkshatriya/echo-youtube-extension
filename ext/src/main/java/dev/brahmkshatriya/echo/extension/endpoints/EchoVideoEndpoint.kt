@file:Suppress("unused")

package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EchoVideoEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    private suspend fun request(
        context: JsonObject,
        id: String,
        playlist: String? = null
    ): HttpResponse {
        return api.client.request {
            endpointPath("player")
            addApiHeadersWithAuthenticated()
            postWithBody(context) {
                put("videoId", id)
                put("playlistId", playlist)
            }
        }
    }

    suspend fun getVideo(id: String, playlist: String? = null) = coroutineScope {
//        val desc = run {
//            val req = request(YoutubeiPostBody.WEB.getPostBody(api), id, playlist)
//            println(req.bodyAsText())
//            req.body<YoutubeFormatResponse>()
//                .microformat!!.playerMicroformatRenderer.description.simpleText
//        }
        val response = request(context, id, playlist).body<YoutubeFormatResponse>()
        response to response.videoDetails.shortDescription
    }

    private val context = buildJsonObject {
        put("context", buildJsonObject {
            put("client", buildJsonObject {
//                put("clientName", "IOS")
//                put("clientVersion", "19.29.1")
                put("clientName", "5")
                put("clientVersion", "19.34.2")
            })
        })
    }
}

@Serializable
data class YoutubeFormatResponse(
    val streamingData: StreamingData,
    val videoDetails: VideoDetails,
    val microformat: Microformat? = null
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
    val hlsManifestUrl: String?,
    val adaptiveFormats: List<AdaptiveFormat>
)

@Serializable
data class AdaptiveFormat(
    val itag: Long? = null,
    val url: String? = null,
    val mimeType: String,
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
    val videoId: String,
    val title: String?,
    val lengthSeconds: String,
    val channelId: String,
    val isOwnerViewing: Boolean? = null,
    val isCrawlable: Boolean? = null,
    val thumbnail: VideoDetailsThumbnail? = null,
    val allowRatings: Boolean? = null,
    val viewCount: String? = null,
    val author: String,
    val isPrivate: Boolean? = null,
    val isUnpluggedCorpus: Boolean? = null,
    val musicVideoType: String? = null,
    val isLiveContent: Boolean? = null,
    val shortDescription: String? = null,
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

@Serializable
data class Microformat (
    val playerMicroformatRenderer: PlayerMicroformatRenderer
)

@Serializable
data class PlayerMicroformatRenderer (
    val thumbnail: PlayerMicroformatRendererThumbnail,
    val embed: Embed,
    val title: Description,
    val description: Description,
    val lengthSeconds: String,
    val ownerProfileUrl: String,
    val externalChannelId: String,
    val isFamilySafe: Boolean,
    val availableCountries: List<String>,
    val isUnlisted: Boolean,
    val hasYpcMetadata: Boolean,
    val viewCount: String,
    val category: String,
    val publishDate: String,
    val ownerChannelName: String,
    val uploadDate: String,
    val isShortsEligible: Boolean
)

@Serializable
data class Description (
    val simpleText: String
)

@Serializable
data class Embed (
    val iframeUrl: String,
    val width: Long,
    val height: Long
)

@Serializable
data class PlayerMicroformatRendererThumbnail (
    val thumbnails: List<ThumbnailElement>
)