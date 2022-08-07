package polyrhythmmania.storymode.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.PaintboxScreen
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.transition.TransitionScreen
import paintbox.transition.WipeTransitionHead
import paintbox.transition.WipeTransitionTail
import paintbox.ui.Anchor
import paintbox.ui.RenderAlign
import paintbox.ui.SceneRoot
import paintbox.ui.animation.Animation
import paintbox.ui.animation.AnimationHandler
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.QuadElement
import paintbox.util.WindowSize
import paintbox.util.gdxutils.NestedFrameBuffer
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.screen.play.AbstractEnginePlayScreen
import polyrhythmmania.screen.play.pause.PauseMenuHandler
import polyrhythmmania.screen.play.pause.PauseOption
import polyrhythmmania.screen.play.pause.TengokuBgPauseMenuHandler
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.ui.TextSlideInterp
import polyrhythmmania.util.FrameBufferManager
import polyrhythmmania.util.FrameBufferMgrSettings
import kotlin.math.max


class StoryPlayScreen(
        main: PRManiaGame,
        container: Container,
        challenges: Challenges, inputCalibration: InputCalibration,
        gameMode: GameMode?,
        val contract: Contract,
        val exitToScreen: PaintboxScreen,
) : AbstractEnginePlayScreen(main, null, container, challenges, inputCalibration, gameMode) {

    override val pauseMenuHandler: PauseMenuHandler = TengokuBgPauseMenuHandler(this).apply { // FIXME new pause menu
        this.pauseBg.also {
            it.cycleSpeed = 0f
            it.topColor.set(PRManiaColors.debugColor)
            it.bottomColor.set(PRManiaColors.debugColor)
        }
    }

    private var disableCatchingCursorOnHide: Boolean = false

    private val blurShader: ShaderProgram = GaussianBlur.createShaderProgram()
    private val framebufferMgr: FrameBufferManager = FrameBufferManager(2, FrameBufferMgrSettings(Pixmap.Format.RGB888), tag = "StoryPlayScreen", referenceWindowSize = WindowSize(1280, 720))
    
    // Intro card
    private val introCardDefaultDuration: Float = 3f
    private val introCardSceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val introCardAnimationHandler: AnimationHandler = AnimationHandler(introCardSceneRoot)
    private val introCardTime: FloatVar = FloatVar(0f)
    private val inIntroCard: ReadOnlyBooleanVar = BooleanVar { introCardTime.use() > 0f }
    private val blurStrength: FloatVar
    private val blackBarsAmount: FloatVar
    private val textSlide: TextSlideInterp
    
    init {
        val optionList = mutableListOf<PauseOption>()
        optionList += PauseOption("play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", !(gameMode is EndlessPolyrhythm && gameMode.dailyChallenge != null)) {
            playMenuSound("sfx_menu_enter_game")

            val thisScreen: StoryPlayScreen = this
            val resetAction: () -> Unit = {
                resetAndUnpause()
                disableCatchingCursorOnHide = false
            }
            if (shouldCatchCursor()) {
                disableCatchingCursorOnHide = true
                Gdx.input.isCursorCatched = true
            }
            main.screen = TransitionScreen(main, thisScreen, thisScreen,
                    WipeTransitionHead(Color.BLACK.cpy(), 0.4f), WipeTransitionTail(Color.BLACK.cpy(), 0.4f)).apply {
                onEntryEnd = resetAction
            }
        }
        optionList += PauseOption(StoryL10N.getVar("play.pause.quit"), true) {
            quitToScreen(exitToScreen)
            Gdx.app.postRunnable {
                playMenuSound("sfx_pause_exit")
            }
        }
        this.pauseOptions.set(optionList)
    }
    
    init {
        blurStrength = FloatVar {
            val effectInLastSec = 0.5f // Unblur only in the first and last 0.5 seconds
            val time = introCardTime.use() * introCardDefaultDuration // Counts down from duration to 0.0 sec
            
            val interpolation = Interpolation.pow2Out
            when {
                time <= 0f -> 0f
                time < effectInLastSec -> {
                    val timePercent = 1f - time / effectInLastSec
                    interpolation.apply(1f, 0f, timePercent)
                }
                else -> 1f
            }
        }
        blackBarsAmount = FloatVar {
            val effectInLastSec = 0.5f // Effect only in the first and last few seconds
            val time = introCardTime.use() * introCardDefaultDuration // Counts down from duration to 0.0 sec
            
            val interpolation = Interpolation.smoother
            val firstThreshold = introCardDefaultDuration - effectInLastSec
            when {
                time <= 0f -> 0f
                time > firstThreshold -> {
                    val timePercent = 1f - (time - firstThreshold) / effectInLastSec
                    interpolation.apply(0f, 1f, timePercent)
                }
                time < effectInLastSec -> {
                    val timePercent = 1f - time / effectInLastSec
                    interpolation.apply(1f, 0f, timePercent)
                }
                else -> 1f
            }
        }
        textSlide = TextSlideInterp(introCardTime)
        
        // Black bars: Full is 16:9 = 1.778, cinema is 2.35:1 = 2.35. About 25% less height
        val slantAmount = 1f
        val barHeight = 0.125f * 1.25f
        introCardSceneRoot += QuadElement(Color.BLACK).apply { 
            this.bindHeightToParent(multiplier = barHeight * slantAmount)
            Anchor.TopLeft.yConfigure(this) {
                val h = bounds.height.use()
                -h + (blackBarsAmount.use() * h)
            }
            
            this.bottomLeftOffsetV.set(0f)
            this.bottomRightOffsetV.set(1f - slantAmount)
        }
        introCardSceneRoot += QuadElement(Color.BLACK).apply { 
            this.bindHeightToParent(multiplier = barHeight * slantAmount)
            Anchor.BottomLeft.yConfigure(this) {
                val h = bounds.height.use()
                h - (blackBarsAmount.use() * h)
            }

            this.topLeftOffsetV.set(1f)
            this.topRightOffsetV.set(slantAmount)
        }
        // Title
        introCardSceneRoot += TextLabel(contract.name.getOrCompute(), font = PRManiaGame.instance.fontGamePracticeClear).apply { 
//            this.bindYToParentHeight(multiplier = 0.3f)
            Anchor.CentreLeft.configure(this)
            this.bindHeightToParent(multiplier = 0.25f)
            
            this.margin.set(Insets(0f, 0f, 10f, 10f))
            this.renderAlign.set(RenderAlign.center)
            this.textColor.set(Color.WHITE.cpy())
            
            this.bounds.x.bind {
                val parentW = parent.use()?.bounds?.width?.use() ?: 0f    
                MathUtils.lerp(-(bounds.width.use()), parentW, textSlide.textSlideAmount.use())
            }
        }
    }

    override fun initializeGameplay() {
        super.initializeGameplay()
        cancelIntroCard()
    }

    fun initializeIntroCard() {
        introCardTime.set(1f)
        shouldUpdateTiming.set(false)
        soundSystem.setPaused(true)
        
        // Play jingle
        val jingle: Sound = StoryAssets.get<Sound>(contract.jingleType.soundID)
        playMenuSound(jingle)

        introCardAnimationHandler.enqueueAnimation(Animation(Interpolation.linear, introCardDefaultDuration, 1f, 0f).apply { 
            this.onComplete = {
                cancelIntroCard()
            }                                                                                                     
        }, introCardTime)
    }
    
    fun cancelIntroCard() {
        introCardTime.set(0f)
        introCardAnimationHandler.cancelAnimationFor(introCardTime)
        shouldUpdateTiming.set(true)
        soundSystem.setPaused(false)
    }

    override fun renderGameplay(delta: Float) {
        framebufferMgr.frameUpdate()
        
        val batch = this.batch
        val frameBuffer = this.framebufferMgr.getFramebuffer(0)
        val frameBuffer2 = this.framebufferMgr.getFramebuffer(1)
        
        if (frameBuffer != null && frameBuffer2 != null) {
            frameBuffer.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            worldRenderer.render(batch)
            frameBuffer.end()


            // Do blur
            val passes = 7
            val blurStrength = this.blurStrength.get()
            var readBuffer: NestedFrameBuffer = frameBuffer
            var writeBuffer: NestedFrameBuffer = frameBuffer2

            val cam = this.uiCamera // 1280x720 camera always
            val shader = this.blurShader
            batch.projectionMatrix = cam.combined

            if (blurStrength > 0f) {
                batch.shader = shader
                batch.begin()
                batch.setColor(1f, 1f, 1f, 1f)

                shader.setUniformf("resolution", max(cam.viewportHeight, cam.viewportWidth))

                for (i in 0 until passes * 2) {
                    val radius = (passes - i / 2) * blurStrength

                    writeBuffer.begin()
                    if (i % 2 == 0) {
                        shader.setUniformf("dir", radius, 0f)
                    } else {
                        shader.setUniformf("dir", 0f, radius)
                    }
                    val bufferTex = readBuffer.colorBufferTexture
                    batch.draw(bufferTex, 0f, 0f, cam.viewportWidth, cam.viewportHeight, 0, 0, bufferTex.width, bufferTex.height, false, true)
                    batch.flush()
                    writeBuffer.end()


                    // Swap buffers
                    val tmp = readBuffer
                    readBuffer = writeBuffer
                    writeBuffer = tmp
                }

                batch.end()
                batch.shader = null // Reset shader
            }


            // Render final buffer to screen
            uiViewport.apply() // 1280x720 viewport always
            batch.begin()
            val bufferTex = readBuffer.colorBufferTexture
            batch.draw(bufferTex, 0f, 0f, cam.viewportWidth, cam.viewportHeight, 0, 0, bufferTex.width, bufferTex.height, false, true)
            batch.end()

            main.resetViewportToScreen()
        }
    }

    override fun renderAfterGameplay(delta: Float, camera: OrthographicCamera) {
        super.renderAfterGameplay(delta, camera)

        introCardAnimationHandler.frameUpdate()
        
        if (inIntroCard.get()) {
            uiViewport.apply()
            introCardSceneRoot.renderAsRoot(batch)
            main.resetViewportToScreen()
        }
    }

    override fun onEndSignalFired() {
        super.onEndSignalFired()
        
        // TODO
        quitToScreen(exitToScreen)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        
        if (!isPaused.get()) {
            // TODO increment play time for save file, call StorySavefile.updatePlayTime
            GlobalStats.updateTotalStoryModePlayTime()
        }
    }

    override fun uncatchCursorOnHide(): Boolean {
        return super.uncatchCursorOnHide() && !disableCatchingCursorOnHide
    }

    override fun pauseGame(playSound: Boolean) {
        if (!inIntroCard.get()) {
            super.pauseGame(playSound)
        }
    }

    override fun unpauseGame(playSound: Boolean) {
        if (!inIntroCard.get()) {
            super.unpauseGame(playSound)
        }
    }

    override fun _dispose() {
        super._dispose()
        this.framebufferMgr.disposeQuietly()
        this.blurShader.disposeQuietly()
    }
}