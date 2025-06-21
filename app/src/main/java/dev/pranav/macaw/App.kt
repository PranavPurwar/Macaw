package dev.pranav.macaw

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.allowConversionToBitmap
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import dev.pranav.macaw.util.Prefs
import dev.pranav.macaw.util.coil.ApkDecoder
import dev.pranav.macaw.util.coil.AudioFileFetcherFactory
import dev.pranav.macaw.util.coil.FileMapper
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver

class App: Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var app: App
        lateinit var prefs: Prefs
    }

    override fun onCreate() {
        app = this
        prefs = Prefs(this)

        setupTextmate()

        super.onCreate()
    }

    private fun setupTextmate() {
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .allowConversionToBitmap(true)
            .components {
                add(AudioFileFetcherFactory(context))
                add(FileMapper())
                add(ApkDecoder.Factory(context))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}
