package dev.pranav.macaw.ui.actions

sealed interface ActionState {
    data object Pending : ActionState
    data class InProgress(val progress: Float, val message: String = "") : ActionState
    data class Completed(val message: String) : ActionState
    data class Failed(val reason: String) : ActionState
}

