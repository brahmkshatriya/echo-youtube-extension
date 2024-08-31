package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import io.ktor.client.call.body
import io.ktor.client.request.request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class EchoEditPlaylistEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    sealed class Action {
        data class Add(val id: String) : Action()
        data class Remove(val id: String, val setId: String) : Action()
        data class Move(val setId: String, val aboveVideoSetId: String?) : Action()
    }

    suspend fun editPlaylist(playlistId: String, actions: List<Action>) = run {
        api.client.request {
        endpointPath("browse/edit_playlist")
        addApiHeadersWithAuthenticated()
        postWithBody {
            put("playlistId", playlistId.removePrefix("VL"))
            putJsonArray("actions") {
                actions.forEach {
                    add(getActionRequestData(it))
                }
            }
        }
    }.body<EditorResponse>()
    }

    private fun getActionRequestData(action: Action): JsonObject {
        return when (action) {

            is Action.Add -> buildJsonObject {
                put("action", "ACTION_ADD_VIDEO")
                put("addedVideoId", action.id)
                put("dedupeOption", "DEDUPE_OPTION_SKIP")
            }

            is Action.Move -> buildJsonObject {
                put("action", "ACTION_MOVE_VIDEO_BEFORE")
                put("setVideoId", action.setId)
                action.aboveVideoSetId?.let { put("movedSetVideoIdSuccessor", it) }
            }

            is Action.Remove -> buildJsonObject {
                put("action", "ACTION_REMOVE_VIDEO")
                put("removedVideoId", action.id)
                put("setVideoId", action.setId)
            }
        }
    }

    @Serializable
    data class EditorResponse (
        val playlistEditResults: List<PlaylistEditResult>?
    )

    @Serializable
    data class PlaylistEditResult (
        val playlistEditVideoAddedResultData: PlaylistEditVideoAddedResultData
    )

    @Serializable
    data class PlaylistEditVideoAddedResultData (
        val videoId: String,
        val setVideoId: String,
        val multiSelectData: MultiSelectData
    )

    @Serializable
    data class MultiSelectData (
        val multiSelectParams: String,
        val multiSelectItem: String
    )
}