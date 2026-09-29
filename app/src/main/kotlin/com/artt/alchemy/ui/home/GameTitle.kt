package com.artt.alchemy.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.ui.theme.TitleFontFamily

private val TitleGradient = Brush.verticalGradient(listOf(Color(0xFFFFEFB8), Color(0xFFF3C35B), Color(0xFFD08A26)))
private val TitleShadow = Shadow(color = Color(0xFF1A0E3D), offset = Offset(0f, 3f), blurRadius = 8f)
private val LOGO_SIZE = 64.dp
private val TITLE_SIZE = 38.sp
private val TAGLINE_SIZE = 13.sp

/** The game's name as on the concept: a golden serif wordmark beside the spell book, with a tagline under it. */
@Composable
fun GameTitle(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(LOGO_SIZE))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = TitleGradient,
                    shadow = TitleShadow,
                    fontSize = TITLE_SIZE,
                    lineHeight = TITLE_SIZE
                ),
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = TAGLINE_SIZE, fontFamily = TitleFontFamily, fontWeight = FontWeight.SemiBold, shadow = TitleShadow),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
