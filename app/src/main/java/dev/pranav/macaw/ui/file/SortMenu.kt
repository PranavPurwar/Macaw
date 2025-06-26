package dev.pranav.macaw.ui.file

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.util.SortOrder
import me.saket.cascade.CascadeDropdownMenu

@Composable
fun SortMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    CascadeDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = { Text("Name (A-Z)") },
            onClick = { onSortOrderSelected(SortOrder.NAME_ASCENDING) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.SortByAlpha,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Sort by name ascending",
                )
            },
            trailingIcon = if (currentSortOrder == SortOrder.NAME_ASCENDING) {
                { Icon(Icons.Default.ArrowUpward, contentDescription = "Selected") }
            } else null
        )

        DropdownMenuItem(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = { Text("Name (Z-A)") },
            onClick = { onSortOrderSelected(SortOrder.NAME_DESCENDING) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.SortByAlpha,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Sort by name descending"
                )
            },
            trailingIcon = if (currentSortOrder == SortOrder.NAME_DESCENDING) {
                { Icon(Icons.Default.ArrowDownward, contentDescription = "Selected") }
            } else null
        )

        // Date sorting options
        DropdownMenuItem(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = { Text("Date (Oldest first)") },
            onClick = { onSortOrderSelected(SortOrder.DATE_ASCENDING) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DateRange,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Sort by date ascending"
                )
            },
            trailingIcon = if (currentSortOrder == SortOrder.DATE_ASCENDING) {
                { Icon(Icons.Default.ArrowUpward, contentDescription = "Selected") }
            } else null
        )

        DropdownMenuItem(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = { Text("Date (Newest first)") },
            onClick = { onSortOrderSelected(SortOrder.DATE_DESCENDING) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DateRange,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Sort by date descending"
                )
            },
            trailingIcon = if (currentSortOrder == SortOrder.DATE_DESCENDING) {
                { Icon(Icons.Default.ArrowDownward, contentDescription = "Selected") }
            } else null
        )

        // Size sorting options
        DropdownMenuItem(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = { Text("Size (Smallest first)") },
            onClick = { onSortOrderSelected(SortOrder.SIZE_ASCENDING) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Storage,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Sort by size ascending"
                )
            },
            trailingIcon = if (currentSortOrder == SortOrder.SIZE_ASCENDING) {
                { Icon(Icons.Default.ArrowUpward, contentDescription = "Selected") }
            } else null
        )

        DropdownMenuItem(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = { Text("Size (Largest first)") },
            onClick = { onSortOrderSelected(SortOrder.SIZE_DESCENDING) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Storage,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Sort by size descending"
                )
            },
            trailingIcon = if (currentSortOrder == SortOrder.SIZE_DESCENDING) {
                { Icon(Icons.Default.ArrowDownward, contentDescription = "Selected") }
            } else null
        )
    }
}

