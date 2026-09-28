package com.artt.alchemy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artt.alchemy.game.ElementDefinition

private val SilhouetteFilter = ColorFilter.tint(Color(0xFF2B2B33))

@Composable
fun ElementIcon(element: ElementDefinition, modifier: Modifier = Modifier, silhouette: Boolean = false) {
    Image(
        painter = painterResource(elementIconRes(element.id)),
        contentDescription = null,
        colorFilter = if (silhouette) SilhouetteFilter else null,
        modifier = modifier.aspectRatio(1f)
    )
}

@Composable
fun ElementTile(element: ElementDefinition, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .semantics { contentDescription = element.name }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        ElementIcon(element, Modifier.fillMaxWidth())
        Text(
            text = element.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
