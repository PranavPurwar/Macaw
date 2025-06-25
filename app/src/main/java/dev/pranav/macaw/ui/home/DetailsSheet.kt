package dev.pranav.macaw.ui.home

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
import dev.pranav.macaw.util.getLastModifiedDate
import dev.pranav.macaw.util.sizeString
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(file: File, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var md5 by remember { mutableStateOf<String?>(null) }
    var sha1 by remember { mutableStateOf<String?>(null) }
    var sha256 by remember { mutableStateOf<String?>(null) }
    var sha512 by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(file) {
        if (file.isFile) {
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
                        placeholder = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
                        error = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            file.absolutePath,
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
                    if (file.isFile) {
                        item { DetailItem("Extension", file.extension) }
                    }
                    item { DetailItem("Last modified", file.getLastModifiedDate()) }
                    item { DetailItem("Size", file.length().sizeString()) }

                    try {
                        val attrs =
                            Files.readAttributes(file.toPath(), PosixFileAttributes::class.java)
                        item {
                            DetailItem(
                                "Permissions",
                                PosixFilePermissions.toString(attrs.permissions())
                            )
                        }
                        item { DetailItem("Owner", attrs.owner().name) }
                        item { DetailItem("Group", attrs.group().name) }
                    } catch (_: Exception) {
                        item { DetailItem("Permissions", "Not available") }
                        item { DetailItem("Owner", "Not available") }
                        item { DetailItem("Group", "Not available") }
                    }

                    if (file.isFile) {
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HashDetailItem("MD5", md5 ?: "Calculating...")
                                HashDetailItem("SHA1", sha1 ?: "Calculating...")
                                HashDetailItem("SHA256", sha256 ?: "Calculating...")
                                HashDetailItem("SHA512", sha512 ?: "Calculating...")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = value)
    }
}

@Composable
fun HashDetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

