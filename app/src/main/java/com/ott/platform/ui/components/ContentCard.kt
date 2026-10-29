package com.ott.platform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ott.platform.ui.theme.OttGradientEnd
import com.ott.platform.ui.theme.OttGradientStart
import com.ott.platform.ui.theme.OttSurfaceVariant
import com.ott.platform.ui.theme.OttTextPrimary

/**
 * Content poster card used in horizontal rows and grids.
 * Displays poster image with title overlay at the bottom.
 */
@Composable
fun ContentCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 130.dp,
    aspectRatio: Float = 0.7f, // Poster ratio (2:3 approx)
    showTitle: Boolean = true,
) {
    val cleanTitle = androidx.compose.runtime.remember(title) {
        val regex = Regex("""\s+S\d+:E\d+.*$""", RegexOption.IGNORE_CASE)
        title.replace(regex, "").trim()
    }

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(OttSurfaceVariant)
            .clickable(onClick = onClick),
    ) {
        // Poster image
        AsyncImage(
            model = posterUrl,
            contentDescription = cleanTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Title gradient overlay with deep contrast
        if (showTitle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f)
                            ),
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 13.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = androidx.compose.ui.graphics.Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    ),
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Wide content card for "Continue Watching" or featured rows.
 */
@Composable
fun ContentCardWide(
    title: String,
    backdropUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
) {
    ContentCard(
        title = title,
        posterUrl = backdropUrl,
        onClick = onClick,
        modifier = modifier,
        width = width,
        aspectRatio = 16f / 9f,
        showTitle = true,
    )
}
