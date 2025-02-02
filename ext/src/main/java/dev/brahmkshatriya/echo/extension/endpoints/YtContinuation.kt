package dev.brahmkshatriya.echo.extension.endpoints

import kotlinx.serialization.Serializable

@Serializable
data class YtContinuation(
    val onResponseReceivedActions: List<OnResponseReceivedAction>
) {
    @Serializable
    data class OnResponseReceivedAction(
        val appendContinuationItemsAction: AppendContinuationItemsAction
    )

    @Serializable
    data class AppendContinuationItemsAction(
        val continuationItems: List<YoutubeiBrowseResponse.YoutubeiShelf.YoutubeiShelfContentsItem>
    )
}