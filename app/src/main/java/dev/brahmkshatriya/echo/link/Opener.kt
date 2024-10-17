package dev.brahmkshatriya.echo.link

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle


class Opener : Activity() {

    private val extensionId = "youtube-musicApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        if (uri != null) {
            val type = uri.pathSegments[0]
            val path = when (type) {
                "channel" -> {
                    val channelId = uri.pathSegments[1] ?: return
                    "artist/$channelId"
                }

                "playlist" -> {
                    val playlistId = uri.getQueryParameter("list") ?: return
                    "playlist/$playlistId"
                }

                "browse" -> {
                    val browseId = uri.pathSegments[1] ?: return
                    "album/$browseId"
                }

                "watch" -> {
                    val videoId = uri.getQueryParameter("v") ?: return
                    "track/$videoId"
                }

                else -> return
            }
            val uriString = "echo://music/$extensionId/$path"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
            finishAndRemoveTask()
        }
    }
}