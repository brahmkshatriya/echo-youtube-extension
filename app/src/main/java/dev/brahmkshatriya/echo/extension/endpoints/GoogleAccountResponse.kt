package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import kotlinx.serialization.Serializable

@Serializable
data class GoogleAccountResponse(
    val code: String,
    val data: Data
) {

    private fun getAccountList(): List<AccountItem> {
        return data.actions[0].getMultiPageMenuAction.menu.multiPageMenuRenderer.sections[0].accountSectionListRenderer.contents.map {
            it.accountItemSectionRenderer.contents.mapNotNull { content ->
                content.accountItem
            }
        }.flatten()
    }

    fun getArtists(cookie: String, auth: String): List<Artist> {
        return getAccountList().mapNotNull {
            if (it.isDisabled) return@mapNotNull null
            val cover =
                it.accountPhoto.thumbnails.firstOrNull()?.url?.toImageHolder()
            val signInUrl =
                it.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens
                    .find { token -> token.accountSigninToken != null }
                    ?.accountSigninToken?.signinUrl ?: return@mapNotNull null
            val channelId = it.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens
                .find { token -> token.offlineCacheKeyToken != null }
                ?.offlineCacheKeyToken?.clientCacheKey
            Artist(
                if (channelId != null) "UC$channelId" else "",
                it.accountName.simpleText,
                cover,
                mapOf("auth" to auth, "cookie" to cookie, "signInUrl" to signInUrl),
                it.accountByline.simpleText
            )
        }
    }


    @Serializable
    data class Data(
        val responseContext: ResponseContext,
        val selectText: SelectText,
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
        val header: MultiPageMenuRendererHeader,
        val sections: List<Section>,
        val footer: Footer,
        val style: String
    )

    @Serializable
    data class Footer(
        val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer
    )

    @Serializable
    data class MultiPageMenuSectionRenderer(
        val items: List<Item>
    )

    @Serializable
    data class Item(
        val compactLinkRenderer: ItemCompactLinkRenderer
    )

    @Serializable
    data class ItemCompactLinkRenderer(
        val icon: Icon,
        val title: SelectText,
        val navigationEndpoint: PurpleNavigationEndpoint,
        val style: String
    )

    @Serializable
    data class Icon(
        val iconType: String
    )

    @Serializable
    data class PurpleNavigationEndpoint(
        val commandMetadata: CommandMetadata,
        val urlEndpoint: URLEndpoint? = null,
        val signOutEndpoint: SignOutEndpoint? = null
    )

    @Serializable
    data class CommandMetadata(
        val webCommandMetadata: WebCommandMetadata
    )

    @Serializable
    data class WebCommandMetadata(
        val url: String,
        val webPageType: String,
        val rootVe: Long
    )

    @Serializable
    data class SignOutEndpoint(
        val hack: Boolean
    )

    @Serializable
    data class URLEndpoint(
        val url: String
    )

    @Serializable
    data class SelectText(
        val simpleText: String
    )

    @Serializable
    data class MultiPageMenuRendererHeader(
        val simpleMenuHeaderRenderer: SimpleMenuHeaderRenderer
    )

    @Serializable
    data class SimpleMenuHeaderRenderer(
        val backButton: BackButton,
        val title: SelectText
    )

    @Serializable
    data class BackButton(
        val buttonRenderer: ButtonRenderer
    )

    @Serializable
    data class ButtonRenderer(
        val style: String,
        val size: String,
        val isDisabled: Boolean,
        val icon: Icon,
        val accessibility: Accessibility,
        val accessibilityData: AccessibilityData
    )

    @Serializable
    data class Accessibility(
        val label: String
    )

    @Serializable
    data class AccessibilityData(
        val accessibilityData: Accessibility
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
        val accountName: SelectText,
        val accountPhoto: AccountPhoto,
        val isSelected: Boolean,
        val isDisabled: Boolean,
        val mobileBanner: AccountPhoto? = null,
        val hasChannel: Boolean? = null,
        val serviceEndpoint: ServiceEndpoint,
        val accountByline: SelectText,
        val unlimitedStatus: List<SelectText>? = null,
        val channelHandle: SelectText,
        val channelDelegationRole: String? = null
    )

    @Serializable
    data class AccountPhoto(
        val thumbnails: List<Thumbnail>
    )

    @Serializable
    data class Thumbnail(
        val url: String,
        val width: Long,
        val height: Long
    )

    @Serializable
    data class ServiceEndpoint(
        val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint
    )

    @Serializable
    data class SelectActiveIdentityEndpoint(
        val supportedTokens: List<SupportedToken>,
        val nextNavigationEndpoint: NextNavigationEndpoint? = null
    )

    @Serializable
    data class NextNavigationEndpoint(
        val commandMetadata: CommandMetadata,
        val urlEndpoint: URLEndpoint
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
        val signinUrl: String
    )

    @Serializable
    data class AccountStateToken(
        val hasChannel: Boolean,
        val isMerged: Boolean,
        val obfuscatedGaiaId: String
    )

    @Serializable
    data class DatasyncIdToken(
        val datasyncIdToken: String
    )

    @Serializable
    data class OfflineCacheKeyToken(
        val clientCacheKey: String
    )

    @Serializable
    data class ContentCompactLinkRenderer(
        val title: SelectText,
        val navigationEndpoint: FluffyNavigationEndpoint
    )

    @Serializable
    data class FluffyNavigationEndpoint(
        val commandMetadata: CommandMetadata,
        val signalNavigationEndpoint: SignalNavigationEndpoint
    )

    @Serializable
    data class SignalNavigationEndpoint(
        val signal: String
    )

    @Serializable
    data class AccountSectionListRendererHeader(
        val googleAccountHeaderRenderer: GoogleAccountHeaderRenderer
    )

    @Serializable
    data class GoogleAccountHeaderRenderer(
        val name: SelectText,
        val email: SelectText
    )

    @Serializable
    data class ResponseContext(
        val serviceTrackingParams: List<ServiceTrackingParam>,
        val mainAppWebResponseContext: MainAppWebResponseContext,
        val webResponseContextExtensionData: WebResponseContextExtensionData
    )

    @Serializable
    data class MainAppWebResponseContext(
        val datasyncId: String,
        val loggedOut: Boolean,
        val trackingParam: String
    )

    @Serializable
    data class ServiceTrackingParam(
        val service: String,
        val params: List<Param>
    )

    @Serializable
    data class Param(
        val key: String,
        val value: String
    )

    @Serializable
    data class WebResponseContextExtensionData(
        val hasDecorated: Boolean
    )
}