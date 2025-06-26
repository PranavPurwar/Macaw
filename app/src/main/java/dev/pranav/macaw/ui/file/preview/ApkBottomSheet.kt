package dev.pranav.macaw.ui.file.preview

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import dev.pranav.macaw.util.sizeString
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun ApkBottomSheet(file: Path, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apkInfo by remember { mutableStateOf<ApkInfo?>(null) }
    var installStatus by remember { mutableStateOf<InstallStatus>(InstallStatus.NotInstalled) }
    var isIconExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        apkInfo = getApkInfo(context, file)
        apkInfo?.let {
            installStatus = getInstallStatus(context, it.packageName)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        BoxWithConstraints {
            val screenWidth = this.maxWidth
            val screenHeight = this.maxHeight
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    apkInfo?.let { info ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isIconExpanded) 1f else 0f)
                        ) {
                            if (info.icon != null) {
                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = info.icon.asImageBitmap(),
                                        contentDescription = "App Icon",
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = if (isIconExpanded) 3f else 1f
                                                scaleY = if (isIconExpanded) 3f else 1f
                                                if (isIconExpanded) {
                                                    translationX = (screenWidth / 2 - 48.dp).toPx()
                                                    translationY = (screenHeight / 2 - 56.dp).toPx()
                                                } else {
                                                    translationX = 0f
                                                    translationY = 0f
                                                }
                                            }
                                            .clip(RoundedCornerShape(8.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (isIconExpanded) {
                                                        isIconExpanded = false
                                                    }
                                                },
                                                onLongClick = {
                                                    isIconExpanded = !isIconExpanded
                                                })
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .graphicsLayer {
                                            scaleX = if (isIconExpanded) 3f else 1f
                                            scaleY = if (isIconExpanded) 3f else 1f
                                            if (isIconExpanded) {
                                                translationX = (screenWidth / 2 - 48.dp).toPx()
                                                translationY = (screenHeight / 2 - 56.dp).toPx()
                                            } else {
                                                translationX = 0f
                                                translationY = 0f
                                            }
                                        }
                                        .combinedClickable(
                                            onClick = {
                                                if (isIconExpanded) {
                                                    isIconExpanded = false
                                                }
                                            },
                                            onLongClick = {
                                                isIconExpanded = !isIconExpanded
                                            }),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .blur(if (isIconExpanded) 8.dp else 0.dp)
                            ) {
                                Text(
                                    modifier = Modifier.padding(start = 16.dp),
                                text = info.appName ?: info.packageName,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    modifier = Modifier.padding(start = 16.dp),
                                    text = info.packageName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    modifier = Modifier.padding(start = 16.dp),
                                    text = "Version: ${info.versionName} (${info.versionCode})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Column(modifier = Modifier.blur(if (isIconExpanded) 8.dp else 0.dp)) {

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (installStatus is InstallStatus.Installed) {
                                    FilledTonalButton(
                                        onClick = {
                                            uninstallApp(context, info.packageName)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Uninstall"
                                        )
                                        Text(
                                            text = "Uninstall",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(
                                                FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    file.toFile()
                                                ), "application/vnd.android.package-archive"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Install"
                                    )
                                    Text(
                                        text = if (installStatus is InstallStatus.Installed) "Update" else "Install",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            if (installStatus is InstallStatus.Installed) {
                                TextButton(
                                    onClick = {
                                        openAppInfo(context, info.packageName)
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("App Info")
                                }
                            }

                            if (installStatus is InstallStatus.Installed) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Installation Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        val installedInfo = installStatus as InstallStatus.Installed
                                        DetailItem("Installed Version", installedInfo.versionName)
                                        DetailItem(
                                            "First Install",
                                            formatTimestamp(installedInfo.firstInstallTime)
                                        )
                                        DetailItem(
                                            "Last Updated",
                                            formatTimestamp(installedInfo.lastUpdateTime)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            Text(
                                text = "APK Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    DetailItem("Min SDK", info.minSdkVersion.toString())
                                    DetailItem("Target SDK", info.targetSdkVersion.toString())
                                    DetailItem("Install Location", info.installLocation)
                                    info.sharedUserId?.let { DetailItem("Shared User ID", it) }
                                    DetailItem("File Size", info.fileSize)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Signatures",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    info.signatures.forEachIndexed { index, signature ->
                                        Column {
                                            Text("Issuer", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                signature.issuer,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            Text("Subject", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                signature.subject,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            Text("Serial", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                signature.serialNumber,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            Text("SHA-256", fontWeight = FontWeight.SemiBold)
                                            Text(signature.sha256)
                                        }

                                        if (index < info.signatures.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                    }

                                    if (info.signatures.isEmpty()) {
                                        Text("No signature information available")
                                    }
                                }
                            }

                            info.metaData?.let { bundle ->
                                if (!bundle.isEmpty) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Metadata",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            bundle.keySet().sorted().forEach { key ->
                                                val value = @Suppress("DEPRECATION") bundle.get(key)
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = key,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(bottom = 4.dp)
                                                    )
                                                    Text(text = value?.toString() ?: "N/A")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Permissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    info.permissions.forEach { permission ->
                                        Text(
                                            text = "• $permission",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }

                                    if (info.permissions.isEmpty()) {
                                        Text("No permissions required")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    } ?: run {
                        Text(
                            text = "Loading APK information...",
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(0.4f)
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.6f)
        )
    }
}

data class ApkInfo(
    val packageName: String,
    val appName: String?,
    val versionName: String?,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val installLocation: String,
    val icon: Bitmap?,
    val fileSize: String,
    val permissions: List<String>,
    val signatures: List<SignatureInfo>,
    val sharedUserId: String?,
    val metaData: Bundle?
)

data class SignatureInfo(
    val issuer: String,
    val subject: String,
    val serialNumber: String,
    val sha256: String
)

sealed class InstallStatus {
    object NotInstalled : InstallStatus()
    data class Installed(
        val versionName: String,
        val firstInstallTime: Long,
        val lastUpdateTime: Long
    ) : InstallStatus()
}

private fun getApkInfo(context: Context, apkFile: Path): ApkInfo? {
    val packageManager = context.packageManager

    @Suppress("DEPRECATION")
    var flags =
        PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA or PackageManager.GET_SIGNATURES
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        flags = flags or PackageManager.GET_SIGNING_CERTIFICATES
    }

    @Suppress("DEPRECATION")
    val packageInfo = packageManager.getPackageArchiveInfo(
        apkFile.absolutePathString(),
        flags
    )

    packageInfo?.applicationInfo?.sourceDir = apkFile.absolutePathString()
    packageInfo?.applicationInfo?.publicSourceDir = apkFile.absolutePathString()

    return packageInfo?.let {
        val appName = it.applicationInfo?.let { appInfo ->
            packageManager.getApplicationLabel(appInfo).toString()
        }
        val icon = it.applicationInfo?.loadIcon(packageManager)?.toBitmap()
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && it.signingInfo?.apkContentsSigners != null) {
                it.signingInfo!!.apkContentsSigners.mapNotNull { sig -> extractSignatureInfo(sig) }
            } else {
                @Suppress("DEPRECATION")
                it.signatures?.mapNotNull { sig -> extractSignatureInfo(sig) }
            }

        @Suppress("DEPRECATION")
        ApkInfo(
            appName = appName,
            packageName = it.packageName,
            versionName = it.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong(),
            minSdkVersion = it.applicationInfo?.minSdkVersion ?: 0,
            targetSdkVersion = it.applicationInfo?.targetSdkVersion ?: 0,
            fileSize = apkFile.fileSize().sizeString(),
            installLocation = formatInstallLocation(it.installLocation),
            icon = icon,
            permissions = it.requestedPermissions?.toList() ?: emptyList(),
            signatures = signatures ?: emptyList(),
            sharedUserId = it.sharedUserId,
            metaData = it.applicationInfo?.metaData
        )
    }
}

private fun extractSignatureInfo(signature: Signature): SignatureInfo? {
    return try {
        val certFactory = CertificateFactory.getInstance("X.509")
        val cert =
            certFactory.generateCertificate(ByteArrayInputStream(signature.toByteArray())) as X509Certificate
        SignatureInfo(
            issuer = cert.issuerX500Principal.name,
            subject = cert.subjectX500Principal.name,
            serialNumber = cert.serialNumber.toString(16).uppercase(Locale.ROOT),
            sha256 = calculateSignatureSha256(cert.encoded)
        )
    } catch (_: Exception) {
        null
    }
}

private fun calculateSignatureSha256(signature: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(signature)
    return digest.joinToString("") { "%02x".format(it) }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatInstallLocation(location: Int): String {
    return when (location) {
        1 -> "Internal Only"
        2 -> "Prefer External"
        0 -> "Auto"
        else -> "Unknown"
    }
}

private fun getInstallStatus(context: Context, packageName: String): InstallStatus {
    return try {
        val installedPackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        InstallStatus.Installed(
            installedPackageInfo.versionName ?: "N/A",
            installedPackageInfo.firstInstallTime,
            installedPackageInfo.lastUpdateTime
        )
    } catch (_: PackageManager.NameNotFoundException) {
        InstallStatus.NotInstalled
    }
}

private fun uninstallApp(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = "package:${'$'}packageName".toUri()
    }
    context.startActivity(intent)
}

private fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
