package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import io.ktor.client.call.body
import io.ktor.client.request.request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EchoLyricsEndPoint(override val api: YoutubeiApi) : ApiEndpoint() {
    suspend fun getLyrics(id: String): List<TimedLyricsDatum>? {
        val response = api.client.request {
            endpointPath("browse")
            addApiHeadersWithAuthenticated()
            addApiHeadersWithoutAuthentication(PLAIN_HEADERS)
            postWithBody(context) {
                put("browseId", id)
            }
        }

        val data = response.body<LyricsResponse>()
        return data.contents?.elementRenderer?.newElement?.type?.componentType?.model?.timedLyricsModel?.lyricsData?.timedLyricsData
    }

    private val context = buildJsonObject {
        put("context", buildJsonObject {
            put("client", buildJsonObject {
                put("clientName", "26")
                put("clientVersion", "6.48.2")
            })
        })
    }
}

@Serializable
data class LyricsResponse(
    val contents: Contents? = null
)

@Serializable
data class Contents(
    val elementRenderer: ElementRenderer? = null
)

@Serializable
data class ElementRenderer(
    val trackingParams: String? = null,
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
    val timedLyricsModel: TimedLyricsModel? = null
)

@Serializable
data class TimedLyricsModel(
    val lyricsData: LyricsData? = null
)

@Serializable
data class LyricsData(
    val timedLyricsData: List<TimedLyricsDatum>? = null,
    val sourceMessage: String? = null,
    val trackingParams: String? = null,
    val disableTapToSeek: Boolean? = null,
    val loggingCommand: Command? = null,
    val colorSamplePaletteEntityKey: String? = null,
    val backgroundImage: BackgroundImage? = null,
    val enableDirectUpdateProperties: Boolean? = null,
    val timedLyricsCommand: Command? = null,
    val collectionKey: String? = null
)

@Serializable
data class BackgroundImage(
    val sources: List<Source>? = null
)

@Serializable
data class Source(
    val url: String? = null
)

@Serializable
data class Command(
    val clickTrackingParams: String? = null,
    val logLyricEventCommand: LogLyricEventCommand? = null
)

@Serializable
data class LogLyricEventCommand(
    val serializedLyricInfo: String? = null
)

@Serializable
data class TimedLyricsDatum(
    val lyricLine: String? = null,
    val cueRange: CueRange? = null
)

@Serializable
data class CueRange(
    val startTimeMilliseconds: String? = null,
    val endTimeMilliseconds: String? = null,
    val metadata: Metadata? = null
)

@Serializable
data class Metadata(
    val id: String? = null
)