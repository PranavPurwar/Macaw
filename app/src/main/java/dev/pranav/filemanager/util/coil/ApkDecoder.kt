package dev.pranav.filemanager.util.coil

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import java.io.File

class ApkDecoder(
    private val context: Context,
    private val apk: File
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_META_DATA)

            if (packageInfo == null) {
                Log.w("ApkDecoder", "Failed to get package info for: ${apk.absolutePath}")
                return null
            }

            // Ensure applicationInfo is not null before proceeding
            val appInfo: ApplicationInfo = packageInfo.applicationInfo ?: run {
                Log.w("ApkDecoder", "ApplicationInfo is null for: ${apk.absolutePath}")
                return null
            }

            appInfo.sourceDir = apk.absolutePath
            appInfo.publicSourceDir = apk.absolutePath

            val iconDrawable: Drawable? = appInfo.loadIcon(packageManager)

            if (iconDrawable == null) {
                Log.w("ApkDecoder", "Failed to load icon for: ${apk.absolutePath}")
                return null
            }

            val bitmap = iconDrawable.toBitmap()

            DecodeResult(
                image = bitmap.asImage(),
                isSampled = true
            )
        } catch (e: Exception) {
            Log.e("ApkDecoder", "Error decoding APK icon: ${apk.absolutePath}", e)
            null
        }
    }

    class Factory(private val context: Context) : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            Log.e("ApkDecoder", "Source: ${result.source.fileOrNull()?.name}")
            val file = result.source.fileOrNull()?.toFile() ?: return null
            if (file.extension.equals("apk", ignoreCase = true)) {
                return ApkDecoder(context, file)
            }
            return null
        }
    }
}

