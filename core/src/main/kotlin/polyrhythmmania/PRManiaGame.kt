package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import polyrhythmmania.init.InitalAssetLoader
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.PaintboxSettings
import io.github.chrislo27.paintbox.ResizeAction
import io.github.chrislo27.paintbox.font.FontCache
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.PaintboxFontFreeType
import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.registry.AssetRegistry
import org.lwjgl.glfw.GLFW
import polyrhythmmania.init.AssetRegistryLoadingScreen
import polyrhythmmania.world.render.TestWorldRenderScreen
import sun.rmi.runtime.Log
import java.io.File


class PRManiaGame(paintboxSettings: PaintboxSettings)
    : PaintboxGame(paintboxSettings) {

    companion object {
        fun createPaintboxSettings(launchArguments: List<String>, logger: Logger, logToFile: File?): PaintboxSettings =
                PaintboxSettings(launchArguments, logger, logToFile, PRMania.VERSION, PRMania.DEFAULT_SIZE,
                        ResizeAction.ANY_SIZE, PRMania.MINIMUM_SIZE)
    }

    override fun getTitle(): String = PRMania.TITLE

    override fun create() {
        super.create()
        this.localizationInstance = Localization
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        GLFW.glfwSetWindowAspectRatio(windowHandle, 3, 2)

        addFontsToCache(this.fontCache)

        AssetRegistry.addAssetLoader(InitalAssetLoader())

        setScreen(AssetRegistryLoadingScreen(this).apply {
            nextScreen = {
                TestWorldRenderScreen(this@PRManiaGame)
            }
        })
    }

    override fun dispose() {
        super.dispose()
    }

    private fun addFontsToCache(cache: FontCache) {
        val emulatedSize = paintboxSettings.emulatedSize
        fun makeParam() = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            magFilter = Texture.TextureFilter.Linear
            minFilter = Texture.TextureFilter.Linear
            genMipMaps = false
            incremental = true
            mono = false
            color = Color(1f, 1f, 1f, 1f)
            borderColor = Color(0f, 0f, 0f, 1f)
            characters = ""
            hinting = FreeTypeFontGenerator.Hinting.AutoFull
        }

        val afterLoad: PaintboxFontFreeType.(font: BitmapFont) -> Unit = { font ->
//            font.data.blankLineScale = 0.75f
        }
        cache["OpenSans"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-Regular.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 0f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-Regular.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 1.5f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_ITALIC"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-Italic.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 0f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_ITALIC_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-Italic.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 1.5f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_BOLD"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-Bold.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 0f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_BOLD_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-Bold.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 1.5f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_BOLD_ITALIC"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-BoldItalic.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 0f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["OpenSans_BOLD_ITALIC_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("fonts/OpenSans/OpenSans-BoldItalic.ttf"), emulatedSize,
                makeParam().apply {
                    size = 18
                    borderWidth = 1.5f
                }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
    }


    val mainFont: PaintboxFont inline get() = fontCache["OpenSans"]
    val mainFontBordered: PaintboxFont inline get() = fontCache["OpenSans_BORDERED"]
    val mainFontBold: PaintboxFont inline get() = fontCache["OpenSans_BOLD"]
    val mainFontBoldBordered: PaintboxFont inline get() = fontCache["OpenSans_BOLD_BORDERED"]
    val mainFontItalic: PaintboxFont inline get() = fontCache["OpenSans_ITALIC"]
    val mainFontItalicBordered: PaintboxFont inline get() = fontCache["OpenSans_ITALIC_BORDERED"]
    val mainFontBoldItalic: PaintboxFont inline get() = fontCache["OpenSans_BOLD_ITALIC"]
    val mainFontBoldItalicBordered: PaintboxFont inline get() = fontCache["OpenSans_BOLD_ITALIC_BORDERED"]

}