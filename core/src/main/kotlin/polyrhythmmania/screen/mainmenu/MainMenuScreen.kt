package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StreamUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import net.beadsproject.beads.ugens.CrossFade
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.Paintbox
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.binding.VarChangedListener
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabelSkin
import paintbox.ui.layout.VBox
import paintbox.util.MathHelper
import paintbox.util.Version
import paintbox.util.WindowSize
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.grey
import paintbox.util.settableLazy
import paintbox.util.viewport.ExtendFixedRatioViewport
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.screen.mainmenu.bg.MainMenuBg
import polyrhythmmania.screen.mainmenu.menu.*
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.beads.ugen.Bandpass
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.MusicSample
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt


class MainMenuScreen(main: PRManiaGame) : PRManiaScreen(main) {

    companion object {
        const val FLIP_SEC_PER_TILE: Float = 1f / 15f / 2
    }

    private class Tile(var x: Int, var y: Int, var flipAmt: Float = 0f) {
        fun reset() {
            flipAmt = 0f
        }
    }

    data class TileFlip(
            val startX: Int, val startY: Int, val width: Int, val height: Int,
            var cornerStart: Corner = Corner.TOP_LEFT,
            var flipWidth: Float = 3f,
    ) {
        var diagonalProgress: Float = 0f

        var isDone: Boolean = false
            private set

        fun update(delta: Float, mainMenu: MainMenuScreen) {
            if (isDone) return
            if (!isDone && !mainMenu.flipAnimationEnabled.getOrCompute()) {
                isDone = true
                return
            }

            val progressDelta = delta / FLIP_SEC_PER_TILE

            diagonalProgress += progressDelta

            var anyNotDone = false
            for (ix in startX until (startX + width)) {
                if (ix !in 0 until mainMenu.tilesWidth) continue
                for (iy in startY until (startY + height)) {
                    if (iy !in 0 until mainMenu.tilesHeight) continue
                    val tile = mainMenu.tiles[ix][iy]
                    if (tile.flipAmt >= 1f) continue
                    val thisDiag = computeDiagonalIndex(ix, iy)
                    val newAmt = ((diagonalProgress - thisDiag) / flipWidth).coerceIn(0f, 1f)
                    tile.flipAmt = newAmt
                    if (!anyNotDone && newAmt < 1f)
                        anyNotDone = true
                }
            }

            if (!anyNotDone) {
                isDone = true
            }
        }

        fun computeDiagonalIndex(ix: Int, iy: Int): Int {
            return when (cornerStart) {
                Corner.TOP_LEFT -> (ix - startX) + (iy - startY)
                Corner.TOP_RIGHT -> ((width - 1) - (ix - startX)) + (iy - startY)
                Corner.BOTTOM_LEFT -> (ix - startX) + ((height - 1) - (iy - startY))
                Corner.BOTTOM_RIGHT -> ((width - 1) - (ix - startX)) + ((height - 1) - (iy - startY))
            }
        }
    }

    private val lastProjMatrix: Matrix4 = Matrix4()
    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)

    /**
     * Used for frame buffer
     */
    private val fullCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val fullViewport: Viewport = ExtendFixedRatioViewport(1280f, 720f, fullCamera)

    val pendingKeyboardBinding: Var<InputSettingsMenu.PendingKeyboardBinding?> = Var(null)

    private val batch: SpriteBatch = main.batch
    private val sceneRoot: SceneRoot = SceneRoot(uiViewport).apply { 
        this.applyViewport.set(false) // Must be disabled due to being rendered to a framebuffer
    }
    private val processor: InputProcessor = sceneRoot.inputSystem
    
    var backgroundType: BgType = BgType.NORMAL
    private val background: MainMenuBg = MainMenuBg(this)

    private val logoImage: ImageNode
    private val menuPane: Pane = Pane()
    val menuCollection: MenuCollection = MenuCollection(this, sceneRoot, menuPane)
    private val newVersionFloaterAnimation: FloatVar = FloatVar(0f)

    // Related to tile flip effect --------------------------------------------------------

    private val flipAnimationEnabled: ReadOnlyVar<Boolean> = main.settings.mainMenuFlipAnimation
    val tileSize: Int = 48
    val tilesWidth: Int = ceil(1280f / tileSize).toInt()
    val tilesHeight: Int = ceil(720f / tileSize).toInt()
    private val tiles: Array<Array<Tile>> = Array(tilesWidth) { x -> Array(tilesHeight) { y -> Tile(x, y) } }
    var flipAnimation: TileFlip? = null
        private set
    private var framebufferSwapRequested: Boolean = false
    private var transitionAway: (() -> Unit)? = null
    private var framebufferSize: WindowSize = WindowSize(0, 0)

    /**
     * The old framebuffer should be the last rendered frame.
     */
    private lateinit var framebufferOld: FrameBuffer

    /**
     * The current framebuffer is what's drawn for this frame.
     */
    private lateinit var framebufferCurrent: FrameBuffer

    // Music related ----------------------------------------------------------------------------------------------
    private val menuMusicVolume: FloatVar = FloatVar { 
        main.settings.menuMusicVolume.use() / 100f
    }
    private val musicSample: MusicSample
    private val beadsMusic: BeadsMusic
    private var shouldBeBandpass: Boolean = false
    var soundSys: SoundSys by settableLazy {
        SoundSys().apply {
            musicPlayer.pause(true)
        }
    }
    private val enoughMusicLoaded: AtomicBoolean = AtomicBoolean(false)
    private val musicFinishedLoading: AtomicBoolean = AtomicBoolean(false)
    private val firstShowing: AtomicBoolean = AtomicBoolean(true)

    init {
        val (sample, handler) = GdxAudioReader.newDecodingMusicSample(Gdx.files.internal("music/Title_ABC.ogg"),
                object : GdxAudioReader.AudioLoadListener {
            override fun progress(bytesReadSoFar: Long, bytesReadThisChunk: Int) {
                if (bytesReadSoFar > 100_000L && !enoughMusicLoaded.get()) {
                    enoughMusicLoaded.set(true)
                    Gdx.app.postRunnable {
                        val ss = this@MainMenuScreen.soundSys
                        ss.resetMusic()
                        ss.musicPlayer.pause(false)
                    }
                }
            }

            override fun onFinished(totalBytesRead: Long) {
                musicFinishedLoading.set(true)
            }
        })
        musicSample = sample
        musicSample.metricsPopulateBuffer = PRMania.metrics.timer("mainMenu.musicSample.populateBuffer")
        thread(start = true, isDaemon = true, name = "Main Menu music decoder", priority = 8) {
            Paintbox.LOGGER.debug("Starting main menu music decode")
            handler.decode()
            Paintbox.LOGGER.debug("Finished main menu music decode")
        }
        beadsMusic = BeadsMusic(musicSample)
    }

    init {
        val startingWidth = Gdx.graphics.width
        val startingHeight = Gdx.graphics.height
        if (startingWidth > 0 && startingHeight > 0) {
            createFramebuffers(Gdx.graphics.width, Gdx.graphics.height, null)
        } else {
            createFramebuffers(PRMania.WIDTH, PRMania.HEIGHT, null)
        }
    }

    init {
        val markup = Markup(mapOf(), TextRun(main.fontMainMenuMain, ""), Markup.FontStyles("bold", "italic", "bolditalic"))
        val leftPane = Pane().apply {
            this.margin.set(Insets(64f))
        }
        logoImage = ImageNode(TextureRegion(AssetRegistry.get<Texture>("logo_2lines_en"))).apply {
            this.bounds.height.set(175f)
            this.bounds.y.set(24f)
            this.renderAlign.set(Align.topLeft)
            this.visible.bind { (menuCollection.activeMenu.use() as? MMMenu)?.showLogo?.use() != false }
        }
        leftPane.addChild(logoImage)
        menuPane.apply {
            Anchor.BottomLeft.configure(this)
//            this.bindHeightToParent(-(logoImage.bounds.height.get() + logoImage.bounds.y.get() + 32f))
        }
        leftPane.addChild(menuPane)

        sceneRoot += leftPane
        val bottomRight = VBox().apply {
            Anchor.BottomRight.configure(this, offsetY = -32f)
            this.spacing.set(0f)
            this.align.set(VBox.Align.BOTTOM)
            this.bounds.width.set(64f)
            this.bounds.height.set(64f * 2)
        }
        bottomRight.temporarilyDisableLayouts {
            bottomRight += Button("").apply {
                Anchor.BottomRight.configure(this)
                this.bounds.width.set(48f)
                this.bounds.height.set(48f)
                this.skinID.set(UppermostMenu.BUTTON_SKIN_ID)
                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("support_donate")))
                this.setOnAction {
                    Gdx.net.openURI(PRMania.DONATE_LINK)
                }
                val loc: ReadOnlyVar<String> = Localization.getVar("mainMenu.support.tooltip", Var { listOf(PRMania.DONATE_LINK) })
                this.tooltipElement.set(Tooltip(binding = { loc.use() }, font = main.fontMainMenuMain).apply { 
                    this.markup.set(markup)
                })
            }
            bottomRight += Button("").apply {
                Anchor.BottomRight.configure(this)
                this.bounds.width.set(48f)
                this.bounds.height.set(48f)
                this.skinID.set(UppermostMenu.BUTTON_SKIN_ID)
                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("github_mark")))
                this.setOnAction {
                    Gdx.net.openURI(PRMania.GITHUB)
                }
                val loc: ReadOnlyVar<String> = Localization.getVar("mainMenu.github.tooltip", Var { listOf(PRMania.GITHUB) })
                this.tooltipElement.set(Tooltip(binding = { loc.use() }, font = main.fontMainMenuMain).apply { 
                    this.markup.set(markup)
                })
            }
        }
        sceneRoot += bottomRight
        val newVersionAvailable: ReadOnlyVar<Boolean> = Var {
            val github = main.githubVersion.use()
            github != Version.ZERO && github > PRMania.VERSION
        }
        val versionTooltip = Tooltip(binding = {
            if (PRMania.portableMode) Localization.getVar("mainMenu.portableModeVersion", Var {
                listOf(PRMania.VERSION.toString())
            }).use() else PRMania.VERSION.toString()
        }, font = main.fontMainMenuMain).apply {
            Anchor.BottomRight.configure(this)
            resizeBoundsToContent()
            this.bounds.height.set(32f)
            this.renderAlign.set(Align.bottomRight)
            this.textAlign.set(TextAlign.RIGHT)
            this.renderBackground.set(true)
            this.bgPadding.set(Insets(8f))
            this.textColor.bind {
                if (newVersionAvailable.use()) Color.ORANGE.cpy() else Color(1f, 1f, 1f, 1f)
            }
            (this.skin.getOrCompute() as TextLabelSkin).defaultBgColor.set(Color().grey(0.1f, 0.5f))
            this.tooltipElement.set(Tooltip(binding = {
                val t = "${PRMania.TITLE} ${PRMania.VERSION}"
                if (newVersionAvailable.use()) {
                    "${Localization.getVar("mainMenu.newVersion", Var { listOf(main.githubVersion.use()) })}"
                } else t
            }, font = main.fontMainMenuMain).apply {
                this.markup.set(markup)
            })
            this.setOnAction { 
                Gdx.net.openURI("${PRMania.GITHUB}/releases/latest")
            }
        }
        sceneRoot += versionTooltip
        sceneRoot += Tooltip(binding = { Localization.getVar("mainMenu.newVersionFloater").use() }, font = main.fontMainMenuMain).apply {
            Anchor.BottomRight.configure(this)
            val leftSide = FloatVar {
                versionTooltip.bounds.x.useF() - bounds.width.useF()
            }
            this.bounds.x.bind { 
                leftSide.useF() - (newVersionFloaterAnimation.useF() * 20f)
            }
            resizeBoundsToContent()
            this.bounds.height.set(32f)
            this.renderAlign.set(Align.bottomRight)
            this.textAlign.set(TextAlign.RIGHT)
            this.renderBackground.set(true)
            this.bgPadding.set(Insets(8f))
            this.visible.bind { 
                newVersionAvailable.use()
            }
            (this.skin.getOrCompute() as TextLabelSkin).defaultBgColor.set(Color().grey(0.1f, 0.5f))
        }
    }

    init {
        soundSys.start()

        menuMusicVolume.addListener {
            this.soundSys.soundSystem.audioContext.out.gain = it.getOrCompute()
        }
        
        val visibleListener: VarChangedListener<Boolean> = VarChangedListener { v ->
            if (v.getOrCompute()) {
                soundSys.fadeToNormal()
            } else {
                soundSys.fadeToBandpass()
            }
        }
        menuCollection.uppermostMenu.visible.addListener(visibleListener)
        menuCollection.audioSettingsMenu.visible.addListener(visibleListener)
        
        if (PRMania.possiblyNewPortableMode) {
            val m = PortableModeWarningMenu(menuCollection)
            menuCollection.addMenu(m)
            menuCollection.pushNextMenu(m, playSound = false)
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val currentTooltip: UIElement? = sceneRoot.currentTooltipVar.getOrCompute()
        currentTooltip?.visible?.set(this.flipAnimation == null)
        
        if (framebufferSwapRequested) {
            framebufferSwapRequested = false
            val tmpBuffer = framebufferOld
            framebufferOld = framebufferCurrent
            framebufferCurrent = tmpBuffer
        }

        // Draw active scene
        val boundFB: FrameBuffer = framebufferCurrent
        val camera = uiCamera
        boundFB.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Render background
        HdpiUtils.glViewport(0, 0, boundFB.width, boundFB.height)
        background.render(batch, camera)

        // Render UI
        batch.projectionMatrix = camera.combined
        batch.begin()

        HdpiUtils.glViewport(0, 0, boundFB.width, boundFB.height)
        sceneRoot.renderAsRoot(batch)

        if (this.transitionAway != null) {
            batch.setColor(0f, 0f, 0f, 1f)
            batch.fillRect(0f, 0f, camera.viewportWidth, camera.viewportHeight)
            batch.setColor(1f, 1f, 1f, 1f)
        }

        batch.end()
        boundFB.end()

        // Tile flip effect
        fullViewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(framebufferCurrent.colorBufferTexture, 0f, 0f, camera.viewportWidth, camera.viewportHeight, 0f, 0f, 1f, 1f)

        val currentFlip = this.flipAnimation
        if (currentFlip != null) {
            currentFlip.update(delta, this)
            if (currentFlip.isDone) {
                this.flipAnimation = null
                val transitionAway = this.transitionAway
                if (transitionAway != null) {
                    this.transitionAway = null
                    transitionAway.invoke()
                } else {
                    resetTiles()
                }
            } else {
                val tileSizeF = tileSize.toFloat()
                val tileSizeU = tileSizeF / camera.viewportWidth
                val tileSizeV = tileSizeF / camera.viewportHeight

                for (tx in currentFlip.startX until (currentFlip.startX + currentFlip.width)) {
                    if (tx !in 0 until tilesWidth) continue
                    for (ty in currentFlip.startY until (currentFlip.startY + currentFlip.height)) {
                        if (ty !in 0 until tilesHeight) continue
                        val rx: Float = tx * tileSizeF
                        val ry: Float = camera.viewportHeight - ((ty + 1) * tileSizeF)
                        val tile = tiles[tx][ty]
                        val flipAmt = tile.flipAmt

                        val oldAmt = (flipAmt / 0.5f).coerceIn(0f, 1f)
                        val oldAmtInv = 1f - oldAmt
                        val newAmt = (flipAmt / 0.5f - 1f).coerceIn(0f, 1f)

                        batch.setColor(0f, 0f, 0f, 1f)
                        batch.fillRect(rx, ry, tileSizeF, tileSizeF)
                        batch.setColor(1f, 1f, 1f, 1f)

                        if (flipAmt <= 0.5f) {
                            batch.draw(framebufferOld.colorBufferTexture,
                                    rx + (tileSizeF - (tileSizeF * oldAmtInv)) * 0.5f, ry,
                                    tileSizeF * oldAmtInv, tileSizeF,
                                    tx * tileSizeU, 1f - (ty + 1) * tileSizeV, (tx + 1) * tileSizeU, 1f - (ty) * tileSizeV)
                        } else {
                            batch.draw(framebufferCurrent.colorBufferTexture,
                                    rx + (tileSizeF - (tileSizeF * newAmt)) * 0.5f, ry,
                                    tileSizeF * newAmt, tileSizeF,
                                    tx * tileSizeU, 1f - (ty + 1) * tileSizeV, (tx + 1) * tileSizeU, 1f - (ty) * tileSizeV)
                        }

                        batch.setColor(1f, 1f, 1f, 1f)
                    }
                }
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }

        batch.end()
    }

    override fun renderUpdate() {
        super.renderUpdate()
        
        newVersionFloaterAnimation.set(MathHelper.getCosineWave(1.5f))
        
//        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
//            crossFade.fadeTo(musicPlayer, 1000f)
//        }
//        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
//            crossFade.fadeTo(bandpassGain, 1000f)
//        }
        // DEBUG remove later 
//        if (Paintbox.debugMode && Paintbox.stageOutlines != Paintbox.StageOutlineMode.NONE && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
//            main.screen = MainMenuScreen(main)
//        }
    }

    fun requestTileFlip(newFlip: TileFlip) {
        this.flipAnimation = newFlip
        resetTiles()
        framebufferSwapRequested = true
    }
    
    fun transitionAway(action: () -> Unit) {
        requestTileFlip(TileFlip(0, 0, tilesWidth, tilesHeight, cornerStart = Corner.TOP_LEFT))
        this.transitionAway = action
        main.inputMultiplexer.removeProcessor(processor)
        soundSys.fadeMusicToSilent()
    }

    fun prepareShow(doFlipAnimation: Boolean = false): MainMenuScreen {
        resize(Gdx.graphics.width, Gdx.graphics.height)
        
        resetTiles()
        // Uncomment 2 lines below to have it reset to the uppermostMenu each time
//        menuCollection.changeActiveMenu(menuCollection.uppermostMenu, false, instant = true)
//        menuCollection.resetMenuStack()
        if (doFlipAnimation && flipAnimationEnabled.getOrCompute()) {
            // Black out frame buffers
            lastProjMatrix.set(batch.projectionMatrix)
            val camera = fullCamera
            listOf(framebufferOld, framebufferCurrent).forEach { newFB ->
                newFB.begin()
                batch.projectionMatrix = camera.combined
                batch.begin()
                batch.setColor(0f, 0f, 0f, 1f)
                batch.fillRect(0f, 0f, camera.viewportWidth, camera.viewportHeight)
                batch.setColor(1f, 1f, 1f, 1f)
                batch.end()
                batch.projectionMatrix = lastProjMatrix
                newFB.end()
            }
            
            flipAnimation = TileFlip(0, 0, tilesWidth, tilesHeight, cornerStart = Corner.TOP_LEFT)
        }
        
        return this
    }

    private fun updateFramebuffers() {
        val cachedFramebufferSize = this.framebufferSize
        val viewport = fullViewport
        val width = viewport.worldWidth.roundToInt()
        val height = viewport.worldHeight.roundToInt()
        if (width > 0 && height > 0 && (cachedFramebufferSize.width != width || cachedFramebufferSize.height != height)) {
            createFramebuffers(width, height, Pair(framebufferOld, framebufferCurrent))
        }
    }

    private fun createFramebuffers(width: Int, height: Int, oldBuffers: Pair<FrameBuffer, FrameBuffer>?) {
        oldBuffers?.second?.disposeQuietly()
        this.framebufferOld = FrameBuffer(Pixmap.Format.RGB888, width, height, true)
        this.framebufferCurrent = FrameBuffer(Pixmap.Format.RGB888, width, height, true)
        this.framebufferSize = WindowSize(width, height)
        // Render old old FB into new old FB
        val oldoldFB = oldBuffers?.first
        if (oldoldFB != null) {
            lastProjMatrix.set(batch.projectionMatrix)
            val camera = fullCamera
            val newFB = this.framebufferOld
            newFB.begin()
            batch.projectionMatrix = camera.combined
            batch.begin()
            batch.setColor(1f, 1f, 1f, 1f)
            batch.draw(oldoldFB.colorBufferTexture, 0f, 0f, camera.viewportWidth, camera.viewportHeight, 0f, 0f, 1f, 1f)
            batch.end()
            batch.projectionMatrix = lastProjMatrix
            newFB.end()
        }

        oldBuffers?.first?.disposeQuietly()
    }

    private fun resetTiles() {
        tiles.forEach { it.forEach { t -> t.reset() } }
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
        sceneRoot.cancelTooltip()
        
        soundSys.soundSystem.setPaused(false)
        if (enoughMusicLoaded.get() && musicFinishedLoading.get() && !firstShowing.get()) {
            soundSys.resetMusic()
        }
        firstShowing.set(false)

        DiscordCore.updateActivity(DefaultPresences.idle())
        background.initializeFromType(this.backgroundType)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(processor)
        soundSys.soundSystem.setPaused(true)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        fullViewport.update(width, height, true)
        sceneRoot.resize()
        updateFramebuffers()
    }

    override fun dispose() {
        framebufferOld.disposeQuietly()
        framebufferCurrent.disposeQuietly()
        soundSys.shutdown()
        soundSys.disposeQuietly()
        StreamUtils.closeQuietly(musicSample)
    }

    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        val currentPendingKBBinding = pendingKeyboardBinding.getOrCompute()
        if (currentPendingKBBinding != null) {
            val status: InputSettingsMenu.PendingKeyboardBinding.Status =
                    if (keycode == Input.Keys.ESCAPE) {
                        InputSettingsMenu.PendingKeyboardBinding.Status.CANCELLED
                    } else InputSettingsMenu.PendingKeyboardBinding.Status.GOOD
            currentPendingKBBinding.onInput(status, keycode)
            this.pendingKeyboardBinding.set(null)
            consumed = true
        }

        return consumed || super.keyDown(keycode)
    }

    override fun getDebugString(): String {
        return "" +
                """path: ${sceneRoot.mainLayer.lastHoveredElementPath.map { "${it::class.java.simpleName}" }}
currentMenu: ${menuCollection.activeMenu.getOrCompute()?.javaClass?.simpleName}
soundSysPaused: ${soundSys.soundSystem.isPaused} / player: ${soundSys.musicPlayer.isPaused}
playerPos: ${soundSys.musicPlayer.position}
"""
    }
    
    inner class SoundSys : Disposable {
        val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem(settings = SoundSystem.SoundSystemSettings(false)).apply {
            this.setPaused(true)
            this.audioContext.out.gain = menuMusicVolume.get()
        }
        val musicPlayer: MusicSamplePlayer = beadsMusic.createPlayer(soundSystem.audioContext).also { player ->
            val sample = musicSample
            player.loopStartMs = sample.samplesToMs(123_381.0).toFloat()
            player.loopEndMs = sample.samplesToMs(8_061_382.0).toFloat()
            player.loopType = SamplePlayer.LoopType.LOOP_FORWARDS
        }
        val bandpass: Bandpass = Bandpass(soundSystem.audioContext, musicPlayer.outs, musicPlayer.outs)
        val crossFade: CrossFade = CrossFade(soundSystem.audioContext, if (shouldBeBandpass) bandpass else musicPlayer)
        
        init {
            bandpass.addInput(musicPlayer)
            
            soundSystem.audioContext.out.addInput(crossFade)
        }
        
        fun start() {
            soundSystem.setPaused(false)
            soundSystem.startRealtime()
        }
        
        fun shutdown() {
            soundSystem.setPaused(true)
            soundSystem.stopRealtime()
        }
        
        fun fadeToBandpass(durationMs: Float = 1000f) {
            shouldBeBandpass = true
//            crossFade.fadeTo(bandpass, durationMs) // Reimplement bandpass when needed.
        }
        
        fun fadeToNormal(durationMs: Float = 1000f) {
            shouldBeBandpass = false
//            crossFade.fadeTo(musicPlayer, durationMs) // Reimplement bandpass when needed.
        }
        
        fun resetMusic() {
            musicPlayer.gain = 1f
            musicPlayer.position = 0.0
            fadeToNormal(1f)
        }
        
        fun fadeMusicToSilent() {
            Gdx.app.postRunnable {
                musicPlayer.gain = 0.5f
                Gdx.app.postRunnable {
                    musicPlayer.gain = 0f
                }
            }
        }

        override fun dispose() {
            shutdown()
            soundSystem.dispose()
        }
    }
}