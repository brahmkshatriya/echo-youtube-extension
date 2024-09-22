package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.models.QuickSearch
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

open class EchoSearchSuggestionsEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    private val feedbackTokens = mutableMapOf<String, String>()
    suspend fun get(
        query: String
    ): Result<List<QuickSearch>> = runCatching {
        val response: HttpResponse = api.client.request {
            endpointPath("music/get_search_suggestions")
            addApiHeadersWithAuthenticated()
            postWithBody {
                put("input", query)
            }
        }

        val parsed: YoutubeiSearchSuggestionsResponse = response.body()

        val suggestions = parsed.getSuggestions(feedbackTokens)
            ?: throw NullPointerException("Suggestions is null ($parsed)")

        return@runCatching suggestions
    }

    suspend fun delete(query: String) {
        val feedbackToken = feedbackTokens[query] ?: return
        runCatching {
            api.client.request {
                endpointPath("feedback")
                addApiHeadersWithAuthenticated()
                postWithBody {
                    put("feedbackTokens", buildJsonArray { add(feedbackToken) })
                    put("isFeedbackTokenUnencrypted", false)
                    put("shouldMerge", false)
                }
            }
        }
    }
}

@Serializable
private data class YoutubeiSearchSuggestionsResponse(
    val contents: List<Content>?
) {
    fun getSuggestions(feedbackTokens: MutableMap<String, String>) = contents?.firstOrNull()
        ?.searchSuggestionsSectionRenderer
        ?.contents
        ?.mapNotNull { suggestion ->
            if (suggestion.searchSuggestionRenderer != null) {
                return@mapNotNull QuickSearch.QueryItem(
                    suggestion.searchSuggestionRenderer.navigationEndpoint.searchEndpoint.query,
                    false
                )
            } else if (suggestion.historySuggestionRenderer != null) {
                return@mapNotNull QuickSearch.QueryItem(
                    suggestion.historySuggestionRenderer.navigationEndpoint.searchEndpoint.query,
                    true
                ).also { item ->
                    suggestion.historySuggestionRenderer.serviceEndpoint
                        ?.feedbackEndpoint?.feedbackToken?.let {
                            feedbackTokens[item.query] = it
                        }
                }
            }
            return@mapNotNull null
        }

    @Serializable
    data class Content(val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer?)

    @Serializable
    data class SearchSuggestionsSectionRenderer(val contents: List<Suggestion>)

    @Serializable
    data class Suggestion(
        val searchSuggestionRenderer: SearchSuggestionRenderer?,
        val historySuggestionRenderer: SearchSuggestionRenderer?
    )

    @Serializable
    data class SearchSuggestionRenderer(
        val navigationEndpoint: NavigationEndpoint,
        val serviceEndpoint: ServiceEndpoint? = null
    )

    @Serializable
    data class ServiceEndpoint(val feedbackEndpoint: FeedbackEndpoint)

    @Serializable
    data class FeedbackEndpoint(val feedbackToken: String)

    @Serializable
    data class NavigationEndpoint(val searchEndpoint: SearchEndpoint)

    @Serializable
    data class SearchEndpoint(val query: String)
}
