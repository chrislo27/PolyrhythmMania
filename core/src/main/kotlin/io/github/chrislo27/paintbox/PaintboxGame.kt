package io.github.chrislo27.paintbox

import com.badlogic.gdx.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.Paintbox.StageOutlineMode.ALL
import io.github.chrislo27.paintbox.Paintbox.StageOutlineMode.NONE
import io.github.chrislo27.paintbox.Paintbox.StageOutlineMode.ONLY_VISIBLE
import io.github.chrislo27.paintbox.font.FontCache
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.PaintboxFontFreeType
import io.github.chrislo27.paintbox.i18n.LocalizationBase
import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.logging.SysOutPiper
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.registry.ScreenRegistry
import io.github.chrislo27.paintbox.util.MemoryUtils
import io.github.chrislo27.paintbox.util.gdxutils.GdxGame
import io.github.chrislo27.paintbox.util.gdxutils.isShiftDown
import io.github.chrislo27.paintbox.util.Version
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.drawCompressed
import java.io.File
import java.text.NumberFormat
import kotlin.system.measureNanoTime

/**
 * This class is the base of all Paintbox games. [ResizeAction] and its other size parameters are behaviours for how
 * resizing works. This is important for fonts that scale up.
 */
abstract class PaintboxGame(val paintboxSettings: PaintboxSettings)
    : GdxGame(), InputProcessor {

    companion object {
        lateinit var fillTexture: Texture
            private set
        lateinit var gameInstance: PaintboxGame
            private set
        lateinit var launchArguments: List<String>  
            private set
    }
    
    inner class DebugInfo {
        val numberFormat: NumberFormat = NumberFormat.getIntegerInstance()
        var memoryDeltaTime: Float = 0f
        var lastMemory: Long = 0L
        var memoryDelta: Long = 0L
        val tmpMatrix: Matrix4 = Matrix4() // Used for rendering debug text

        fun update(delta: Float) {
            memoryDeltaTime += delta
            if (memoryDeltaTime >= 1f) {
                memoryDeltaTime = 0f
                val heap = Gdx.app.nativeHeap
                memoryDelta = heap - lastMemory
                lastMemory = heap
            }
        }
    }
    
    init {
        @Suppress("RedundantCompanionReference")
        PaintboxGame.Companion.launchArguments = paintboxSettings.launchArguments
    }
    
    val version: Version = paintboxSettings.version
    val versionString: String = version.toString()
    
    val startTimeMillis: Long = System.currentTimeMillis()

    protected val debugInfo: DebugInfo = DebugInfo()
    lateinit var originalResolution: WindowSize
        private set

    /**
     * A camera that represents the emulated size by the [resizeAction].
     */
    val emulatedCamera: OrthographicCamera = OrthographicCamera()

    /**
     * A camera that always represents the actual window size.
     */
    val nativeCamera: OrthographicCamera = OrthographicCamera()

    lateinit var fontCache: FontCache
        private set
    lateinit var batch: SpriteBatch
        private set
    lateinit var shapeRenderer: ShapeRenderer
        private set

    open val inputMultiplexer: InputMultiplexer = ExceptionalInputMultiplexer({ exceptionHandler(it) })

    private var shouldToggleDebugAfterPress = true
    private val disposeCalls: MutableList<Runnable> = mutableListOf()

    /**
     * Set this for default debug localization behaviours.
     */
    protected var localizationInstance: LocalizationBase? = null

    val debugFont: PaintboxFont
        inline get() = fontCache["DEBUG_FONT"]
    val debugFontBordered: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_BORDERED"]
    val debugFontBold: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_BOLD"]
    val debugFontBoldBordered: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_BOLD_BORDERED"]
    val debugFontItalic: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_ITALIC"]
    val debugFontItalicBordered: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_ITALIC_BORDERED"]
    val debugFontBoldItalic: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_BOLD_ITALIC"]
    val debugFontBoldItalicBordered: PaintboxFont
        inline get() = fontCache["DEBUG_FONT_BOLD_ITALIC_BORDERED"]

    /**
     * Should include the version.
     */
    abstract fun getTitle(): String

    var programLaunchArguments: List<String> = emptyList()

    override fun create() {
        val logToFile = paintboxSettings.logToFile
        if (logToFile != null) {
            SysOutPiper.pipe(programLaunchArguments, this, logToFile)
        }
        Paintbox.LOGGER = paintboxSettings.logger
        PaintboxGame.gameInstance = this

        originalResolution = WindowSize(Gdx.graphics.width, Gdx.graphics.height)
        resetCameras()

        val pixmap: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(1f, 1f, 1f, 1f)
            fill()
        }
        fillTexture = Texture(pixmap)
        pixmap.dispose()
        
        AssetRegistry.addAssetLoader(object : AssetRegistry.IAssetLoader {
            override fun addManagedAssets(manager: AssetManager) {}
            override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
                assets["paintbox_spritesheet"] = Texture("paintbox/paintbox_spritesheet.png")
            }
        })

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        fontCache = FontCache(this)
        addDebugFonts(fontCache)
        val fontLoadNano = measureNanoTime {
            fontCache.resizeAll(emulatedCamera.viewportWidth, emulatedCamera.viewportHeight)
        }
        Paintbox.LOGGER.info("Initialized all ${fontCache.fonts.size} fonts in ${fontLoadNano / 1_000_000.0} ms")

        Gdx.input.inputProcessor = inputMultiplexer.apply {
            addProcessor(0, this@PaintboxGame)
        }
    }

    /**
     * This function handles camera updates and screen clearing.
     */
    open fun preRender() {
        emulatedCamera.update()
        batch.projectionMatrix = emulatedCamera.combined
        shapeRenderer.projectionMatrix = emulatedCamera.combined

        (screen as? PaintboxScreen)?.renderUpdate()

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glClearDepthf(1f)
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
    }

    /**
     * This is called after the main [render] function is called.
     */
    open fun postRender() {

    }

    protected open fun onDebugChange(old: Boolean, new: Boolean) {
    }

    /**
     * The default render function. This calls [preRender], then `super.[render]`, then [postRender].
     * The debug overlay is also rendered at this time.
     */
    final override fun render() {
        try {
            preRender()
            super.render()
            postRender()

            debugInfo.update(Gdx.graphics.deltaTime)

            if (Paintbox.debugMode) {
                debugInfo.tmpMatrix.set(batch.projectionMatrix)
                batch.projectionMatrix = nativeCamera.combined
                val font = debugFontBoldBordered.font
                batch.begin()
                val fps = Gdx.graphics.framesPerSecond
                val numberFormat = debugInfo.numberFormat
                val string =
                        """FPS: $fps
Debug mode: ${Paintbox.DEBUG_KEY_NAME}
  Holding ${Paintbox.DEBUG_KEY_NAME}: I - Reload I18N | S - Stage outlines (SHIFT for only visible) | G - gc
Version: $versionString
Memory: ${numberFormat.format(Gdx.app.nativeHeap / 1024)} / ${numberFormat.format(MemoryUtils.maxMemoryKiB)} KiB (${numberFormat.format(debugInfo.memoryDelta / 1024)} KiB/s)
Screen: ${screen?.javaClass?.canonicalName}
${getDebugString()}
${(screen as? PaintboxScreen)?.getDebugString() ?: ""}"""

                font.setColor(1f, 1f, 1f, 1f)
                font.drawCompressed(batch, string, 8f, nativeCamera.viewportHeight - 8f, nativeCamera.viewportWidth - 16f,
                                    Align.left)
                batch.end()
                batch.projectionMatrix = debugInfo.tmpMatrix
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            exceptionHandler(t)
        }
    }

    protected open fun exceptionHandler(t: Throwable) {
        Gdx.app.exit()
    }

    /**
     * This returns a string to be put in the debug overlay above the current screen's debug string. By default this is
     * an empty string.
     */
    open fun getDebugString(): String {
        return ""
    }

    /**
     * This will reset the camera + reload fonts if the resize action is [ResizeAction.KEEP_ASPECT_RATIO]. It then calls
     * the super-method last.
     */
    override fun resize(width: Int, height: Int) {
        val lastCameraDimensions = emulatedCamera.viewportWidth to emulatedCamera.viewportHeight
        resetCameras()
        if (paintboxSettings.resizeAction == ResizeAction.KEEP_ASPECT_RATIO &&
                (emulatedCamera.viewportWidth to emulatedCamera.viewportHeight) != lastCameraDimensions) {
            val nano = measureNanoTime {
                fontCache.resizeAll(emulatedCamera.viewportWidth, emulatedCamera.viewportHeight)
            }
            Paintbox.LOGGER.info("Reloaded all ${fontCache.fonts.size} fonts in ${nano / 1_000_000.0} ms")
        }
        super.resize(width, height)
    }

    override fun setScreen(screen: Screen?) {
        val current = getScreen()
        super.setScreen(screen)
        Paintbox.LOGGER.debug("Changed screens from ${current?.javaClass?.name} to ${screen?.javaClass?.name}")
    }

    override fun dispose() {
        Paintbox.LOGGER.info("Starting dispose call")

        super.dispose()

        disposeCalls.forEach { r ->
            try {
                r.run()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        batch.dispose()
        shapeRenderer.dispose()
        fontCache.dispose()
        fillTexture.dispose()

        ScreenRegistry.dispose()
        AssetRegistry.dispose()

        Paintbox.LOGGER.info("Dispose call finished, goodbye!")
    }

    fun addDisposeCall(runnable: Runnable) {
        disposeCalls += runnable
    }

    fun removeDisposeCall(runnable: Runnable) {
        disposeCalls -= runnable
    }

    fun resetCameras() {
        nativeCamera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        nativeCamera.update()
        val resizeAction = paintboxSettings.resizeAction
        val emulatedSize = paintboxSettings.emulatedSize
        val minimumSize = paintboxSettings.minimumSize
        when (resizeAction) {
            ResizeAction.ANY_SIZE -> emulatedCamera.setToOrtho(false, Gdx.graphics.width.toFloat(),
                                                               Gdx.graphics.height.toFloat())
            ResizeAction.LOCKED -> emulatedCamera.setToOrtho(false, emulatedSize.width.toFloat(),
                                                             emulatedSize.height.toFloat())
            ResizeAction.KEEP_ASPECT_RATIO -> {
                val width: Float
                val height: Float

                if (Gdx.graphics.width < Gdx.graphics.height) {
                    width = Gdx.graphics.width.toFloat()
                    height = (emulatedSize.height.toFloat() / emulatedSize.width) * width
                } else {
                    height = Gdx.graphics.height.toFloat()
                    width = (emulatedSize.width.toFloat() / emulatedSize.height) * height
                }

                emulatedCamera.setToOrtho(false, width, height)
            }
        }
        if (emulatedCamera.viewportWidth < minimumSize.width || emulatedCamera.viewportHeight < minimumSize.height) {
//            Paintbox.LOGGER.debug("Camera too small, forcing it at minimum")
            emulatedCamera.setToOrtho(false, minimumSize.width.toFloat(), minimumSize.height.toFloat())
        }
        emulatedCamera.update()
//        Paintbox.LOGGER.debug("Resizing camera as $resizeAction, window is ${Gdx.graphics.width} x ${Gdx.graphics.height}, camera is ${defaultCamera.viewportWidth} x ${defaultCamera.viewportHeight}")
    }

    override fun keyDown(keycode: Int): Boolean {
        if (Gdx.input.isKeyPressed(Paintbox.DEBUG_KEY)) {
            var pressed = true
            when (keycode) {
                Input.Keys.I -> {
                    val loc = localizationInstance
                    if (loc != null) {
                        val nano = measureNanoTime {
                            loc.reloadAll()
                            loc.logMissingLocalizations()
                        }
                        Paintbox.LOGGER.debug("Reloaded I18N from files in ${nano / 1_000_000.0} ms")
                    } else {
                        Paintbox.LOGGER.debug("Could not reload I18N, localizationInstance was not set")
                    }
                }
                Input.Keys.S -> {
                    Paintbox.stageOutlines = if (Paintbox.stageOutlines == NONE) {
                        if (Gdx.input.isShiftDown()) ONLY_VISIBLE else ALL
                    } else NONE
                    Paintbox.LOGGER.debug("Toggled stage outlines to ${Paintbox.stageOutlines}")
                }
                Input.Keys.G -> System.gc()
                else -> {
                    pressed = false
                }
            }
            if (shouldToggleDebugAfterPress && pressed) {
                shouldToggleDebugAfterPress = false
            }
            if (pressed) {
                return true
            }
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == Paintbox.DEBUG_KEY) {
            val shouldToggle = shouldToggleDebugAfterPress
            shouldToggleDebugAfterPress = true
            if (shouldToggle) {
                val old = Paintbox.debugMode
                Paintbox.debugMode = !old
                onDebugChange(old, !old)
                Paintbox.LOGGER.debug("Switched debug mode to ${!old}")
                return true
            }
        }
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }
    
    private fun addDebugFonts(cache: FontCache) {
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
            font.setFixedWidthGlyphs("1234567890")
            font.setUseIntegerPositions(false)
        }
        cache["DEBUG_FONT"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-Regular.ttf"), emulatedSize,
                                                   makeParam().apply {
                                                       size = 18
                                                       borderWidth = 0f
                                                   }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-Regular.ttf"), emulatedSize,
                                                            makeParam().apply {
                                                                size = 18
                                                                borderWidth = 1.5f
                                                            }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_ITALIC"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-Italic.ttf"), emulatedSize,
                                                          makeParam().apply {
                                                              size = 18
                                                              borderWidth = 0f
                                                          }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_ITALIC_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-Italic.ttf"), emulatedSize,
                                                                   makeParam().apply {
                                                                       size = 18
                                                                       borderWidth = 1.5f
                                                                   }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-Bold.ttf"), emulatedSize,
                                                        makeParam().apply {
                                                            size = 18
                                                            borderWidth = 0f
                                                        }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-Bold.ttf"), emulatedSize,
                                                                 makeParam().apply {
                                                                     size = 18
                                                                     borderWidth = 1.5f
                                                                 }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD_ITALIC"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-BoldItalic.ttf"), emulatedSize,
                                                               makeParam().apply {
                                                                   size = 18
                                                                   borderWidth = 0f
                                                               }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD_ITALIC_BORDERED"] = PaintboxFontFreeType(Gdx.files.internal("paintbox/fonts/OpenSans-BoldItalic.ttf"), emulatedSize,
                                                                        makeParam().apply {
                                                                            size = 18
                                                                            borderWidth = 1.5f
                                                                        }, PaintboxFont.LoadPriority.LAZY).setAfterLoad(afterLoad)
    }
}
