package dev.pranav.macaw.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.pranav.macaw.ui.actions.ActionState
import dev.pranav.macaw.util.ConflictInfo
import dev.pranav.macaw.util.ConflictResolution
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

sealed interface Action {
    val id: Long
    val files: List<File>
    var state: MutableState<ActionState>
    val isCancelled: AtomicBoolean
    val onConflict: suspend (ConflictInfo) -> ConflictResolution
}

data class RenameAction(
    override val id: Long,
    val file: File,
    val newName: String,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.SKIP }
) : Action {
    override val files: List<File> get() = listOf(file)
}

data class DeleteAction(
    override val id: Long,
    override val files: List<File>,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.SKIP }
) : Action

data class CopyAction(
    override val id: Long,
    override val files: List<File>,
    val destination: File,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.OVERWRITE }
) : Action

data class MoveAction(
    override val id: Long,
    override val files: List<File>,
    val destination: File,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.OVERWRITE }
) : Action

data class CompressAction(
    override val id: Long,
    override val files: List<File>,
    val destination: File,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.SKIP }
) : Action

data class ExtractAction(
    override val id: Long,
    val file: File,
    val destination: File,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.SKIP }
) : Action {
    override val files: List<File> get() = listOf(file)
}

data class CloneAction(
    override val id: Long,
    val file: File,
    override var state: MutableState<ActionState> = mutableStateOf(ActionState.Pending),
    override val isCancelled: AtomicBoolean = AtomicBoolean(false),
    override val onConflict: suspend (ConflictInfo) -> ConflictResolution = { ConflictResolution.RENAME }
) : Action {
    override val files: List<File> get() = listOf(file)
}
