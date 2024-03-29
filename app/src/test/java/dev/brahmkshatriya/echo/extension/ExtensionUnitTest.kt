package dev.brahmkshatriya.echo.extension

import androidx.paging.AsyncPagingDataDiffer
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
class ExtensionUnitTest {
    private val extension: Any = YoutubeExtension()
    private val searchQuery = "Skrillex"

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun testIn(title: String, block: suspend () -> Unit) = runBlocking {
        println("\n-- $title --")
        block.invoke()
        println("\n")
    }

    @Test
    fun testMetadata() = testIn("Testing Extension Metadata") {
        if (extension !is ExtensionClient)
            error("ExtensionClient is not implemented")
        val metadata = extension.metadata
        println(metadata)
    }

    private val differ = AsyncPagingDataDiffer(
        MediaItemsContainerComparator(),
        ListCallback(),
    )

    @Test
    fun testHomeFeed() = testIn("Testing Home Feed") {
        if (extension !is HomeFeedClient)
            error("HomeFeedClient is not implemented")
        val genre = MutableStateFlow<Genre?>(null)

        extension.getHomeFeed(genre).collect {
            println("Received Data from source")
            differ.submitData(it)
        }
        differ.snapshot().items.forEach {
            println(it)
        }
    }

    @Test
    fun testHomeFeedWithGenre() = testIn("Testing Home Feed with Genre") {
        if (extension !is HomeFeedClient)
            error("HomeFeedClient is not implemented")
        val genre = MutableStateFlow(
            extension.getHomeGenres().firstOrNull()
        )
        val feed = extension.getHomeFeed(genre).first()
        differ.submitData(feed)
        differ.snapshot().items.forEach {
            println(it)
        }
    }

    @Test
    fun testEmptySearch() = testIn("Testing Quick Search") {
        if (extension !is SearchClient)
            error("SearchClient is not implemented")
        val search = extension.search(searchQuery).first()
        differ.submitData(search)
        differ.snapshot().items.forEach {
            println(it)
        }
    }

    @Test
    fun testSearch() = testIn("Testing Search") {
        if (extension !is SearchClient)
            error("SearchClient is not implemented")
        val search = extension.quickSearch(searchQuery)
        search.forEach {
            println(it)
        }
    }


    private suspend fun searchTrack(): Track {
        if (extension !is SearchClient)
            error("SearchClient is not implemented")
        val search = extension.search(searchQuery).first()
        val items = differ.snapshot().items
        differ.submitData(search)
        var track = items.filterIsInstance<MediaItemsContainer.TrackItem>()
            .firstOrNull()?.track
        if (track == null) {
            track = items.filterIsInstance<MediaItemsContainer.Category>().map {
                it.list.filterIsInstance<EchoMediaItem.TrackItem>()
            }.flatten().firstOrNull()?.track
        }
        return track ?: error("Track not found, try a different search query")
    }

    @Test
    fun testTrackGet() = testIn("Testing Track Get") {
        if (extension !is TrackClient)
            error("HomeFeedClient is not implemented")
        val track = extension.getTrack(searchTrack().id)
        println(track)
    }

}