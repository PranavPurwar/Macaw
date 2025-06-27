package dev.pranav.macaw.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import dev.pranav.macaw.model.Action
import dev.pranav.macaw.model.CloneAction
import dev.pranav.macaw.model.CompressAction
import dev.pranav.macaw.model.CopyAction
import dev.pranav.macaw.model.DeleteAction
import dev.pranav.macaw.model.ExtractAction
import dev.pranav.macaw.model.MoveAction
import dev.pranav.macaw.model.RenameAction
import dev.pranav.macaw.ui.actions.ActionState
import dev.pranav.macaw.util.compress
import dev.pranav.macaw.util.copyRecursively
import dev.pranav.macaw.util.deleteFile
import dev.pranav.macaw.util.duplicate
import dev.pranav.macaw.util.extract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.name

class ActionService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        ActionManager.actions
            .onEach { actions ->
                actions.filter { it.state.value is ActionState.Pending }
                    .forEach { action ->
                        serviceScope.launch {
                            handleAction(action)
                        }
                    }
            }
            .launchIn(serviceScope)

        return START_STICKY
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun handleAction(action: Action) {
        if (action.state.value !is ActionState.Pending) return

        when (action) {
            is ExtractAction -> {
                try {
                    action.state.value =
                        ActionState.InProgress(0f, "Extracting ${action.path.name}")
                    action.path.extract(
                        destinationDir = action.destination,
                        onProgress = { entryName, progress ->
                            action.state.value =
                                ActionState.InProgress(progress, "Extracting $entryName")
                        },
                        onConflict = action.onConflict,
                        shouldContinue = { !action.isCancelled.get() }
                    )
                    if (action.isCancelled.get()) {
                        action.state.value = ActionState.Failed("Cancelled")
                    } else {
                        action.state.value = ActionState.Completed("Extraction complete")
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }

            is CompressAction -> {
                try {
                    action.state.value = ActionState.InProgress(0f, "Compressing files")
                    action.files.compress(
                        destination = action.destination,
                        onProgress = { entryName, progress ->
                            action.state.value =
                                ActionState.InProgress(progress, "Compressing $entryName")
                        },
                        shouldContinue = { !action.isCancelled.get() }
                    )
                    if (action.isCancelled.get()) {
                        action.state.value = ActionState.Failed("Cancelled")
                    } else {
                        action.state.value = ActionState.Completed("Compression complete")
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }

            is DeleteAction -> {
                try {
                    action.state.value = ActionState.InProgress(0f, "Deleting files")
                    val totalFiles = action.files.size.toFloat()
                    action.files.forEachIndexed { index, file ->
                        if (action.isCancelled.get()) {
                            action.state.value = ActionState.Failed("Cancelled")
                            return
                        }
                        file.deleteFile(
                            onProgress = { fileName, progress ->
                                action.state.value = ActionState.InProgress(
                                    (index + progress) / totalFiles,
                                    "Deleting $fileName"
                                )
                            },
                            shouldContinue = { !action.isCancelled.get() }
                        )
                    }
                    if (action.isCancelled.get()) {
                        action.state.value = ActionState.Failed("Cancelled")
                    } else {
                        action.state.value = ActionState.Completed("Deletion complete")
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }

            is CopyAction -> {
                try {
                    action.state.value = ActionState.InProgress(0f, "Copying files")
                    val totalFiles = action.files.size.toFloat()
                    action.files.forEachIndexed { index, file ->
                        if (action.isCancelled.get()) {
                            action.state.value = ActionState.Failed("Cancelled")
                            return
                        }
                        val destinationFile = action.destination.resolve(file.name)
                        file.copyRecursively(
                            target = destinationFile,
                            onProgress = { fileName, progress ->
                                action.state.value = ActionState.InProgress(
                                    (index + progress) / totalFiles,
                                    "Copying $fileName"
                                )
                            },
                            onConflict = action.onConflict,
                            shouldContinue = { !action.isCancelled.get() }
                        )
                    }
                    if (action.isCancelled.get()) {
                        action.state.value = ActionState.Failed("Cancelled")
                    } else {
                        action.state.value = ActionState.Completed("Copy complete")
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }

            is MoveAction -> {
                try {
                    action.state.value = ActionState.InProgress(0f, "Moving files")
                    val totalFiles = action.files.size.toFloat()
                    action.files.forEachIndexed { index, file ->
                        if (action.isCancelled.get()) {
                            action.state.value = ActionState.Failed("Cancelled")
                            return
                        }
                        val destinationFile = action.destination.resolve(file.name)
                        try {
                            file.moveTo(destinationFile)
                            action.state.value = ActionState.InProgress(
                                (index + 1) / totalFiles,
                                "Moving ${file.name}"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()

                            // If move fails, fall back to copy and delete
                            file.copyRecursively(
                                target = destinationFile,
                                onProgress = { fileName, progress ->
                                    action.state.value = ActionState.InProgress(
                                        (index + progress / 2) / totalFiles,
                                        "Copying $fileName"
                                    )
                                },
                                onConflict = action.onConflict,
                                shouldContinue = { !action.isCancelled.get() }
                            )
                            if (action.isCancelled.get()) {
                                action.state.value = ActionState.Failed("Cancelled")
                                return
                            }
                            file.deleteFile(
                                onProgress = { fileName, progress ->
                                    action.state.value = ActionState.InProgress(
                                        (index + 0.5f + progress / 2) / totalFiles,
                                        "Deleting $fileName"
                                    )
                                },
                                shouldContinue = { !action.isCancelled.get() }
                            )
                        }
                    }
                    if (action.isCancelled.get()) {
                        action.state.value = ActionState.Failed("Cancelled")
                    } else {
                        action.state.value = ActionState.Completed("Move complete")
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }

            is RenameAction -> {
                try {
                    action.state.value = ActionState.InProgress(0f, "Renaming file...")
                    val newFile = action.path.resolveSibling(action.newName)

                    if (newFile.exists()) {
                        action.state.value =
                            ActionState.Failed("File with this name already exists")
                        return
                    }

                    try {
                        action.path.moveTo(newFile)
                        action.state.value = ActionState.Completed("Rename complete")
                        return
                    } catch (e: Exception) {
                        e.printStackTrace()

                        // If move fails, fall back to copy and delete
                        action.path.copyRecursively(
                            target = newFile,
                            onProgress = { entryName, progress ->
                                action.state.value =
                                    ActionState.InProgress(progress * 0.5f, "Copying $entryName")
                            },
                            onConflict = action.onConflict,
                            shouldContinue = { !action.isCancelled.get() }
                        )

                        if (action.isCancelled.get()) {
                            newFile.deleteRecursively()
                            action.state.value = ActionState.Failed("Cancelled")
                            return
                        }

                        action.path.deleteFile(
                            onProgress = { entryName, progress ->
                                action.state.value = ActionState.InProgress(
                                    0.5f + progress * 0.5f,
                                    "Deleting $entryName"
                                )
                            },
                            shouldContinue = { !action.isCancelled.get() }
                        )

                        if (action.isCancelled.get()) {
                            action.state.value = ActionState.Failed("Cancelled")
                        } else {
                            action.state.value = ActionState.Completed("Rename complete")
                        }
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }

            is CloneAction -> {
                try {
                    action.state.value = ActionState.InProgress(0f, "Cloning ${action.path.name}")

                    val duplicated = action.path.duplicate(
                        onProgress = { fileName, progress ->
                            action.state.value =
                                ActionState.InProgress(progress, "Cloning $")
                        },
                        shouldContinue = { !action.isCancelled.get() }
                    )

                    if (action.isCancelled.get()) {
                        action.state.value = ActionState.Failed("Cancelled")
                    } else {
                        action.state.value =
                            ActionState.Completed("Clone complete: ${duplicated.name}")
                    }
                } catch (e: Exception) {
                    action.state.value = ActionState.Failed(e.message ?: "Unknown error")
                }
            }
        }
        if (action.state.value is ActionState.Completed) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    applicationContext,
                    (action.state.value as ActionState.Completed).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
