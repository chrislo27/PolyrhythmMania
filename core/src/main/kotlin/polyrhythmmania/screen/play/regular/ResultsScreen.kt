package polyrhythmmania.screen.play.regular

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.transition.WipeTransitionHead
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.animation.Animation
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.engine.input.score.Ranking
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.practice.AbstractPolyrhythmPractice
import polyrhythmmania.library.score.LevelScoreAttempt
import polyrhythmmania.screen.results.ResultsPane
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.WorldType
import kotlin.properties.Delegates

/**
 * Results screen for non-story gamemodes, like tutorials, Library levels, and extras.
 */
class ResultsScreen(
        main: PRManiaGame, val score: ScoreWithResults, val container: Container, val gameMode: GameMode?,
        val startOverFactory: () -> EnginePlayScreenBase,
        private val keyboardKeybinds: InputKeymapKeyboard,
        val levelScoreAttempt: LevelScoreAttempt,
        val onRankingRevealed: OnRankingRevealed?,
) : PRManiaScreen(main) {
    
    private lateinit var soundFirstLine: Sound
    private lateinit var soundMiddleLine: Sound
    private lateinit var soundEndLine: Sound
    private lateinit var soundRanking: Sound
    private lateinit var soundFilling: Sound
    private lateinit var soundFinish: Sound
    private var soundsInited: Boolean = false

    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    private val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val inputProcessor: InputProcessor = sceneRoot.inputSystem
    private val resultsPane: ResultsPane
    private val controlsPane: Pane
    
    private var currentStage: ResultsStage by Delegates.observable(this.Loading()) { _, old, new ->
        if (old != new) {
            old.whenDone()
            new.onStart()
        }
    }
    
    init {
        resultsPane = ResultsPane(main, score)
        sceneRoot += resultsPane
        
        controlsPane = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.align.set(HBox.Align.LEFT)
            this.bounds.height.set(48f)
            this.bounds.width.set(700f)
            this.spacing.set(12f)

            this += Button(Localization.getValue("play.results.back"), font = main.fontResultsMain).apply {
                this.bounds.width.set(300f)
                (this.skin.getOrCompute() as ButtonSkin).roundedRadius.set(8)
                this.setScaleXY(0.6f)
                this.setOnHoverStart {
                    playSound(AssetRegistry.get<Sound>("sfx_menu_blip"))
                }
                this.setOnAction {
                    playSound(AssetRegistry.get<Sound>("sfx_pause_exit"))
                    val thisScreen = main.screen
                    main.screen = TransitionScreen(main, thisScreen, main.mainMenuScreen,
                            WipeTransitionHead(Color.BLACK.cpy(), 0.4f, invertDirection = true),
                            FadeToTransparent(0.125f, Color(0f, 0f, 0f, 1f))).apply {
                        onEntryEnd = {
                            container.disposeQuietly()
                            main.mainMenuScreen.prepareShow(doFlipAnimation = true)
                        }
                    }
                }
            }
            this += Button(Localization.getValue("play.pause.startOver"), font = main.fontResultsMain).apply {
                this.bounds.width.set(220f)
                (this.skin.getOrCompute() as ButtonSkin).roundedRadius.set(8)
                this.setScaleXY(0.6f)
                this.setOnHoverStart {
                    playSound(AssetRegistry.get<Sound>("sfx_menu_blip"))
                }
                this.setOnAction {
                    playSound(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                    val playScreen = startOverFactory()
                    Gdx.input.isCursorCatched = true
                    main.screen = TransitionScreen(main, main.screen, playScreen, FadeToOpaque(0.5f, Color(0f, 0f, 0f, 1f)),
                            FadeToTransparent(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                        this.onEntryEnd = {
                            playScreen.resetAndUnpause()
                        }
                    }
                }
            }
        }
        
        resultsPane += controlsPane
        
        resultsPane.titleLabel.visible.set(false)
        resultsPane.linesLabel.text.set("")
        resultsPane.scoreValueFloat.set(-1f)
        resultsPane.rankingPane.visible.set(false)
        resultsPane.bonusStatsPane.visible.set(false)
        controlsPane.visible.set(false)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = main.batch
        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)
        
        batch.end()
        batch.projectionMatrix = main.actualWindowSizeCamera.combined

        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        if (!soundsInited) {
            soundFirstLine = Gdx.audio.newSound(Gdx.files.internal("sounds/results/results_first.ogg"))
            soundMiddleLine = Gdx.audio.newSound(Gdx.files.internal("sounds/results/results_middle.ogg"))
            soundEndLine = Gdx.audio.newSound(Gdx.files.internal("sounds/results/results_end.ogg"))
            soundRanking = Gdx.audio.newSound(Gdx.files.internal(score.ranking.sfxFile))
            soundFilling = Gdx.audio.newSound(Gdx.files.internal("sounds/results/score_filling.ogg"))
            soundFinish = Gdx.audio.newSound(Gdx.files.internal(if (score.noMiss || score.skillStar == true) "sounds/results/score_finish_nhs.ogg" else "sounds/results/score_finish.ogg"))
            soundsInited = true
            currentStage = this.SkipFrame(WaitFor(1f) { this.Title() }, framesToWait = 2)
        } else {
            val delta = Gdx.graphics.deltaTime
            currentStage.progress += delta
            currentStage.update()
            if (currentStage.isDone()) {
                currentStage = currentStage.nextStage() ?: this.None()
            }
        }
        
        val currentStage = this.currentStage
        if (currentStage is ScoreFilling && !currentStage.doneOverride) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(keyboardKeybinds.buttonA)) {
                currentStage.doneOverride = true
            }
        }
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.addProcessor(inputProcessor)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(inputProcessor)
        this.disposeQuietly()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun showTransition() {
        super.showTransition()
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun dispose() {
        if (soundsInited) {
            soundFirstLine.disposeQuietly()
            soundMiddleLine.disposeQuietly()
            soundEndLine.disposeQuietly()
            soundRanking.disposeQuietly()
            soundFilling.disposeQuietly()
            soundFinish.disposeQuietly()
        }
    }
    
    fun playSound(sound: Sound): Long {
        return main.playMenuSfx(sound)
    }

    override fun getDebugString(): String {
        return "stage: ${currentStage.javaClass.simpleName}"
    }

    // Result stages
    
    abstract inner class ResultsStage {
        var timeout: Float = -1f
        var progress: Float = 0f

        open fun isDone(): Boolean {
            return (progress >= timeout)
        }

        open fun whenDone() {}
        open fun onStart() {}
        open fun update() {}

        abstract fun nextStage(): ResultsStage?
    }

    private inner class None : ResultsStage() {
        override fun isDone(): Boolean {
            return false
        }

        override fun nextStage(): ResultsStage? {
            return null
        }
    }

    private inner class SkipFrame(val nextStage: ResultsStage?, val framesToWait: Int = 1)
        : ResultsStage() {

        private var framesWaited: Int = 0

        override fun isDone(): Boolean {
            return framesWaited >= framesToWait
        }

        override fun update() {
            super.update()
            framesWaited++
        }

        override fun nextStage(): ResultsStage? {
            return nextStage
        }
    }

    private inner class Loading : ResultsStage() {
        init {
            timeout = Float.MAX_VALUE
        }

        override fun nextStage(): ResultsStage? {
            return null
        }
    }
    
    private inner class WaitFor(seconds: Float, val nextStage: () -> ResultsStage?) : ResultsStage() {
        init {
            timeout = seconds
        }

        override fun nextStage(): ResultsStage? {
            return nextStage.invoke()
        }
    }
    
    private inner class Title : ResultsStage() {
        init {
            timeout = 1f
        }

        override fun onStart() {
            super.onStart()
            playSound(soundFirstLine)
            resultsPane.titleLabel.visible.set(true)
        }

        override fun nextStage(): ResultsStage {
            return Line1()
        }
    }
    private inner class Line1 : ResultsStage() {
        init {
            timeout = if (score.line2.isEmpty()) 1.25f else 1f
        }

        override fun onStart() {
            super.onStart()
            playSound(if (score.line2.isEmpty()) soundEndLine else soundMiddleLine)
            resultsPane.linesLabel.text.set("${score.line1}\n\n[color=CLEAR]${score.line2}[]")
        }

        override fun nextStage(): ResultsStage {
            return if (score.line2.isEmpty()) {
                ScoreFilling()
            } else Line2()
        }
    }
    private inner class Line2 : ResultsStage() {
        init {
            timeout =  1.25f
        }

        override fun onStart() {
            super.onStart()
            playSound(soundEndLine)
            resultsPane.linesLabel.text.set("${score.line1}\n\n${score.line2}")
        }

        override fun nextStage(): ResultsStage {
            return ScoreFilling()
        }
    }

    private inner class ScoreFilling : ResultsStage() {
        private var soundID: Long = -1L
        var doneOverride = false
        
        init {
            timeout = (145f / 60) * (score.scoreInt / 100f)
        }

        override fun isDone(): Boolean {
            return super.isDone() || doneOverride
        }

        override fun onStart() {
            soundID = playSound(soundFilling)
        }

        override fun update() {
            resultsPane.scoreValueFloat.set(MathUtils.lerp(0f, score.scoreInt.toFloat(), (progress / timeout).coerceIn(0f, 1f)))
        }

        override fun whenDone() {
            resultsPane.scoreValueFloat.set(score.scoreInt.toFloat())
            soundFilling.stop(soundID)
        }

        override fun nextStage(): ResultsStage {
            return ScoreRevealed()
        }
    }

    private inner class ScoreRevealed : ResultsStage() {
        init {
            timeout = 0.75f
        }

        override fun onStart() {
            super.onStart()
            playSound(soundFinish)
        }

        override fun nextStage(): ResultsStage {
            return RankingRevealed()
        }
    }

    private inner class RankingRevealed : ResultsStage() {
        init {
            timeout = Float.MAX_VALUE
        }

        override fun onStart() {
            super.onStart()
            playSound(soundRanking)
            Gdx.input.isCursorCatched = false
            resultsPane.rankingPane.visible.set(true)
            resultsPane.bonusStatsPane.visible.set(true)
            controlsPane.visible.set(true)
            controlsPane.opacity.set(0f)
            sceneRoot.animations.enqueueAnimation(Animation(Interpolation.smooth2, 0.25f, 0f, 1f, delay = 0.75f), controlsPane.opacity)
            Gdx.app.postRunnable { 
                onRankingRevealed?.onRankingRevealed(levelScoreAttempt, score)
                
                // Statistics-related
                container.engine.inputter.addNonEndlessInputStats()
                when (score.ranking) {
                    Ranking.TRY_AGAIN -> GlobalStats.rankingTryAgain
                    Ranking.OK -> GlobalStats.rankingOK
                    Ranking.SUPERB -> GlobalStats.rankingSuperb
                }.increment()
                val challenges = score.challenges
                if (score.noMiss) {
                    GlobalStats.noMissesGotten.increment()
                    if (container.world.worldMode.worldType == WorldType.Assemble) {
                        Achievements.awardAchievement(Achievements.assembleNoMiss)
                    }
                }
                if (challenges.goingForPerfect) {
                    if (score.noMiss) {
                        GlobalStats.perfectsEarned.increment()
                        Achievements.attemptAwardThresholdAchievement(Achievements.perfectFirstTime, score.nInputs)
                    }
                }
                if (gameMode != null && gameMode is AbstractPolyrhythmPractice) {
                    if (score.ranking != Ranking.TRY_AGAIN) {
                        Achievements.practicePassFlag = Achievements.practicePassFlag or gameMode.flagBit
                        if (Achievements.practicePassFlag == 0b0011) {
                            Achievements.awardAchievement(Achievements.playAllPractices)
                        }
                        if (score.noMiss) {
                            Achievements.practiceNoMissFlag = Achievements.practiceNoMissFlag or gameMode.flagBit
                            if (Achievements.practiceNoMissFlag == 0b0011) {
                                Achievements.awardAchievement(Achievements.noMissAllPractices)
                            }
                        }
                        Achievements.persist()
                    }
                }
            }
        }

        override fun nextStage(): ResultsStage? {
            return null
        }
    }
}