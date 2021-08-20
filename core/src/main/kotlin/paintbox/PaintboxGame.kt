package paintbox

import com.badlogic.gdx.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox.StageOutlineMode.ALL
import paintbox.Paintbox.StageOutlineMode.NONE
import paintbox.Paintbox.StageOutlineMode.ONLY_VISIBLE
import paintbox.font.FontCache
import paintbox.font.PaintboxFont
import paintbox.font.PaintboxFontFreeType
import paintbox.font.PaintboxFontParams
import paintbox.i18n.LocalizationBase
import paintbox.logging.SysOutPiper
import paintbox.registry.AssetRegistry
import paintbox.registry.ScreenRegistry
import paintbox.util.MemoryUtils
import paintbox.util.gdxutils.GdxGame
import paintbox.util.gdxutils.isShiftDown
import paintbox.util.Version
import paintbox.util.WindowSize
import paintbox.util.gdxutils.drawCompressed
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
        lateinit var spritesheetTexture: Texture
            private set
        lateinit var paintboxSpritesheet: PaintboxSpritesheet
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
        spritesheetTexture = Texture(Gdx.files.internal("paintbox/paintbox_spritesheet_noborder.png"), true).apply {
            this.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        paintboxSpritesheet = PaintboxSpritesheet(spritesheetTexture)

        AssetRegistry.addAssetLoader(object : AssetRegistry.IAssetLoader {
            override fun addManagedAssets(manager: AssetManager) {}
            override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {}
        })

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        fontCache = FontCache(this)
        addDebugFonts(fontCache)
        val fontLoadNano = measureNanoTime {
            fontCache.resizeAll(emulatedCamera.viewportWidth.toInt(), emulatedCamera.viewportHeight.toInt())
        }
        Paintbox.LOGGER.info("Initialized all ${fontCache.fonts.size} fonts in ${fontLoadNano / 1_000_000.0} ms")

        Gdx.input.inputProcessor = inputMultiplexer.apply {
            addProcessor(0, this@PaintboxGame)
        }
    }
    
    fun resetViewportToScreen() {
        HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
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
        Gdx.gl.glClearDepthf(1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or (if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0))
    }

    /**
     * This is called after the main [render] function is called. Default implementation is to do nothing.
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
            resetViewportToScreen()
            
            preRender()
            super.render()
            postRender()

            resetViewportToScreen()
            debugInfo.update(Gdx.graphics.deltaTime)

            if (Paintbox.debugMode) {
                debugInfo.tmpMatrix.set(batch.projectionMatrix)
                batch.projectionMatrix = nativeCamera.combined
                batch.begin()

                val paintboxFont = debugFontBoldBordered
                val font = paintboxFont.begin()
                val fps = Gdx.graphics.framesPerSecond
                val numberFormat = debugInfo.numberFormat
                val string =
                        """FPS: $fps
Debug mode: ${Paintbox.DEBUG_KEY_NAME} + I - Reload I18N | S(+Shift) - UI outlines: ${Paintbox.stageOutlines} | G - gc
Version: $versionString
Memory: ${numberFormat.format(Gdx.app.nativeHeap / 1024)} / ${numberFormat.format(MemoryUtils.maxMemoryKiB)} KiB (${numberFormat.format(debugInfo.memoryDelta / 1024)} KiB/s)
Screen: ${screen?.javaClass?.canonicalName}
${getDebugString()}
${(screen as? PaintboxScreen)?.getDebugString() ?: ""}"""

                font.setColor(1f, 1f, 1f, 1f)
                font.drawCompressed(batch, string, 8f, nativeCamera.viewportHeight - 8f, nativeCamera.viewportWidth - 16f,
                        Align.left)
                paintboxFont.end()

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
        resetCameras()
        val nano = measureNanoTime {
            val nativeCamWidth = nativeCamera.viewportWidth.toInt()
            val nativeCamHeight = nativeCamera.viewportHeight.toInt()
            fontCache.resizeAll(nativeCamWidth, nativeCamHeight)
        }
//        Paintbox.LOGGER.info("Reloaded all ${fontCache.fonts.size} fonts in ${nano / 1_000_000.0} ms")
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
        spritesheetTexture.dispose()

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
        val resizeAction = paintboxSettings.resizeAction
        val emulatedSize = paintboxSettings.emulatedSize
        val minimumSize = paintboxSettings.minimumSize
        
        nativeCamera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        if (nativeCamera.viewportWidth < minimumSize.width || nativeCamera.viewportHeight < minimumSize.height) {
            nativeCamera.setToOrtho(false, minimumSize.width.toFloat(), minimumSize.height.toFloat())
        }
        nativeCamera.update()
        
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
                    val old = Paintbox.stageOutlines
                    Paintbox.stageOutlines = when (old) {
                        NONE -> if (Gdx.input.isShiftDown()) ALL else ONLY_VISIBLE
                        ALL -> if (Gdx.input.isShiftDown()) ONLY_VISIBLE else NONE
                        ONLY_VISIBLE -> if (Gdx.input.isShiftDown()) ALL else NONE
                    }
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
        fun makeParam() = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            magFilter = Texture.TextureFilter.Linear
            minFilter = Texture.TextureFilter.Linear
            genMipMaps = false
            incremental = true
            mono = false
            color = Color(1f, 1f, 1f, 1f)
            borderColor = Color(0f, 0f, 0f, 1f)
            characters = ""
            hinting = FreeTypeFontGenerator.Hinting.Full
        }

        val afterLoad: PaintboxFontFreeType.(font: BitmapFont) -> Unit = { font ->
//            font.data.blankLineScale = 0.75f
            font.setFixedWidthGlyphs("1234567890")
            font.setUseIntegerPositions(false)
        }
        val defaultFontSize = 16
        val defaultBorderWidth = 1.5f
        val normalFilename = "OpenSans-Regular.ttf"
        val normalItalicFilename = "OpenSans-Italic.ttf"
        val boldFilename = "OpenSans-Bold.ttf"
        val boldItalicFilename = "OpenSans-BoldItalic.ttf"
        cache["DEBUG_FONT"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$normalFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = 0f
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BORDERED"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$normalFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = defaultBorderWidth
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_ITALIC"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$normalItalicFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = 0f
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_ITALIC_BORDERED"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$normalItalicFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = defaultBorderWidth
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$boldFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = 0f
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD_BORDERED"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$boldFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = defaultBorderWidth
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD_ITALIC"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$boldItalicFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = 0f
                }).setAfterLoad(afterLoad)
        cache["DEBUG_FONT_BOLD_ITALIC_BORDERED"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("paintbox/fonts/$boldItalicFilename"), 1, 1f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    size = defaultFontSize
                    borderWidth = defaultBorderWidth
                }).setAfterLoad(afterLoad)
    }
}
