package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.User
import kotlinx.serialization.Serializable

@Serializable
data class GoogleAccountResponse(
    val code: String,
    val data: Data
) {

    private fun getAccountList(): List<Pair<String?, AccountItem>> {
        return data.actions[0].getMultiPageMenuAction.menu.multiPageMenuRenderer.sections[0].accountSectionListRenderer.run {
            contents.map {
                it.accountItemSectionRenderer.contents.mapNotNull { content ->
                    content.accountItem
                }
            }.flatten().map {
                header.googleAccountHeaderRenderer?.email?.simpleText to it
            }
        }
    }

    fun getUsers(cookie: String, auth: String): List<User> {
        return getAccountList().mapNotNull {
            val (email, item) = it
            if (item.isDisabled == true) return@mapNotNull null
            val cover =
                item.accountPhoto?.thumbnails?.firstOrNull()?.url?.toImageHolder()
            val signInUrl =
                item.serviceEndpoint?.selectActiveIdentityEndpoint?.supportedTokens
                    ?.find { token -> token.accountSigninToken != null }
                    ?.accountSigninToken?.signinUrl ?: return@mapNotNull null
            val channelId = item.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens
                .find { token -> token.offlineCacheKeyToken != null }
                ?.offlineCacheKeyToken?.clientCacheKey

            User(
                if (channelId != null) "UC$channelId" else "",
                item.accountName.simpleText,
                cover,
                email,
                mapOf("auth" to auth, "cookie" to cookie, "signInUrl" to signInUrl)
            )
        }
    }

    @Serializable
    data class Data(
        val actions: List<Action>
    )

    @Serializable
    data class Action(
        val getMultiPageMenuAction: GetMultiPageMenuAction
    )

    @Serializable
    data class GetMultiPageMenuAction(
        val menu: Menu
    )

    @Serializable
    data class Menu(
        val multiPageMenuRenderer: MultiPageMenuRenderer
    )

    @Serializable
    data class MultiPageMenuRenderer(
        val header: MultiPageMenuRendererHeader? = null,
        val sections: List<Section>,
        val footer: Footer? = null,
        val style: String? = null
    )

    @Serializable
    data class Footer(
        val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer? = null
    )

    @Serializable
    data class MultiPageMenuSectionRenderer(
        val items: List<Item>? = null
    )

    @Serializable
    data class Item(
        val compactLinkRenderer: ItemCompactLinkRenderer? = null
    )

    @Serializable
    data class ItemCompactLinkRenderer(
        val icon: Icon? = null,
        val title: Title? = null,
        val navigationEndpoint: PurpleNavigationEndpoint? = null,
        val style: String? = null
    )

    @Serializable
    data class Icon(
        val iconType: String? = null
    )

    @Serializable
    data class PurpleNavigationEndpoint(
        val commandMetadata: CommandMetadata? = null,
        val urlEndpoint: URLEndpoint? = null,
        val signOutEndpoint: SignOutEndpoint? = null
    )

    @Serializable
    data class CommandMetadata(
        val webCommandMetadata: WebCommandMetadata? = null
    )

    @Serializable
    data class WebCommandMetadata(
        val url: String? = null,
        val webPageType: String? = null,
        val rootVe: Long? = null
    )

    @Serializable
    data class SignOutEndpoint(
        val hack: Boolean? = null
    )

    @Serializable
    data class URLEndpoint(
        val url: String? = null
    )

    @Serializable
    data class Title(
        val simpleText: String
    )

    @Serializable
    data class MultiPageMenuRendererHeader(
        val simpleMenuHeaderRenderer: SimpleMenuHeaderRenderer? = null
    )

    @Serializable
    data class SimpleMenuHeaderRenderer(
        val backButton: BackButton? = null,
        val title: Title? = null
    )

    @Serializable
    data class BackButton(
        val buttonRenderer: ButtonRenderer? = null
    )

    @Serializable
    data class ButtonRenderer(
        val style: String? = null,
        val size: String? = null,
        val isDisabled: Boolean? = null,
        val icon: Icon? = null,
        val accessibility: Accessibility? = null,
        val accessibilityData: AccessibilityData? = null
    )

    @Serializable
    data class Accessibility(
        val label: String? = null
    )

    @Serializable
    data class AccessibilityData(
        val accessibilityData: Accessibility? = null
    )

    @Serializable
    data class Section(
        val accountSectionListRenderer: AccountSectionListRenderer
    )

    @Serializable
    data class AccountSectionListRenderer(
        val contents: List<AccountSectionListRendererContent>,
        val header: AccountSectionListRendererHeader
    )

    @Serializable
    data class AccountSectionListRendererContent(
        val accountItemSectionRenderer: AccountItemSectionRenderer
    )

    @Serializable
    data class AccountItemSectionRenderer(
        val contents: List<AccountItemSectionRendererContent>
    )

    @Serializable
    data class AccountItemSectionRendererContent(
        val accountItem: AccountItem? = null,
        val compactLinkRenderer: ContentCompactLinkRenderer? = null
    )

    @Serializable
    data class AccountItem(
        val accountName: Title,
        val accountPhoto: AccountPhoto? = null,
        val isSelected: Boolean? = null,
        val isDisabled: Boolean? = null,
        val hasChannel: Boolean? = null,
        val serviceEndpoint: ServiceEndpoint? = null,
        val accountByline: Title? = null,
        val unlimitedStatus: List<Title>? = null,
        val channelHandle: Title? = null
    )

    @Serializable
    data class AccountPhoto(
        val thumbnails: List<Thumbnail>? = null
    )

    @Serializable
    data class Thumbnail(
        val url: String? = null,
        val width: Long? = null,
        val height: Long? = null
    )

    @Serializable
    data class ServiceEndpoint(
        val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint? = null
    )

    @Serializable
    data class SelectActiveIdentityEndpoint(
        val supportedTokens: List<SupportedToken>? = null,
        val nextNavigationEndpoint: NextNavigationEndpoint? = null
    )

    @Serializable
    data class NextNavigationEndpoint(
        val commandMetadata: CommandMetadata? = null,
        val urlEndpoint: URLEndpoint? = null
    )

    @Serializable
    data class SupportedToken(
        val accountStateToken: AccountStateToken? = null,
        val offlineCacheKeyToken: OfflineCacheKeyToken? = null,
        val accountSigninToken: AccountSigninToken? = null,
        val datasyncIdToken: DatasyncIdToken? = null
    )

    @Serializable
    data class AccountSigninToken(
        val signinUrl: String? = null
    )

    @Serializable
    data class AccountStateToken(
        val hasChannel: Boolean? = null,
        val isMerged: Boolean? = null,
        val obfuscatedGaiaId: String? = null
    )

    @Serializable
    data class DatasyncIdToken(
        val datasyncIdToken: String? = null
    )

    @Serializable
    data class OfflineCacheKeyToken(
        val clientCacheKey: String? = null
    )

    @Serializable
    data class ContentCompactLinkRenderer(
        val title: Title? = null,
        val navigationEndpoint: FluffyNavigationEndpoint? = null
    )

    @Serializable
    data class FluffyNavigationEndpoint(
        val commandMetadata: CommandMetadata? = null,
        val signalNavigationEndpoint: SignalNavigationEndpoint? = null
    )

    @Serializable
    data class SignalNavigationEndpoint(
        val signal: String? = null
    )

    @Serializable
    data class AccountSectionListRendererHeader(
        val googleAccountHeaderRenderer: GoogleAccountHeaderRenderer? = null
    )

    @Serializable
    data class GoogleAccountHeaderRenderer(
        val name: Title? = null,
        val email: Title? = null
    )
}