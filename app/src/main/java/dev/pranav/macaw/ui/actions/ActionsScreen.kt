package dev.pranav.macaw.ui.actions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.Action
import dev.pranav.macaw.model.CloneAction
import dev.pranav.macaw.model.CompressAction
import dev.pranav.macaw.model.CopyAction
import dev.pranav.macaw.model.DeleteAction
import dev.pranav.macaw.model.ExtractAction
import dev.pranav.macaw.model.MoveAction
import dev.pranav.macaw.model.RenameAction
import dev.pranav.macaw.service.ActionManager
import kotlin.io.path.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen() {
    val actions by ActionManager.actions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Active Operations",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { padding ->
        AnimatedContent(
            targetState = actions.isEmpty(),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(actions, key = { it.id }) { action ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                animationSpec = tween(300),
                                initialOffsetY = { it / 2 }
                            ) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(
                                animationSpec = tween(300),
                                targetOffsetY = { -it / 2 }
                            ) + fadeOut(animationSpec = tween(300))
                        ) {
                            ActionItem(action = action)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "All caught up!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "No active file operations at the moment",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionItem(action: Action) {
    val state by action.state

    val containerColor by animateColorAsState(
        targetValue = when (state) {
            is ActionState.Completed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            is ActionState.Failed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(300)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (state is ActionState.InProgress) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionIcon(action = action, state = state)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.files.joinToString { it.name },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionStateContent(state = state)
            }
            Spacer(modifier = Modifier.width(16.dp))
            ActionTrailingIcon(action = action, state = state)
        }
    }
}

@Composable
private fun ActionIcon(action: Action, state: ActionState) {
    val icon = when (action) {
        is CopyAction -> Icons.Default.ContentCopy
        is MoveAction -> Icons.Default.MoveToInbox
        is DeleteAction -> Icons.Default.Clear
        is RenameAction -> Icons.Default.Edit
        is CompressAction -> Icons.Default.Archive
        is ExtractAction -> Icons.Default.Unarchive
        is CloneAction -> Icons.Default.FileCopy
    }

    val iconColor by animateColorAsState(
        targetValue = when (state) {
            is ActionState.Completed -> MaterialTheme.colorScheme.primary
            is ActionState.Failed -> MaterialTheme.colorScheme.error
            is ActionState.InProgress -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300)
    )

    val scale by animateFloatAsState(
        targetValue = if (state is ActionState.InProgress) 1.1f else 1f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = iconColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = iconColor
        )
    }
}

@Composable
private fun ActionStateContent(state: ActionState) {
    when (state) {
        is ActionState.Completed -> {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        is ActionState.Failed -> {
            Text(
                text = state.reason,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.error
            )
        }

        is ActionState.InProgress -> {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        is ActionState.Pending -> {
            Text(
                text = "Queued for processing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionTrailingIcon(action: Action, state: ActionState) {
    when (state) {
        is ActionState.Completed -> {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(animationSpec = tween(300))
            ) {
                IconButton(
                    onClick = { ActionManager.removeAction(action) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        is ActionState.Failed -> {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(animationSpec = tween(300))
            ) {
                IconButton(
                    onClick = { ActionManager.removeAction(action) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        else -> {
            IconButton(
                onClick = { action.isCancelled.set(true) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
