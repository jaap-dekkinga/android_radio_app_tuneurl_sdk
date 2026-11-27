package com.tuneurlradio.app.ui.screens.article

import android.content.Intent
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tuneurlradio.app.data.ksal.AdPlacement
import com.tuneurlradio.app.data.ksal.Html
import com.tuneurlradio.app.domain.model.Station
import com.tuneurlradio.app.ui.components.ksal.AdSlot
import com.tuneurlradio.app.ui.components.ksal.formatArticleDate
import com.tuneurlradio.app.ui.components.ksal.isTablet
import com.tuneurlradio.app.ui.components.ksal.readableWidth
import com.tuneurlradio.app.ui.components.ksal.rememberOpenUrl

/** A ksal.com story shown natively: headline, byline, photo, body text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    playingStationId: Int?,
    onPlayStation: (Int) -> Unit,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
    viewModel: ArticleViewModel = hiltViewModel()
) {
    val article = viewModel.article
    val textScale by viewModel.textScale.collectAsState()
    val context = LocalContext.current
    val openUrl = rememberOpenUrl()
    val textWidth = 720.dp
    val tablet = isTablet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::cycleTextSize) {
                        Text("Aa", fontWeight = FontWeight.SemiBold)
                    }
                    val shareLink = article?.link
                    val shareTitle = article?.title
                    if (shareLink != null) {
                        IconButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                                putExtra(Intent.EXTRA_TEXT, shareLink)
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (article == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This story is no longer loaded. Go back and open it again.")
            }
            return@Scaffold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            val text = Modifier.readableWidth(textWidth).padding(horizontal = 20.dp)

            Column(text, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                article.categoryName?.let {
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(article.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    article.author?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    formatArticleDate(article.date, long = true)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            article.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth()
                        .height(if (tablet) 420.dp else 230.dp)
                        .clip(RoundedCornerShape(if (tablet) 16.dp else 0.dp))
                )
            }

            ArticleBodyText(article.contentHtml, textScale, text)

            Column(text, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AdSlot(AdPlacement.ARTICLE_BANNER)
                AdSlot(AdPlacement.ARTICLE_BOX)
            }

            viewModel.newsStation?.let { station ->
                ListenCard(
                    station = station,
                    isPlaying = playingStationId == station.id,
                    onClick = {
                        if (playingStationId == station.id) onExpandPlayer() else onPlayStation(station.id)
                    },
                    modifier = text
                )
            }

            article.link?.let { link ->
                Row(text) {
                    TextButton(onClick = { openUrl(link) }) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Open on ksal.com", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(72.dp)) // room for the mini player
        }
    }
}

@Composable
private fun ListenCard(station: Station, isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Hear more on ${station.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                if (isPlaying) "Playing now" else "Live now",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The story body. WordPress HTML is converted with HtmlCompat and shown in a
 * TextView so links work. Scripts, styles, iframes and images are removed first.
 */
@Composable
private fun ArticleBodyText(html: String, scale: Float, modifier: Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val spanned = remember(html) {
        var cleaned = html
        listOf(
            "<script[\\s\\S]*?</script>",
            "<style[\\s\\S]*?</style>",
            "<iframe[\\s\\S]*?</iframe>",
            "<img[^>]*>",
        ).forEach { cleaned = cleaned.replace(Regex(it, RegexOption.IGNORE_CASE), "") }
        runCatching { HtmlCompat.fromHtml(cleaned, HtmlCompat.FROM_HTML_MODE_LEGACY).trimEnd() }
            .getOrElse { Html.strip(html) }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextIsSelectable(false)
            }
        },
        update = { view ->
            view.text = spanned
            view.setTextColor(textColor)
            view.setLinkTextColor(linkColor)
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f * 1.08f * scale)
            view.setLineSpacing(5f * scale * view.resources.displayMetrics.density, 1f)
        }
    )
}
