package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import polyrhythmmania.init.InitialAssetLoader
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.PaintboxSettings
import io.github.chrislo27.paintbox.ResizeAction
import io.github.chrislo27.paintbox.font.*
import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.isAltDown
import io.github.chrislo27.paintbox.util.gdxutils.isControlDown
import io.github.chrislo27.paintbox.util.gdxutils.isShiftDown
import org.lwjgl.glfw.GLFW
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.engine.input.InputThresholds
import polyrhythmmania.init.AssetRegistryLoadingScreen
import java.io.File


class PRManiaGame(paintboxSettings: PaintboxSettings)
    : PaintboxGame(paintboxSettings) {

    companion object {
        fun createPaintboxSettings(launchArguments: List<String>, logger: Logger, logToFile: File?): PaintboxSettings =
                PaintboxSettings(launchArguments, logger, logToFile, PRMania.VERSION, PRMania.DEFAULT_SIZE,
                        ResizeAction.ANY_SIZE, PRMania.MINIMUM_SIZE)
    }

    private var lastWindowed: WindowSize = PRMania.DEFAULT_SIZE.copy()

    override fun getTitle(): String = "${PRMania.TITLE} ${PRMania.VERSION}"

    override fun create() {
        super.create()
        this.localizationInstance = Localization
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        GLFW.glfwSetWindowAspectRatio(windowHandle, 16, 9)
//        GLFW.glfwSetWindowAspectRatio(windowHandle, 3, 2)

        addFontsToCache(this.fontCache)
        PRManiaColors

        AssetRegistry.addAssetLoader(InitialAssetLoader())


        setScreen(AssetRegistryLoadingScreen(this).apply {
            onStart = {
                InputThresholds.initInputClasses()
            }
            nextScreen = {
//                TestWorldRenderScreen(this@PRManiaGame)
                EditorScreen(this@PRManiaGame)
            }
        })
    }

    override fun dispose() {
        super.dispose()
    }


    fun attemptFullscreen() {
        lastWindowed = WindowSize(Gdx.graphics.width, Gdx.graphics.height)
        Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
    }

    fun attemptEndFullscreen() {
        val last = lastWindowed
        Gdx.graphics.setWindowedMode(last.width, last.height)
    }

    fun attemptResetWindow() {
        Gdx.graphics.setWindowedMode(PRMania.DEFAULT_SIZE.width, PRMania.DEFAULT_SIZE.height)
    }

    override fun keyDown(keycode: Int): Boolean {
        val res = super.keyDown(keycode)
        if (!res) {
            if (!Gdx.input.isControlDown() && !Gdx.input.isAltDown()) {
                if (keycode == Input.Keys.F11) {
                    if (!Gdx.input.isShiftDown()) {
                        if (Gdx.graphics.isFullscreen) {
                            attemptEndFullscreen()
                        } else {
                            attemptFullscreen()
                        }
                    } else {
                        attemptResetWindow()
                    }
//                    persistWindowSettings()
                    return true
                }
            }
        }
        return res
    }

    private fun addFontsToCache(cache: FontCache) {
        val emulatedSize = paintboxSettings.emulatedSize
        fun makeParam() = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear //Nearest
            genMipMaps = false
            incremental = true
            mono = false
            color = Color(1f, 1f, 1f, 1f)
            borderColor = Color(0f, 0f, 0f, 1f)
            characters = ""
            hinting = FreeTypeFontGenerator.Hinting.Medium
        }

        val defaultAfterLoad: PaintboxFontFreeType.(font: BitmapFont) -> Unit = { font ->
            font.setUseIntegerPositions(true) // Filtering doesn't kick in so badly, solves "wiggly" glyphs
            font.setFixedWidthGlyphs("0123456789")
        }
        val defaultFontSize = 20

        fun addFontFamily(
                familyName: String, fontIDPrefix: String = familyName,
                normalFilename: String = "$familyName-Regular.ttf",
                normalItalicFilename: String = "$familyName-Italic.ttf",
                boldFilename: String = "$familyName-Bold.ttf",
                boldItalicFilename: String = "$familyName-BoldItalic.ttf",
                fontSize: Int = defaultFontSize, borderWidth: Float = 1.5f, folder: String = familyName,
                hinting: FreeTypeFontGenerator.Hinting? = null, generateBordered: Boolean = true,
                scaleToReferenceSize: Boolean = false,
                afterLoadFunc: PaintboxFontFreeType.(BitmapFont) -> Unit = defaultAfterLoad,
        ) {

            cache["${fontIDPrefix}"] = PaintboxFontFreeType(
                    PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$normalFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                    makeParam().apply {
                        if (hinting != null) {
                            this.hinting = hinting
                        }
                        this.size = fontSize
                        this.borderWidth = 0f
                    }).setAfterLoad(afterLoadFunc)
            if (generateBordered) {
                cache["${fontIDPrefix}_BORDERED"] = PaintboxFontFreeType(
                        PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$normalFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                        makeParam().apply {
                            if (hinting != null) {
                                this.hinting = hinting
                            }
                            this.size = fontSize
                            this.borderWidth = borderWidth
                        }).setAfterLoad(afterLoadFunc)
            }
            cache["${fontIDPrefix}_ITALIC"] = PaintboxFontFreeType(
                    PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$normalItalicFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                    makeParam().apply {
                        if (hinting != null) {
                            this.hinting = hinting
                        }
                        this.size = fontSize
                        this.borderWidth = 0f
                    }).setAfterLoad(afterLoadFunc)
            if (generateBordered) {
                cache["${fontIDPrefix}_ITALIC_BORDERED"] = PaintboxFontFreeType(
                        PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$normalItalicFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                        makeParam().apply {
                            if (hinting != null) {
                                this.hinting = hinting
                            }
                            this.size = fontSize
                            this.borderWidth = borderWidth
                        }).setAfterLoad(afterLoadFunc)
            }
            cache["${fontIDPrefix}_BOLD"] = PaintboxFontFreeType(
                    PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$boldFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                    makeParam().apply {
                        if (hinting != null) {
                            this.hinting = hinting
                        }
                        this.size = fontSize
                        this.borderWidth = 0f
                    }).setAfterLoad(afterLoadFunc)
            if (generateBordered) {
                cache["${fontIDPrefix}_BOLD_BORDERED"] = PaintboxFontFreeType(
                        PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$boldFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                        makeParam().apply {
                            if (hinting != null) {
                                this.hinting = hinting
                            }
                            this.size = fontSize
                            this.borderWidth = borderWidth
                        }).setAfterLoad(afterLoadFunc)
            }
            cache["${fontIDPrefix}_BOLD_ITALIC"] = PaintboxFontFreeType(
                    PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$boldItalicFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                    makeParam().apply {
                        if (hinting != null) {
                            this.hinting = hinting
                        }
                        this.size = fontSize
                        this.borderWidth = 0f
                    }).setAfterLoad(afterLoadFunc)
            if (generateBordered) {
                cache["${fontIDPrefix}_BOLD_ITALIC_BORDERED"] = PaintboxFontFreeType(
                        PaintboxFontParams(Gdx.files.internal("fonts/${folder}/$boldItalicFilename"), 1, 1f, scaleToReferenceSize, WindowSize(1280, 720)),
                        makeParam().apply {
                            if (hinting != null) {
                                this.hinting = hinting
                            }
                            this.size = fontSize
                            this.borderWidth = borderWidth
                        }).setAfterLoad(afterLoadFunc)
            }
        }


//        addFontFamily("OpenSans")
        addFontFamily(familyName = "Roboto", hinting = FreeTypeFontGenerator.Hinting.Slight)

        addFontFamily(fontIDPrefix = "editor_status", familyName = "Roboto", fontSize = 16,
                hinting = FreeTypeFontGenerator.Hinting.Slight, generateBordered = false)

        cache["prmania_icons"] = PaintboxFontBitmap(
                PaintboxFontParams(Gdx.files.internal("fonts/prmania_icons/prmania_icons.fnt"), 16, 0f, false, WindowSize(1280, 720)),
                BitmapFont(Gdx.files.internal("fonts/prmania_icons/prmania_icons.fnt"), Gdx.files.internal("fonts/prmania_icons/prmania_icons.png"), false, true).apply {
                    region.texture.also { tex ->
                        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                    }
                },
                true
        )
        cache["editor_beat_time"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 24
                    borderWidth = 0f
                    padTop = 1
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_beat_track"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 20
                    borderWidth = 2f
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_instantiator"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 24
                    borderWidth = 2f
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_instantiator_summary"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 24
                    borderWidth = 0f
                }
        ).setAfterLoad(defaultAfterLoad)
        addFontFamily(fontIDPrefix = "editor_instantiator_desc", familyName = "Roboto", fontSize = 20,
                hinting = FreeTypeFontGenerator.Hinting.Slight, generateBordered = false,
                afterLoadFunc = { bitmapFont ->
                    defaultAfterLoad.invoke(this, bitmapFont)
                    bitmapFont.data.blankLineScale = 0.3f
                })
        cache["rodin"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = defaultFontSize
                    borderWidth = 0f
                }).setAfterLoad(defaultAfterLoad)
        cache["rodin_BORDERED"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = defaultFontSize
                    borderWidth = 1.5f
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_marker"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 20
                    borderWidth = 1f
                }).setAfterLoad(defaultAfterLoad)
    }


    val mainFont: PaintboxFont get() = fontCache["Roboto"]
    val mainFontBordered: PaintboxFont get() = fontCache["Roboto_BORDERED"]
    val mainFontBold: PaintboxFont get() = fontCache["Roboto_BOLD"]
    val mainFontBoldBordered: PaintboxFont get() = fontCache["Roboto_BOLD_BORDERED"]
    val mainFontItalic: PaintboxFont get() = fontCache["Roboto_ITALIC"]
    val mainFontItalicBordered: PaintboxFont get() = fontCache["Roboto_ITALIC_BORDERED"]
    val mainFontBoldItalic: PaintboxFont get() = fontCache["Roboto_BOLD_ITALIC"]
    val mainFontBoldItalicBordered: PaintboxFont get() = fontCache["Roboto_BOLD_ITALIC_BORDERED"]
    val fontIcons: PaintboxFont get() = fontCache["prmania_icons"]
    val fontEditorBeatTime: PaintboxFont get() = fontCache["editor_beat_time"]
    val fontEditorBeatTrack: PaintboxFont get() = fontCache["editor_beat_track"]
    val fontEditorInstantiatorName: PaintboxFont get() = fontCache["editor_instantiator"]
    val fontEditorInstantiatorSummary: PaintboxFont get() = fontCache["editor_instantiator_summary"]
    val fontRodin: PaintboxFont get() = fontCache["rodin"]
    val fontRodinBordered: PaintboxFont get() = fontCache["rodin_BORDERED"]
    val fontEditorMarker: PaintboxFont get() = fontCache["editor_marker"]

}