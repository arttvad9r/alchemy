package com.artt.alchemy.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.components.PrimitiveElement

@Composable
fun HomeScreen(state: AlchemyUiState, onEvent: (WorkspaceEvent) -> Unit, modifier: Modifier = Modifier) {
    val unlocked = AlchemyCatalog.elements.filter { it.id in state.progress.unlockedIds }
    var workspaceBounds by remember { mutableStateOf<Rect?>(null) }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.progress, state.progress.unlockedIds.size, AlchemyCatalog.elements.size))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.workspace_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onEvent(WorkspaceEvent.Clear) }, modifier = Modifier.testTag("clear_workspace")) {
                Text(stringResource(R.string.clear_workspace))
            }
        }
        OutlinedCard(
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .testTag("home_workspace")
        ) {
            WorkspaceCanvas(
                items = state.workspace.items,
                onMove = { id, position -> onEvent(WorkspaceEvent.Move(id, position.x, position.y)) },
                onResolve = { id, position -> onEvent(WorkspaceEvent.ResolveOverlap(id, position.x, position.y)) },
                onBoundsChanged = { workspaceBounds = it },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.palette_title), style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            unlocked.forEach { element ->
                DraggablePaletteElement(
                    element = element,
                    modifier = Modifier.width(72.dp).testTag("palette_${element.id}"),
                    onDrop = { drop ->
                        workspaceBounds
                            ?.takeIf { it.contains(drop) }
                            ?.let { bounds ->
                                onEvent(
                                    WorkspaceEvent.Spawn(
                                        element.id,
                                        (drop.x - bounds.left) / bounds.width,
                                        (drop.y - bounds.top) / bounds.height
                                    )
                                )
                            }
                    }
                )
            }
        }
    }
}

@Composable
private fun DraggablePaletteElement(element: ElementDefinition, modifier: Modifier, onDrop: (Offset) -> Unit) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnDrop by rememberUpdatedState(onDrop)

    PrimitiveElement(
        element = element,
        modifier = modifier
            .onGloballyPositioned { coordinates = it }
            .pointerInput(element.id) {
                var lastPosition: Offset? = null
                detectDragGestures(
                    onDragStart = { position -> lastPosition = coordinates?.localToRoot(position) },
                    onDrag = { change, _ ->
                        change.consume()
                        lastPosition = coordinates?.localToRoot(change.position)
                    },
                    onDragEnd = { lastPosition?.let(currentOnDrop) },
                    onDragCancel = { lastPosition = null }
                )
            }
    )
}
