package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem.Type
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem.Type.ARTIST
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem.Type.PLAYLIST
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem.Type.SONG
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.model.internal.BrowseEndpoint
import dev.toastbits.ytmkt.model.internal.FixedColumn
import dev.toastbits.ytmkt.model.internal.FlexColumn
import dev.toastbits.ytmkt.model.internal.NavigationEndpoint
import dev.toastbits.ytmkt.model.internal.TextRuns
import dev.toastbits.ytmkt.model.internal.ThumbnailRenderer
import dev.toastbits.ytmkt.radio.YoutubeiNextResponse
import dev.toastbits.ytmkt.uistrings.parseYoutubeDurationString
import kotlinx.serialization.Serializable

@Serializable
data class EchoMusicResponsiveListItemRenderer(
    val playlistItemData: RendererPlaylistItemData?,
    val flexColumns: List<FlexColumn>?,
    val fixedColumns: List<FixedColumn>?,
    val thumbnail: ThumbnailRenderer?,
    val navigationEndpoint: NavigationEndpoint?,
    val menu: YoutubeiNextResponse.Menu?,
    val index: TextRuns?,
    val badges: List<Badge>?
) {
    @Serializable
    data class Badge(val musicInlineBadgeRenderer: MusicInlineBadgeRenderer?) {
        fun isExplicit(): Boolean = musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
    }
    @Serializable
    data class MusicInlineBadgeRenderer(val icon: YoutubeiNextResponse.MenuIcon?)

    fun toMediaItemAndPlaylistSetVideoId(hl: String): Pair<YtmMediaItem, String?>? {
        var videoId: String? = playlistItemData?.videoId ?: navigationEndpoint?.watchEndpoint?.videoId
        val browseId: String? = navigationEndpoint?.browseEndpoint?.browseId
        var videoIsMain = true

        var title: String? = null
        var artists: MutableList<YtmArtist>? = null
        var playlist: YtmPlaylist? = null
        var duration: Long? = null
        var album: YtmPlaylist? = null

        if (videoId == null && browseId != null) {
            val pageType = navigationEndpoint!!.browseEndpoint!!.getType()
            when (pageType) {
                PLAYLIST -> {
                    videoIsMain = false
                    playlist = YtmPlaylist(
                        YtmPlaylist.cleanId(browseId),
                        type = YtmPlaylist.Type.fromBrowseEndpointType(
                            navigationEndpoint.browseEndpoint!!.getPageType()!!
                        )
                    )
                }
                ARTIST -> {
                    videoIsMain = false
                    artists = mutableListOf(YtmArtist(browseId))
                }
                else -> {}
            }
        }

        if (flexColumns != null) {
            for (column in flexColumns.withIndex()) {
                val text = column.value.musicResponsiveListItemFlexColumnRenderer.text
                if (text.runs == null) {
                    continue
                }

                if (column.index == 0) {
                    title = text.first_text
                }

                for (run in text.runs!!) {
                    if (run.navigationEndpoint == null) {
                        continue
                    }

                    if (run.navigationEndpoint!!.watchEndpoint != null) {
                        if (videoId == null) {
                            videoId = run.navigationEndpoint!!.watchEndpoint!!.videoId!!
                        }
                        continue
                    }

                    val browseEndpoint: BrowseEndpoint = run.navigationEndpoint!!.browseEndpoint ?: continue
                    if (browseEndpoint.browseId != null && browseEndpoint.getMediaItemType() == ARTIST) {
                        if (artists?.any { it.id == browseEndpoint.browseId } == true) {
                            continue
                        }

                        if (artists == null) {
                            artists = mutableListOf()
                        }
                        artists.add(
                            YtmArtist(
                                browseEndpoint.browseId!!,
                                name = run.text
                            )
                        )
                    }
                }
            }
        }

        if (fixedColumns != null) {
            for (column in fixedColumns) {
                val text = column.musicResponsiveListItemFixedColumnRenderer.text.first_text
                val parsed = parseYoutubeDurationString(text, hl)
                if (parsed != null) {
                    duration = parsed
                    break
                }
            }
        }

        var itemData: YtmMediaItem
        val thumbnailProvider: ThumbnailProvider? = thumbnail?.toThumbnailProvider()

        if (videoId != null) {
            val firstThumbnail = thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
            val songType: YtmSong.Type? = firstThumbnail?.let {
                if (it.height == it.width) YtmSong.Type.SONG else YtmSong.Type.VIDEO
            }

            itemData = YtmSong(
                YtmSong.cleanId(videoId),
                name = title,
                duration = duration,
                type = songType,
                is_explicit = badges?.any { it.isExplicit() } == true,
                thumbnail_provider = thumbnailProvider
            )
        }
        else if (videoIsMain) {
            return null
        }
        else {
            itemData =
                playlist?.copy(
                    total_duration = duration,
                    thumbnail_provider = thumbnailProvider,
                    name = title
                )
                    ?: artists?.firstOrNull()?.let { artist ->
                        artist.copy(
                            thumbnail_provider = thumbnailProvider,
                            name = artist.name ?: title
                        )
                    }
                            ?: return null
        }

        // Handle songs with no artist (or 'Various artists')
        if (artists == null) {
            if (flexColumns != null && flexColumns.size > 1) {
                val text = flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text
                if (text.runs != null) {
                    artists = mutableListOf(
                        YtmArtist(
                            YtmArtist.getForItemId(itemData),
                            name = text.first_text
                        )
                    )
                }
            }
        }

        for (item in menu?.menuRenderer?.items ?: emptyList()) {
            val browseEndpoint: BrowseEndpoint = (item.menuNavigationItemRenderer ?: continue).navigationEndpoint.browseEndpoint ?: continue
            if (browseEndpoint.browseId == null) {
                continue
            }

            when (browseEndpoint.getType()) {
                ARTIST -> {
                    if (artists?.any { it.id == browseEndpoint.browseId } == true) {
                        continue
                    }

                    if (artists == null) {
                        artists = mutableListOf()
                    }
                    artists.add(YtmArtist(browseEndpoint.browseId!!))
                }
                PLAYLIST -> {
                    if (album == null) {
                        album = YtmPlaylist(YtmPlaylist.cleanId(browseEndpoint.browseId!!))
                    }
                }
                else -> {}
            }
        }

        if (itemData is YtmSong) {
            itemData = itemData.copy(
                artists = artists,
                album = album
            )
        }
        else if (itemData is YtmPlaylist) {
            itemData = itemData.copy(
                artists = artists
            )
        }

        return Pair(itemData, playlistItemData?.playlistSetVideoId)
    }
}

fun BrowseEndpoint.getType(): Type? {
        // Remove "MUSIC_PAGE_TYPE_" prefix
        val typeName: String = getPageType()?.substring(16) ?: return null

        if (typeName.startsWith("ARTIST")) {
            return ARTIST
        }
        if (typeName.startsWith("PODCAST")) {
            return PLAYLIST
        }

        return when (typeName) {
            "PLAYLIST",
            "ALBUM",
            "AUDIOBOOK",
            "RADIO" ->
                PLAYLIST
            "USER_CHANNEL", "LIBRARY_ARTIST" ->
                ARTIST
            "NON_MUSIC_AUDIO_TRACK_PAGE", "UNKNOWN" ->
                SONG
            else -> null
        }
}

@Serializable
data class RendererPlaylistItemData(val videoId: String, val playlistSetVideoId: String?)
