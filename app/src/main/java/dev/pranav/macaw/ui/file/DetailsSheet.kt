package dev.pranav.macaw.ui.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pranav.macaw.R
import dev.pranav.macaw.util.getHash
import dev.pranav.macaw.util.getLastModifiedFormattedNative
import dev.pranav.macaw.util.sizeString
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(file: Path, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var md5 by remember { mutableStateOf<String?>(null) }
    var sha1 by remember { mutableStateOf<String?>(null) }
    var sha256 by remember { mutableStateOf<String?>(null) }
    var sha512 by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(file) {
        if (file.isRegularFile()) {
            coroutineScope.launch {
                md5 = file.getHash("MD5")
                sha1 = file.getHash("SHA-1")
                sha256 = file.getHash("SHA-256")
                sha512 = file.getHash("SHA-512")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = file,
                        placeholder = painterResource(if (file.isDirectory()) R.drawable.twotone_folder_24 else R.mipmap.unk),
                        error = painterResource(if (file.isDirectory()) R.drawable.twotone_folder_24 else R.mipmap.unk),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            file.absolutePathString(),
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (file.isRegularFile()) {
                        item { DetailItem("Extension", file.extension) }
                    }
                    item { DetailItem("Last modified", file.getLastModifiedFormattedNative()) }
                    item { DetailItem("Size", file.fileSize().sizeString()) }

                    try {
                        val attrs =
                            Files.readAttributes(file, PosixFileAttributes::class.java)
                        val perms = PosixFilePermissions.toString(attrs.permissions())
                        item { DetailItem("Permissions", perms) }
                        item { DetailItem("Owner", attrs.owner().name) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Ignore
                    }

                    if (file.isRegularFile()) {
                        if (md5 != null) {
                            item { DetailItem("MD5", md5!!) }
                        }
                        if (sha1 != null) {
                            item { DetailItem("SHA-1", sha1!!) }
                        }
                        if (sha256 != null) {
                            item { DetailItem("SHA-256", sha256!!) }
                        }
                        if (sha512 != null) {
                            item { DetailItem("SHA-512", sha512!!) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontWeight = FontWeight.Bold)
        Text(value)
    }
}

