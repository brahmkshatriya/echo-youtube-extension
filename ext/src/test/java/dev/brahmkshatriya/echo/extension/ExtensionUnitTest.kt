package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
class ExtensionUnitTest {
    private val extension: ExtensionClient = YoutubeExtension()
    private val searchQuery = "Skrillex"

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        extension.setSettings(MockedSettings())
        runBlocking { extension.onExtensionSelected() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun testIn(title: String, block: suspend CoroutineScope.() -> Unit) = runBlocking {
        println("\n-- $title --")
        block.invoke(this)
        println("\n")
    }

    @Test
    fun testHomeFeed() = testIn("Testing Home Feed") {
        if (extension !is HomeFeedClient) error("HomeFeedClient is not implemented")
        val feed = extension.getHomeFeed(null).loadFirst()
        feed.forEach {
            println(it)
        }
    }

    @Test
    fun testHomeFeedWithTab() = testIn("Testing Home Feed with Tab") {
        if (extension !is HomeFeedClient) error("HomeFeedClient is not implemented")
        val tab = extension.getHomeTabs().firstOrNull()
        val feed = extension.getHomeFeed(tab).loadFirst()
        feed.forEach {
            println(it)
        }
    }

    @Test
    fun testNullQuickSearch() = testIn("Testing Null Quick Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val search = extension.quickSearch(null)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testQuickSearch() = testIn("Testing Quick Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val search = extension.quickSearch(searchQuery)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testNullSearch() = testIn("Testing Null Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val search = extension.searchFeed(null, null).loadFirst()
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testSearch() = testIn("Testing Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        println("Tabs")
        extension.searchTabs(searchQuery).forEach {
            println(it.name)
        }
        println("Search Results")
        val search = extension.searchFeed(searchQuery, null).loadFirst()
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testSearchWithTab() = testIn("Testing Search with Tab") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val tab = extension.searchTabs(searchQuery).firstOrNull()
        val search = extension.searchFeed(searchQuery, tab).loadFirst()
        search.forEach {
            println(it)
        }
    }


    private suspend fun searchTrack(q: String? = null): Track {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val query = q ?: searchQuery
        println("Searching  : $query")
        val items = extension.searchFeed(query, null).loadFirst()
        val track = items.firstNotNullOfOrNull {
            val item = when (it) {
                is MediaItemsContainer.Item -> it.media
                is MediaItemsContainer.Category -> it.list.firstOrNull()
                else -> null
            }
            (item as? EchoMediaItem.TrackItem)?.track
        }
        return track ?: error("Track not found, try a different search query")
    }

    @Test
    fun testTrackGet() = testIn("Testing Track Get") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val search = Track("qeFt3fdsydA", "")
        measureTimeMillis {
            val track = extension.loadTrack(search)
            println(track.liked)
        }.also { println("time : $it") }
    }

    @Test
    fun testTrackStream() = testIn("Testing Track Stream") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val search = Track("wimxNdDBII4", "")
        measureTimeMillis {
            val track = extension.loadTrack(search)
            val streamable = track.audioStreamables.firstOrNull()
                ?: error("Track is not streamable")
            val stream = extension.getStreamableAudio(streamable)
            println(stream)
        }.also { println("time : $it") }
    }

    @Test
    fun testTrackRadio() = testIn("Testing Track Radio") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        if (extension !is RadioClient) error("RadioClient is not implemented")
        val track = extension.loadTrack(searchTrack())
        val radio = extension.radio(track)
        val radioTracks = extension.loadTracks(radio).loadFirst()
        radioTracks.forEach {
            println(it)
        }
    }

    @Test
    fun testTrackMediaItems() = testIn("Testing Track Media Items") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val track = extension.loadTrack(Track("iDkSRTBDxJY", ""))
        val mediaItems = extension.getMediaItems(track).loadFirst()
        mediaItems.forEach {
            println(it)
        }
    }

    @Test
    fun testAlbumGet() = testIn("Testing Album Get") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
//        val small = extension.loadTrack(searchTrack()).album ?: error("Track has no album")
        val small = Album("OLAK5uy_mxh0nd0ZvSxiDVWflNGUSWxGz0ju1OA-E", "")
        if (extension !is AlbumClient) error("AlbumClient is not implemented")
        val album = extension.loadAlbum(small)
        println(album)
//        val mediaItems = extension.getMediaItems(album).loadFirst()
//        mediaItems.forEach {
//            println(it)
//        }
    }

    @Test
    fun testPlaylistMediaItems() = testIn("Testing Playlist Media Items") {
        if (extension !is PlaylistClient) error("PlaylistClient is not implemented")
        val playlist =
            extension.loadPlaylist(
                Playlist(
                    "PLSWY81108wBhKajj1EUuBr89ydg-DUjPy",
                    "",
                    false
                )
            )
        val tracks = extension.loadTracks(playlist).loadFirst()
        tracks.forEach {
            println(it)
        }
        val mediaItems = extension.getMediaItems(playlist).loadFirst()
        mediaItems.forEach {
            println(it)
        }
    }

    @Test
    fun testArtistMediaItems() = testIn("Testing Artist Media Items") {
        val small = Artist("UCySqAU8DY0BnB2j5uYdCbLA", "")
        if (extension !is ArtistClient) error("ArtistClient is not implemented")
        val artist = extension.loadArtist(small)
        println(artist)
        val mediaItems = extension.getMediaItems(artist).loadFirst()
        mediaItems.forEach {
            it as MediaItemsContainer.Category
            println("${it.title} : ${it.subtitle}")
            println("${it.list.size} : ${it.more}")
            it.list.forEach { item ->
                println("${item.title} : ${item.subtitle}")
            }
            if (it.more != null) {
                println("Loading More")
                it.more?.loadFirst()?.forEach { item ->
                    println("${item.title} : ${item.subtitle}")
                }
            }
        }
    }

    @Test
    fun testLyrics() = testIn("Lyrics") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val small = Track("iDkSRTBDxJY", "")
        val track = extension.loadTrack(small)
        println(track)
        if (extension !is LyricsClient) error("LyricsClient is not implemented")
        val lyricsItems = extension.searchTrackLyrics("", track).loadFirst()
        val lyricsItem = lyricsItems.firstOrNull()
        if (lyricsItem == null) {
            println("Lyrics not found")
            return@testIn
        }
        val loaded = extension.loadLyrics(lyricsItem)
        loaded.lyrics!!.forEach {
            println(it)
        }
    }
}