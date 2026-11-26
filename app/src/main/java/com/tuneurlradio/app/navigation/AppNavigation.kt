package com.tuneurlradio.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tuneurlradio.app.ui.main.MainIntent
import com.tuneurlradio.app.ui.main.MainState
import com.tuneurlradio.app.ui.screens.article.ArticleScreen
import com.tuneurlradio.app.ui.screens.engagement.EngagementDetailScreen
import com.tuneurlradio.app.ui.screens.home.HomeScreen
import com.tuneurlradio.app.ui.screens.listen.ListenScreen
import com.tuneurlradio.app.ui.screens.local.LocalScreen
import com.tuneurlradio.app.ui.screens.settings.ParsingSettingsScreen
import com.tuneurlradio.app.ui.screens.settings.SettingsScreen
import com.tuneurlradio.app.ui.screens.sports.SportsScreen
import com.tuneurlradio.app.ui.screens.turlstab.TurlsTabScreen

object Routes {
    const val SETTINGS = "settings"
    const val PARSING_SETTINGS = "parsing_settings"

    /** Route template: `article/{articleId}`. */
    const val ARTICLE_TEMPLATE = "article/{${NavArgs.ARTICLE_ID}}"
    fun article(id: Int): String = "article/$id"

    /** Route template: `engagement_detail/{source}/{localId}`. */
    const val ENGAGEMENT_DETAIL_TEMPLATE =
        "engagement_detail/{${NavArgs.SOURCE}}/{${NavArgs.LOCAL_ID}}"

    /** Build a concrete navigation URL for the detail screen. */
    fun engagementDetail(source: EngagementSource, localId: Long): String =
        "engagement_detail/${source.route}/$localId"
}

/** Switch to a bottom tab the same way the tab bar does. */
fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    mainState: MainState,
    onMainIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val openArticle: (Int) -> Unit = { navController.navigate(Routes.article(it)) }
    val openSettings: () -> Unit = { navController.navigate(Routes.SETTINGS) }

    // The app always opens on Home; the last tab is not restored.
    NavHost(
        navController = navController,
        startDestination = AppTab.HOME.route,
        modifier = modifier
    ) {
        composable(AppTab.HOME.route) {
            HomeScreen(
                onOpenArticle = openArticle,
                onOpenLocal = { navController.navigateToTab(AppTab.LOCAL) },
                onOpenSettings = openSettings
            )
        }
        composable(AppTab.SPORTS.route) {
            SportsScreen(onOpenArticle = openArticle)
        }
        composable(AppTab.LOCAL.route) {
            LocalScreen()
        }
        composable(AppTab.LISTEN.route) {
            ListenScreen(state = mainState, onIntent = onMainIntent)
        }
        composable(AppTab.TURLS.route) {
            TurlsTabScreen(
                onOpenSaved = { navController.navigate(Routes.engagementDetail(EngagementSource.SAVED, it)) },
                onOpenHeard = { navController.navigate(Routes.engagementDetail(EngagementSource.HISTORY, it)) },
                onOpenSettings = openSettings
            )
        }
        composable(
            route = Routes.ARTICLE_TEMPLATE,
            arguments = listOf(navArgument(NavArgs.ARTICLE_ID) { type = NavType.IntType })
        ) {
            ArticleScreen(
                playingStationId = mainState.currentStation?.id?.takeIf { mainState.isPlaying },
                onPlayStation = { onMainIntent(MainIntent.SelectStation(it)) },
                onExpandPlayer = { onMainIntent(MainIntent.ExpandPlayer) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToParsingSettings = { navController.navigate(Routes.PARSING_SETTINGS) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PARSING_SETTINGS) {
            ParsingSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.ENGAGEMENT_DETAIL_TEMPLATE,
            arguments = listOf(
                navArgument(NavArgs.SOURCE) { type = NavType.StringType },
                navArgument(NavArgs.LOCAL_ID) { type = NavType.LongType }
            )
        ) {
            EngagementDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
