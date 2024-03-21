package dev.brahmkshatriya.echo.extension

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.helpers.ErrorPagingSource

data class Page<T : Any, C : Any>(
    val data: List<T>,
    val continuation: C?
)

class ContinuationSource<T : Any, C : Any>(
    private val load: suspend (token: C?) -> Page<T, C>
) : ErrorPagingSource<C, T>() {
    override fun getRefreshKey(state: PagingState<C, T>): C? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }

    override suspend fun loadData(params: LoadParams<C>): LoadResult.Page<C, T> {
        val token = params.key
        val page = load(token)
        return LoadResult.Page(
            data = page.data,
            prevKey = null,
            nextKey = page.continuation
        )
    }
}

fun <T : Any> continuationFlow(load: suspend (token: String?) -> Page<T, String>) = Pager(
    config = PagingConfig(pageSize = 10, enablePlaceholders = false),
    pagingSourceFactory = { ContinuationSource(load) }
).flow
