package com.artt.alchemy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artt.alchemy.game.ElementDefinition

@Composable
fun PrimitiveElement(element: ElementDefinition, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .semantics { contentDescription = element.name }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clip(CircleShape)
            .background(Color(element.color))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = element.name, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
