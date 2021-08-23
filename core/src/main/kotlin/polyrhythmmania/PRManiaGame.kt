package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.eclipsesource.json.Json
import org.lwjgl.glfw.GLFW
import paintbox.*
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.*
import paintbox.logging.Logger
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.util.ResolutionSetting
import paintbox.util.Version
import paintbox.util.WindowSize
import paintbox.util.gdxutils.*
import polyrhythmmania.container.Container
import polyrhythmmania.discordrpc.DiscordHelper
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.engine.input.InputThresholds
import polyrhythmmania.init.AssetRegistryLoadingScreen
import polyrhythmmania.init.InitialAssetLoader
import polyrhythmmania.init.TilesetAssetLoader
import polyrhythmmania.screen.CrashScreen
import polyrhythmmania.screen.mainmenu.MainMenuScreen
import polyrhythmmania.sidemodes.SidemodeAssets
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.util.DumpPackedSheets
import polyrhythmmania.util.LelandSpecialChars
import polyrhythmmania.util.TempFileUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


class PRManiaGame(paintboxSettings: PaintboxSettings)
    : PaintboxGame(paintboxSettings) {

    companion object {
        lateinit var instance: PRManiaGame
            private set
        
        fun createPaintboxSettings(launchArguments: List<String>, logger: Logger, logToFile: File?): PaintboxSettings =
                PaintboxSettings(launchArguments, logger, logToFile, PRMania.VERSION, PRMania.DEFAULT_SIZE,
                        ResizeAction.ANY_SIZE, PRMania.MINIMUM_SIZE)
    }

    private var lastWindowed: WindowSize = PRMania.DEFAULT_SIZE.copy()
    @Volatile
    var blockResolutionChanges: Boolean = false
    
    lateinit var preferences: Preferences
        private set
    lateinit var settings: Settings
        private set
    
    // For colour picker
    lateinit var colourPickerHueBar: Texture
        private set
    lateinit var colourPickerTransparencyGrid: Texture
        private set

    // Permanent screens
    lateinit var mainMenuScreen: MainMenuScreen
        private set
    private val permanentScreens: MutableList<PaintboxScreen> = mutableListOf()
    
    val githubVersion: ReadOnlyVar<Version> = Var(Version.ZERO)

    override fun getTitle(): String = "${PRMania.TITLE} ${PRMania.VERSION}"

    override fun create() {
        super.create()
        PRManiaGame.instance = this
        this.localizationInstance = Localization
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        GLFW.glfwSetWindowAspectRatio(windowHandle, 16, 9)
//        GLFW.glfwSetWindowAspectRatio(windowHandle, 3, 2)

        preferences = Gdx.app.getPreferences("PolyrhythmMania")

        addFontsToCache(this.fontCache)
        PRManiaColors
        PRManiaSkins
        settings = Settings(this, preferences).apply { 
            load()
            val mixerHandler = SoundSystem.defaultMixerHandler
            val mixerString = this.mixer.getOrCompute()
            if (mixerString.isNotEmpty()) {
                val found = mixerHandler.supportedMixers.find {
                    it.mixerInfo.name == mixerString
                }
                if (found != null) {
                    Paintbox.LOGGER.info("Attaching to mixer from settings: ${found.mixerInfo.name}")
                    mixerHandler.recommendedMixer = found
                } else {
                    Paintbox.LOGGER.warn("Could not find mixer from settings: settings = $mixerString")
                }
            } else {
                val mixerName = mixerHandler.recommendedMixer.mixerInfo.name
                this.mixer.set(mixerName)
                Paintbox.LOGGER.info("No saved mixer string, using $mixerName")
            }
        }

        AssetRegistry.addAssetLoader(InitialAssetLoader())
        AssetRegistry.addAssetLoader(TilesetAssetLoader())

        if (settings.fullscreen.getOrCompute()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        } else {
            val res = settings.windowedResolution.getOrCompute()
            if (Gdx.graphics.width != res.width || Gdx.graphics.height != res.height) {
                Gdx.graphics.setWindowedMode(res.width, res.height)
            }
        }

        generateColourPickerTextures()
        
        fun initializeScreens() {
            mainMenuScreen = MainMenuScreen(this)
            permanentScreens.add(mainMenuScreen)
        }
        setScreen(AssetRegistryLoadingScreen(this).apply {
            onStart = {
                InputThresholds.initInputClasses()
            }
            onAssetLoadingComplete = {
                DiscordHelper.init(settings.discordRichPresence.getOrCompute())
                
                initializeScreens()
                
                if (PRMania.dumpPackedSheets) {
                    val gdxArray = com.badlogic.gdx.utils.Array<PackedSheet>()
                    AssetRegistry.manager.getAll(PackedSheet::class.java, gdxArray)
                    DumpPackedSheets.dump(gdxArray.toList())
                }
            }
            nextScreenProducer = {
//                polyrhythmmania.world.render.TestWorldRenderScreen(this@PRManiaGame)
//                EditorScreen(this@PRManiaGame, debugMode = true)
//                polyrhythmmania.world.render.TestWorldDunkScreen(this@PRManiaGame)
                mainMenuScreen.prepareShow(doFlipAnimation = true)
            }
        })
        
        if (PRMania.logMissingLocalizations) {
            Localization.logMissingLocalizations()
        }
        
        Runtime.getRuntime().addShutdownHook(thread(start = false, isDaemon = true, name = "Level Recovery Shutdown Hook") {
            val currentScreen = getScreen()
            if (currentScreen is EditorScreen) {
                val editor = currentScreen.editor
                val recoveryFile = editor.getRecoveryFile(overwrite = false)
                val container = editor.container
                try {
                    container.writeToFile(recoveryFile)
                    Paintbox.LOGGER.info("Shutdown hook recovery completed (filename: ${recoveryFile.name})")
                } catch (e: Exception) {
                    Paintbox.LOGGER.warn("Shutdown hook recovery failed! filename: ${recoveryFile.name}")
                    e.printStackTrace()
                }
            }
        })
        thread(isDaemon = true, name = "GitHub version checker", start = true) {
            try {
                val apiUrl = URL("https://api.github.com/repos/${PRMania.GITHUB.substringAfter("https://github.com/", "")}/releases/latest")
                val con = apiUrl.openConnection() as HttpURLConnection
                con.requestMethod = "GET"
                val status = con.responseCode
                if (status == 200) {
                    val content = con.inputStream.bufferedReader().let {
                        val text = it.readText()
                        it.close()
                        text
                    }
                    val parsed = Version.parse(Json.parse(content).asObject().getString("tag_name", ""))
                    if (parsed != null) {
                        Paintbox.LOGGER.info("Got version from server: $parsed")
                        Gdx.app.postRunnable {
                            (githubVersion as Var).set(parsed)
                        }
                    }
                } else {
                    Paintbox.LOGGER.warn("Failed to get version from server: status was $status for url $apiUrl")
                }
                con.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun dispose() {
        super.dispose()
        preferences.putString(PreferenceKeys.LAST_VERSION, PRMania.VERSION.toString()).flush()
        colourPickerHueBar.disposeQuietly()
        colourPickerTransparencyGrid.disposeQuietly()
        SidemodeAssets.disposeQuietly()
        permanentScreens.forEach { s ->
            s.disposeQuietly()
        }
        (screen as? Disposable)?.disposeQuietly()
        try {
            val expiry = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            PRMania.RECOVERY_FOLDER.listFiles()?.filter { f ->
                f != null && f.isFile && f.extension == Container.FILE_EXTENSION && f.lastModified() < expiry
            }?.forEach { 
                it.delete()
                Paintbox.LOGGER.info("Deleted old recovery file ${it.name}, lastModified() = ${it.lastModified()}, limit=$expiry")
            }
            TempFileUtils.clearTempFolder()
        } catch (s: SecurityException) {
            s.printStackTrace()
        }
    }

    override fun postRender() {
        val batch = this.batch

        val cam = nativeCamera
        batch.projectionMatrix = cam.combined
        batch.begin()
        
        batch.setColor(1f, 1f, 1f, 1f)

        @Suppress("ConstantConditionIf")
        if (PRMania.enableEarlyAccessMessage) {
            val paintboxFont = fontRodinFixedBordered
            paintboxFont.useFont { font ->
                val isEditor = this.getScreen() is EditorScreen
                val height = if (!isEditor) (cam.viewportHeight - 10f) else (48f)
                val alpha = if (cam.getInputY() in (height - font.capHeight)..(height) || isEditor) 0.2f else 1f
                font.setColor(1f, 1f, 1f, alpha)
                font.drawCompressed(batch, "Pre-release version ${PRMania.VERSION}. Content subject to change. Do not share screenshots or videos without express permission; do not redistribute.",
                        2f,
                        height,
                        cam.viewportWidth - 4f, Align.center)
                font.setColor(1f, 1f, 1f, 1f)
            }
        }
        
        batch.end()
        
        super.postRender()
    }

    private val userHomeFile: File = File(System.getProperty("user.home"))
    private val desktopFile: File = userHomeFile.resolve("Desktop")

    fun persistDirectory(prefName: String, file: File) {
        preferences.putString(prefName, file.absolutePath)
        preferences.flush()
    }

    fun attemptRememberDirectory(prefName: String): File? {
        val f: File = File(preferences.getString(prefName, null) ?: return null)
        if (f.exists() && f.isDirectory)
            return f
        return null
    }

    fun getDefaultDirectory(): File = if (!desktopFile.exists() || !desktopFile.isDirectory) userHomeFile else desktopFile

    /**
     * Blocks resolution changes and, if in fullscreen mode, resets to windowed mode.
     * After the [func] block is complete, [func] should call the completionCallback function (from any thread).
     * That will reset the resolution change block flag to its original value
     * and also goes back to fullscreen mode if needed.
     *
     * This function should only be called from the GL thread (use `Gdx.app.postRunnable` if necessary).
     */
    fun restoreForExternalDialog(func: (completionCallback: () -> Unit) -> Unit) {
        val originalResBlock = this.blockResolutionChanges
        val originalResolution = ResolutionSetting(Gdx.graphics.width, Gdx.graphics.height, Gdx.graphics.isFullscreen)
        if (originalResolution.fullscreen) {
            Gdx.graphics.setWindowedMode(PRMania.DEFAULT_SIZE.width, PRMania.DEFAULT_SIZE.height)
        }

        val callback: () -> Unit = {
            Gdx.app.postRunnable {
                if (originalResolution.fullscreen) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                }
                this.blockResolutionChanges = originalResBlock
            }
        }
        func.invoke(callback)
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

    override fun exceptionHandler(t: Throwable) {
        val currentScreen = this.screen
        if (currentScreen !is CrashScreen) {
            if (currentScreen is EditorScreen) {
                val editor = currentScreen.editor
                val recoveryFile = editor.getRecoveryFile(overwrite = false, midfix = "crash")
                val container = editor.container
                try {
                    container.writeToFile(recoveryFile)
                    Paintbox.LOGGER.info("Crash recovery completed (filename: ${recoveryFile.name})")
                } catch (e: Exception) {
                    Paintbox.LOGGER.warn("Crash recovery failed! filename: ${recoveryFile.name}")
                    e.printStackTrace()
                }
            }
            setScreen(CrashScreen(this, t, currentScreen))
        } else {
            super.exceptionHandler(t)
            Gdx.app.exit()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        val res = super.keyDown(keycode)
        if (!res) {
            if (!Gdx.input.isControlDown() && !Gdx.input.isAltDown()) {
                if (keycode == Input.Keys.F11 && !blockResolutionChanges) {
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
    
    fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float): Long {
        return sound.play(volume * (settings.menuSfxVolume.getOrCompute() / 100f), pitch, pan)
    }
    fun playMenuSfx(sound: Sound, volume: Float = 1f): Long = playMenuSfx(sound, volume, 1f, 0f)

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
        val defaultScaledFontAfterLoad: PaintboxFontFreeType.(font: BitmapFont) -> Unit = { font ->
            font.setUseIntegerPositions(false) // Stops glyphs from being offset due to rounding
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
        // Effectively the same as "editor_instantiator_summary"
//        cache["editor_beat_time"] = PaintboxFontFreeType(
//                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 1f, false, WindowSize(1280, 720)),
//                makeParam().apply {
//                    hinting = FreeTypeFontGenerator.Hinting.Slight
//                    size = 24
//                    borderWidth = 0f
//                }
//        ).setAfterLoad(defaultAfterLoad)
        cache["editor_beat_track"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 2f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 20
                    borderWidth = 2f
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_music_score"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Leland/Leland.otf"), 1, 2f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 30
                    borderWidth = 2f
                    val chars = LelandSpecialChars.intToString(12340) + LelandSpecialChars.intToString(56789)
                    characters = chars
                    spaceX = -8
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_instantiator"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 2f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 24
                    borderWidth = 2f
                }).setAfterLoad(defaultAfterLoad)
        cache["editor_instantiator_summary"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 0f, false, WindowSize(1280, 720)),
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
//        addFontFamily(fontIDPrefix = "editor_help", familyName = "Roboto", fontSize = 20,
//                hinting = FreeTypeFontGenerator.Hinting.Slight, generateBordered = false,)
        cache["rodin_fixed"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 1, 0f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = defaultFontSize
                    borderWidth = 0f
                }).setAfterLoad(defaultAfterLoad)
        cache["rodin_fixed_BORDERED"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 1, 1.5f, false, WindowSize(1280, 720)),
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
        cache["editor_dialog_title"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 1, 0f, false, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 64
                    borderWidth = 0f
                }).setAfterLoad(defaultAfterLoad)
        cache["mainmenu_main"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Medium.ttf"), 22, 0f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 22
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["mainmenu_thin"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Regular.ttf"), 22, 0f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 22
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["mainmenu_heading"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/Roboto/Roboto-Bold.ttf"), 40, 0f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 40
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["mainmenu_rodin"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 22, 0f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 22
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["pausemenu_title"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/kurokane/kurokanestd.otf"), 100, 10f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 100
                    borderWidth = 10f
                    spaceX = -8
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["game_textbox"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 42, 0f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 42
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["game_more_times"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 60, 4f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 60
                    borderWidth = 4f
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["game_ui_text"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 40, 3f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 40
                    borderWidth = 3f
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["game_practice_clear"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/kurokane/kurokanestd.otf"), 72, 6f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 72
                    borderWidth = 6f
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["results_main"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/rodin/rodin_lat_cy_ja_ko_spec.ttf"), 32, 0f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 32
                }).setAfterLoad(defaultScaledFontAfterLoad)
        cache["results_score"] = PaintboxFontFreeType(
                PaintboxFontParams(Gdx.files.internal("fonts/kurokane/kurokanestd.otf"), 72, 6f, true, WindowSize(1280, 720)),
                makeParam().apply {
                    hinting = FreeTypeFontGenerator.Hinting.Slight
                    size = 72
                    borderWidth = 6f
                    borderColor = Color().grey(0.4f, 1f)
                }).setAfterLoad { font ->
            defaultScaledFontAfterLoad.invoke(this, font)
            font.setFixedWidthGlyphs("0123456789")
        }
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
    val fontEditorBeatTime: PaintboxFont get() = fontCache["editor_instantiator_summary"] // fontCache["editor_beat_time"]
    val fontEditorBeatTrack: PaintboxFont get() = fontCache["editor_beat_track"]
    val fontEditorMusicScore: PaintboxFont get() = fontCache["editor_music_score"]
    val fontEditorInstantiatorName: PaintboxFont get() = fontCache["editor_instantiator"]
    val fontEditorInstantiatorSummary: PaintboxFont get() = fontCache["editor_instantiator_summary"]
    val fontRodinFixed: PaintboxFont get() = fontCache["rodin_fixed"]
    val fontRodinFixedBordered: PaintboxFont get() = fontCache["rodin_fixed_BORDERED"]
    val fontEditorMarker: PaintboxFont get() = fontCache["editor_marker"]
    val fontEditorDialogTitle: PaintboxFont get() = fontCache["editor_dialog_title"]
    val fontMainMenuMain: PaintboxFont get() = fontCache["mainmenu_main"]
    val fontMainMenuThin: PaintboxFont get() = fontCache["mainmenu_thin"]
    val fontMainMenuHeading: PaintboxFont get() = fontCache["mainmenu_heading"]
    val fontMainMenuRodin: PaintboxFont get() = fontCache["mainmenu_rodin"]
    val fontPauseMenuTitle: PaintboxFont get() = fontCache["pausemenu_title"]
    val fontGameTextbox: PaintboxFont get() = fontCache["game_textbox"]
    val fontGameMoreTimes: PaintboxFont get() = fontCache["game_more_times"]
    val fontGameUIText: PaintboxFont get() = fontCache["game_ui_text"]
    val fontGamePracticeClear: PaintboxFont get() = fontCache["game_practice_clear"]
    val fontResultsMain: PaintboxFont get() = fontCache["results_main"]
    val fontResultsScore: PaintboxFont get() = fontCache["results_score"]

    private fun generateColourPickerTextures() {
        colourPickerHueBar = run {
            val pixmap = Pixmap(360, 1, Pixmap.Format.RGBA8888)
            val tmpColor = Color(1f, 1f, 1f, 1f)
            for (i in 0 until 360) {
                tmpColor.fromHsv(i.toFloat(), 1f, 1f)
                pixmap.setColor(tmpColor)
                pixmap.drawPixel(i, 0)
            }
            Texture(pixmap).apply {
                this.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
        colourPickerTransparencyGrid = run {
            val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
            pixmap.setColor(0.5f, 0.5f, 0.5f, 1f)
            pixmap.fillRectangle(0, 0, 8, 8)
            pixmap.fillRectangle(8, 8, 8, 8)
            pixmap.setColor(1f, 1f, 1f, 1f)
            pixmap.fillRectangle(8, 0, 8, 8)
            pixmap.fillRectangle(0, 8, 8, 8)
            Texture(pixmap).apply {
                this.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                this.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
            }
        }
    }
}