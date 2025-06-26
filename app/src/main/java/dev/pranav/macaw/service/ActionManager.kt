package dev.pranav.macaw.service

import dev.pranav.macaw.model.Action
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActionManager {
    private val _actions = MutableStateFlow<List<Action>>(emptyList())
    val actions = _actions.asStateFlow()

    private val _activeDialogAction = MutableStateFlow<Action?>(null)
    val activeDialogAction = _activeDialogAction.asStateFlow()

    fun addAction(action: Action) {
        _actions.value = _actions.value + action
        _activeDialogAction.value = action
    }

    fun removeAction(action: Action) {
        _actions.value = _actions.value - action
        if (_activeDialogAction.value?.id == action.id) {
            _activeDialogAction.value = null
        }
    }

    fun hideDialog() {
        _activeDialogAction.value = null
    }

    fun cancelAction(action: Action) {
        action.isCancelled.set(true)
    }
}
