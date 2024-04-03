package dev.brahmkshatriya.echo.extension

import androidx.paging.PagingConfig
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.helpers.ErrorPagingSource
import dev.brahmkshatriya.echo.common.helpers.PagedData

data class Page<T : Any>(
    val data: List<T>,
    val continuation: String?
)

class ContinuationSource<C : Any>(
    private val load: suspend (token: String?) -> Page<C>,
) : ErrorPagingSource<String, C>() {

    override val config = PagingConfig(pageSize = 10, enablePlaceholders = false)
    override suspend fun loadData(params: LoadParams<String>): LoadResult.Page<String, C> {
        val token = params.key
        val page = load(token)
        return LoadResult.Page(
            data = page.data,
            prevKey = null,
            nextKey = page.continuation
        )
    }

    override fun getRefreshKey(state: PagingState<String, C>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}

fun <T : Any> continuationFlow(load: suspend (token: String?) ->Page<T>) =
    PagedData.Source(ContinuationSource(load))
