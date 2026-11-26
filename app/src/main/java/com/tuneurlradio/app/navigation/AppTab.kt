package com.tuneurlradio.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.ui.graphics.vector.ImageVector

/** KSAL station app tabs (iOS PRs #42-#46). Settings opens from a gear on Home and Turls. */
enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "home"),
    SPORTS("Sports", Icons.Filled.SportsBasketball, Icons.Outlined.SportsBasketball, "sports"),
    LOCAL("Local", Icons.Filled.Place, Icons.Outlined.Place, "local"),
    LISTEN("Listen", Icons.Filled.Radio, Icons.Outlined.Radio, "listen"),
    TURLS("Turls", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "turls")
}
