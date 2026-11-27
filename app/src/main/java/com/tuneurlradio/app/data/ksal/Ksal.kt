package com.tuneurlradio.app.data.ksal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Fixed KSAL content: WordPress categories, community links and team sites.
 * Copied from iOS TuneURLRadio/Flow/News/KSAL.swift (ksal.com menus read 2026-11-18).
 */
object Ksal {

    const val SITE = "https://www.ksal.com"
    const val POSTS_API = "https://www.ksal.com/wp-json/wp/v2/posts"

    /**
     * The station that "Hear more" in an article plays (id in assets/stations.json).
     * iOS uses id 3 in its local stations.json; in this repo's stations.json
     * KSAL Newsradio 1150 is id 1.
     */
    const val NEWS_STATION_ID = 1

    // --- News categories (WordPress category ids) ---

    data class Category(val id: String, val title: String, val wordpressId: Int?)

    val topStories = Category("top", "Top Stories", null)
    val kansasNews = Category("kansas", "Kansas", 9)
    val sportsNews = Category("sports", "Sports", 15100)
    val farmingNews = Category("farm", "Farm", 20443)
    val commentary = Category("commentary", "Commentary", 15974)

    val homeCategories = listOf(topStories, kansasNews, sportsNews, farmingNews, commentary)

    // --- Links (open in Custom Tabs) ---

    data class Link(val id: String, val title: String, val icon: ImageVector, val url: String)

    val forecast = Link("forecast", "Forecast", Icons.Outlined.CalendarMonth, "https://www.ksal.com/forecast/")
    val radar = Link("radar", "Radar", Icons.Outlined.Radar, "https://radar.weather.gov/radar.php?product=NCR&rid=ICT&loop=yes")
    val roads = Link("roads", "Roads", Icons.Outlined.Traffic, "https://kandrive.gov/@-99.86957,38.77164,7?show=roadReports,constructionReports,trafficSpeeds,winterDriving,weatherRadar")
    val closings = Link("closings", "Closings & Delays", Icons.Outlined.WarningAmber, "https://www.ksal.com/closings/")

    val communityLinks = listOf(
        Link("obituaries", "Obituaries", Icons.Outlined.Favorite, "https://www.ksal.com/obituaries/"),
        Link("auctions", "Auctions", Icons.Outlined.Gavel, "https://www.ksal.com/auctions/"),
        Link("calendar", "Calendar", Icons.Outlined.CalendarMonth, "https://www.ksal.com/calendar/"),
        Link("jobs", "Jobs", Icons.Outlined.Work, "https://kansasjobslink.com/"),
        Link("bookings", "Bookings", Icons.Outlined.Security, "https://www.ksal.com/sheriff-bookings/"),
        Link("inspections", "Inspections", Icons.Outlined.Restaurant, "https://www.ksal.com/restaurant-inspections/"),
        Link("graduations", "Graduations", Icons.Outlined.School, "https://www.ksal.com/graduation-announcements/"),
        Link("education", "Education", Icons.AutoMirrored.Outlined.MenuBook, "https://www.ksal.com/education/"),
        Link("wanted", "Most Wanted", Icons.Outlined.Badge, "https://www.ksal.com/most-wanted/"),
        Link("lineup", "KSAL Lineup", Icons.AutoMirrored.Outlined.ListAlt, "https://www.ksal.com/ksal-lineup/"),
        Link("archives", "News Archive", Icons.Outlined.Archive, "https://www.ksal.com/news/"),
        Link("farming", "Ag News", Icons.Outlined.Spa, "https://www.ksal.com/farming-news/"),
    )

    val scoreboard = Link("scoreboard", "Scoreboard & Schedules", Icons.Outlined.SportsBasketball, "https://www.ksal.com/broadcast-schedule-results/")
    val sportsAudio = Link("sportsAudio", "Sports Audio", Icons.Outlined.GraphicEq, "https://www.ksal.com/sports-audio/")
    val pastShows = Link("pastShows", "Past Shows", Icons.Outlined.LibraryMusic, "https://soundcloud.com/oddittenger")
    val lineup = Link("lineupAudio", "KSAL Lineup", Icons.AutoMirrored.Outlined.ListAlt, "https://www.ksal.com/ksal-lineup/")

    data class Team(val id: String, val short: String, val name: String, val url: String)

    val teams = listOf(
        Team("kstate", "KSU", "Wildcats", "https://www.kstatesports.com/"),
        Team("ku", "KU", "Jayhawks", "https://kuathletics.com/"),
        Team("chiefs", "KC", "Chiefs", "https://www.chiefs.com/"),
        Team("royals", "KCR", "Royals", "https://www.mlb.com/royals"),
        Team("shockers", "WSU", "Shockers", "https://goshockers.com/"),
    )

    // Salina, for the NWS forecast
    const val LATITUDE = 38.8403
    const val LONGITUDE = -97.6114
}
