package polyrhythmmania.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.binding.FloatVar
import paintbox.binding.IntVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeIn
import paintbox.transition.FadeOut
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.Settings
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.InputThresholds
import polyrhythmmania.engine.tempo.TempoUtils
import polyrhythmmania.sidemodes.SidemodeAssets
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.soundsystem.sample.PlayerLike
import polyrhythmmania.util.DecimalFormats
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class CalibrationScreen(main: PRManiaGame, val baseInputCalibration: InputCalibration, val aKeybind: Int)
    : PRManiaScreen(main) {
    
    companion object {
        private const val CALIBRATION_BPM: Float = 129f
    }

    private val batch: SpriteBatch = main.batch
    private val settings: Settings = main.settings

    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    private val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val inputProcessor: InputProcessor = sceneRoot.inputSystem
    private val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    
    private lateinit var player: PlayerLike
    private var lastCowbellBeat: Int = -1
    private val summedOffsets: FloatVar = FloatVar(0f)
    private val lastInputOffset: FloatVar = FloatVar(0f)
    private val inputCount: IntVar = IntVar(0)
    private val estimatedOffset: FloatVar = FloatVar {
        (summedOffsets.useF() / inputCount.useI().coerceAtLeast(1)) * 1000
    }
    
    private val pistonAnimations: List<TextureRegion> = listOf(
            TextureRegion(AssetRegistry.get<Texture>("gba_spritesheet"), 1, 35, 32, 40),
            TextureRegion(AssetRegistry.get<Texture>("gba_spritesheet"), 67, 35, 32, 40),
            TextureRegion(AssetRegistry.get<Texture>("gba_spritesheet"), 34, 35, 32, 40),
    )
    private var pistonAnimation: Float = 0f

    init {
        val upperVbox = VBox().apply { 
            this.bindHeightToParent(adjust = -64f)
        }
        sceneRoot += upperVbox
        upperVbox += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(136f)
            this.padding.set(Insets(8f, 8f, 8f, 8f))
            
            this += VBox().apply {
                this.spacing.set(8f)
                this.temporarilyDisableLayouts { 
                    this += TextLabel(binding = { Localization.getVar("calibration.title").use() }, font = main.fontMainMenuHeading).apply { 
                        this.textColor.set(Color.WHITE)
                        this.renderAlign.set(Align.center)
                        this.bounds.height.set(48f)
                    }
                    this += TextLabel(binding = { Localization.getVar("calibration.instructions", Var {
                        listOf(Input.Keys.toString(aKeybind))
                    }).use() }, font = main.fontMainMenuThin).apply { 
                        this.textColor.set(Color.WHITE)
                        this.renderAlign.set(Align.center)
                        this.bounds.height.set(64f)
                        this.doLineWrapping.set(true)
                    }
                }
            }
        }
        
        upperVbox += RectElement(Color(0f, 0f, 0f, 0.75f * 0)).apply {
            Anchor.TopCentre.configure(this)
            this.bindWidthToParent(adjust = -500f)
            this.bounds.height.set(128f)
            this.padding.set(Insets(8f, 8f, 8f, 8f))

            this += TextLabel(binding = { Localization.getVar("calibration.offset", Var {
                listOf(settings.inputCalibration.use().audioOffsetMs.roundToInt(), estimatedOffset.useF().roundToInt())
            }).use() }, font = main.fontMainMenuMain).apply {
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(Align.center)
                
                this.renderBackground.set(true)
                this.backgroundColor.set(Color(0f, 0f, 0f, 0.75f))
                this.bgPadding.set(Insets(10f, 10f, 16f, 16f))
            }
        }
        
        upperVbox += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
            Anchor.TopCentre.configure(this)
            this.bindWidthToParent(adjust = -400f)
            this.bounds.height.set(128f)
            this.padding.set(Insets(8f, 8f, 8f, 8f))

            this += TextLabel(binding = { Localization.getVar("calibration.offset.details", Var {
                listOf(DecimalFormats.format("0.00", estimatedOffset.useF()), DecimalFormats.format("0.00", lastInputOffset.useF()), inputCount.useI())
            }).use() }, font = main.fontMainMenuMain).apply {
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(Align.center)
            }
        }
        
        sceneRoot += RectElement(Color(0f, 0f, 0f, 0.75f)).apply { 
            Anchor.BottomLeft.configure(this)
            this.bounds.height.set(56f)
            this.padding.set(Insets(8f, 8f, 8f, 8f))
            
            this += HBox().apply { 
                this.spacing.set(8f)
                this.temporarilyDisableLayouts { 
                    this += Button(binding = { Localization.getVar("calibration.button.back").use() }, font = main.fontMainMenuMain).apply {
                        this.bounds.width.set(130f)
                        this.applyStyleContent()
                        this.setOnAction {
                            main.screen = TransitionScreen(main, main.screen, main.mainMenuScreen,
                                    FadeOut(0.25f, Color(0f, 0f, 0f, 1f)), FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                                this.onEntryEnd = {
                                    this@CalibrationScreen.disposeQuietly()
                                }
                            }
                        }
                    }
                    this += Button(binding = { Localization.getVar("calibration.button.resetEstimate").use() }, font = main.fontMainMenuThin).apply {
                        this.bounds.width.set(300f)
                        this.applyStyleContent()
                        this.setOnAction {
                            resetCalibrationEstimate()
                        }
                    }
                    this += Button(binding = { Localization.getVar("calibration.button.setSettingAsEstimate").use() }, font = main.fontMainMenuThin).apply {
                        this.bounds.width.set(600f)
                        this.applyStyleContent()
                        this.setOnAction {
                            settings.calibrationAudioOffsetMs.set(estimatedOffset.get().roundToInt())
                            main.mainMenuScreen.menuCollection.calibrationSettingsMenu.manualOffsetSlider.setValue(settings.calibrationAudioOffsetMs.getOrCompute().toFloat())
                        }
                    }
                }
            }
        }
    }

    override fun renderUpdate() {
        super.renderUpdate()
        
        val currentBeat = TempoUtils.secondsToBeats((player.position / 1000).toFloat(), CALIBRATION_BPM)
        val currentBeatInt = currentBeat.toInt()
        if (currentBeatInt != lastCowbellBeat) {
            if (lastCowbellBeat < 0) {
                lastCowbellBeat++
            } else {
                lastCowbellBeat = currentBeatInt
                soundSystem.playAudio(AssetRegistry.get<BeadsSound>("sfx_cowbell")) { playerLike ->
                    playerLike.gain = 0.5f
                }
            }
        }
        
        if (Gdx.input.isKeyJustPressed(aKeybind)) {
            pistonAnimation = 1f
            val beatOffset = -(currentBeat - currentBeat.roundToInt())
            val secOffset = TempoUtils.beatsToSeconds(beatOffset, CALIBRATION_BPM)
            lastInputOffset.set(secOffset * 1000)
            if (secOffset.absoluteValue <= InputThresholds.MAX_OFFSET_SEC * 2) {
                inputCount.set(inputCount.get() + 1)
                summedOffsets.set(summedOffsets.get() + secOffset)
            }
        }
    }
    
    private fun resetCalibrationEstimate() {
        summedOffsets.set(0f)
        inputCount.set(0)
        lastInputOffset.set(0f)
        pistonAnimation = 0f
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = this.batch
        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()
        
        uiViewport.apply()

        batch.setColor(0.9f, 0.9f, 0.9f, 1f)
        batch.fillRect(0f, 0f, camera.viewportWidth, camera.viewportHeight)
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(AssetRegistry.get<Texture>("green_grid"), 0f, 0f, camera.viewportWidth, camera.viewportHeight)
        
        val animationIndex: Int = when (pistonAnimation) {
            in 0.95f..1.0f -> 2
            in 0.5f..0.95f -> 1
            else -> 0
        }
        val tex = pistonAnimations[animationIndex]
        val w = 32f * 4
        val h = 40f * 4
        batch.draw(tex, camera.viewportWidth / 2 - w / 2, 120f, w, h)
        if (pistonAnimation > 0) {
            pistonAnimation = (pistonAnimation - (Gdx.graphics.deltaTime / (60f / CALIBRATION_BPM))).coerceIn(0f, 1f)
        }
        
        sceneRoot.renderAsRoot(batch)
        
        batch.end()
        batch.projectionMatrix = main.nativeCamera.combined
        
        super.render(delta)
    }

    fun prepareShow() {
        soundSystem.startRealtime()
        
        val theme = SidemodeAssets.practiceTheme
        soundSystem.playAudio(theme) { player ->
            player.useLoopParams(LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, theme.musicSample.lengthMs))
            
            this.player = player
        }
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.addProcessor(inputProcessor)
        soundSystem.setPaused(false)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(inputProcessor)
        soundSystem.stopRealtime()
        soundSystem.disposeQuietly()
    }

    override fun showTransition() {
        super.showTransition()
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }
    
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun dispose() {
    }
    

    private fun Button.applyStyleContent() {
        val skin = (this.skin.getOrCompute() as? ButtonSkin) ?: return
        skin.roundedRadius.set(8)
        this.padding.set(Insets(8f))
    }
}