package com.tuneurlradio.app.ui.components.ksal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneurlradio.app.R
import com.tuneurlradio.app.data.ksal.AdCreative
import com.tuneurlradio.app.data.ksal.AdPlacement
import com.tuneurlradio.app.data.ksal.AdServiceEntryPoint
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.data.ksal.NewsArticle
import com.tuneurlradio.app.data.ksal.WeatherRepository
import dagger.hilt.android.EntryPointAccessors
import java.text.DateFormat
import java.util.Date

// ---------------------------------------------------------------------------
// Layout helpers
// ---------------------------------------------------------------------------

/** True on tablets (the iPad "regular width" layout). */
@Composable
fun isTablet(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

/** Content column at most [max] wide, centred (iOS readableWidth: 860, article text 720). */
fun Modifier.readableWidth(max: Dp = 860.dp): Modifier = this.widthIn(max = max).fillMaxWidth()

// ---------------------------------------------------------------------------
// In-app browser (Custom Tabs)
// ---------------------------------------------------------------------------

fun openInAppBrowser(context: Context, url: String, toolbarColor: Int? = null) {
    try {
        val builder = CustomTabsIntent.Builder().setShowTitle(true)
        toolbarColor?.let {
            builder.setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().setToolbarColor(it).build()
            )
        }
        builder.build().launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // No Custom Tabs browser installed: fall back to any browser.
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { Log.w("InAppBrowser", "Could not open $url", it) }
    }
}

/** Returns a function that opens a URL in Custom Tabs, tinted with the app colours. */
@Composable
fun rememberOpenUrl(): (String) -> Unit {
    val context = LocalContext.current
    val surface = MaterialTheme.colorScheme.surface.toArgb()
    return remember(context, surface) { { url: String -> openInAppBrowser(context, url, surface) } }
}

// ---------------------------------------------------------------------------
// Settings gear (Home and Turls)
// ---------------------------------------------------------------------------

@Composable
fun SettingsGear(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
    }
}

// ---------------------------------------------------------------------------
// Headers
// ---------------------------------------------------------------------------

/**
 * "KSAL News" with the station logo. The logo is looked up at runtime as
 * drawable `ksal_logo` so branding can stay in a local, uncommitted file;
 * without it only the text shows.
 */
@Composable
fun Wordmark() {
    val context = LocalContext.current
    val logoId = remember {
        context.resources.getIdentifier("ksal_logo", "drawable", context.packageName)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { heading(); contentDescription = "KSAL News" }
    ) {
        if (logoId != 0) {
            Image(
                painter = androidx.compose.ui.res.painterResource(logoId),
                contentDescription = null,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text("KSAL News", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
fun SmallCapsHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() }
    )
}

// ---------------------------------------------------------------------------
// News
// ---------------------------------------------------------------------------

fun formatArticleDate(date: Date?, long: Boolean = false): String? = date?.let {
    if (long) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(it)
    else DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)
}

@Composable
fun CategoryChips(
    categories: List<Ksal.Category>,
    selected: Ksal.Category,
    onSelect: (Ksal.Category) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.title) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun HeroArticleCard(article: NewsArticle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        if (article.imageUrl != null) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            article.categoryName?.let {
                Text(
                    it.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(article.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (article.excerpt.isNotEmpty()) {
                Text(
                    article.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ArticleByline(article)
        }
    }
}

@Composable
fun ArticleRow(article: NewsArticle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                article.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            ArticleByline(article)
        }
        val thumb = article.thumbnailUrl ?: article.imageUrl
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun ArticleByline(article: NewsArticle) {
    val parts = listOfNotNull(article.author, formatArticleDate(article.date))
    if (parts.isNotEmpty()) {
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A list of rows: one column on phones, two on tablets. */
@Composable
fun ArticleList(articles: List<NewsArticle>, onOpen: (NewsArticle) -> Unit, modifier: Modifier = Modifier) {
    val columns = if (isTablet()) 2 else 1
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        articles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { ArticleRow(it, onClick = { onOpen(it) }, modifier = Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun FeedFooter(isLoading: Boolean, errorMessage: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.size(28.dp))
            errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Could not load stories", style = MaterialTheme.typography.titleSmall)
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Weather strip (Home)
// ---------------------------------------------------------------------------

@Composable
fun WeatherStrip(state: WeatherRepository.State, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(Icons.Outlined.WbCloudy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val now = state.conditions
            Text(
                if (now != null) "Salina · ${now.temperature}°${now.unit} ${now.summary}" else "Salina weather",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Forecast, radar, roads and closings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Link rows and cards
// ---------------------------------------------------------------------------

@Composable
fun LinkRow(link: Ksal.Link, detail: String? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(link.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(link.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Ads
// ---------------------------------------------------------------------------

/**
 * A labelled sponsor slot. Takes no space when the zone has no ad.
 *
 * The outer Box always exists, so the request starts even before an ad has
 * loaded (iOS PR #45: an empty slot view never ran its load task).
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val service = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, AdServiceEntryPoint::class.java).adService()
    }
    val zone = placement.zone(isTablet())
    val openUrl = rememberOpenUrl()
    var creative by remember(zone) { mutableStateOf<AdCreative?>(null) }
    var countedImpression by remember(zone) { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        LaunchedEffect(zone) {
            creative = service.fetch(zone, placement)
        }
        creative?.let { ad ->
            LaunchedEffect(ad) {
                if (!countedImpression && ad.beaconUrl != null) {
                    countedImpression = true
                    service.sendBeacon(ad.beaconUrl)
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Advertisement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AsyncImage(
                    model = ad.imageUrl,
                    contentDescription = if (ad.altText.isEmpty()) "Advertisement" else "Advertisement: ${ad.altText}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = ad.width.dp)
                        .fillMaxWidth()
                        .aspectRatio(ad.width / ad.height)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClickLabel = "Open the advertiser's website") { openUrl(ad.clickUrl) }
                )
            }
        }
    }
}

/** Station logo fallback for rows that show a station. */
val stationLogoFallback = R.drawable.station_logo
