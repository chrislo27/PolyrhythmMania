package polyrhythmmania.screen.results

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.Ranking
import polyrhythmmania.engine.input.Score
import kotlin.math.min
import kotlin.properties.Delegates

class ResultsScreen(main: PRManiaGame, val score: Score)
    : PRManiaScreen(main) {
    
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
    private val sceneRoot: SceneRoot = SceneRoot(uiCamera)
    private val inputProcessor: InputProcessor = sceneRoot.inputSystem
    private val resultsPane: ResultsPane
    
    private var currentStage: ResultsStage by Delegates.observable(this.Loading()) { _, old, new ->
        if (old != new) {
            old.whenDone()
            new.onStart()
        }
    }
    
    init {
        resultsPane = ResultsPane(main, score)
        sceneRoot += resultsPane
        
        resultsPane.titleLabel.visible.set(false)
        resultsPane.linesLabel.text.set("")
        resultsPane.scoreValue.set(-1)
        resultsPane.rankingPane.visible.set(false)
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
        batch.projectionMatrix = main.nativeCamera.combined

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
            soundFinish = Gdx.audio.newSound(Gdx.files.internal(if (score.noMiss || score.skillStar) "sounds/results/score_finish_nhs.ogg" else "sounds/results/score_finish.ogg"))
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
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            main.screen = ResultsScreen(main, score.copy())
        }
    }

    override fun show() {
        super.show()
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.addProcessor(inputProcessor)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(inputProcessor)
        this.disposeQuietly()
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
        return sound.play(main.settings.menuSfxVolume.getOrCompute() / 100f)
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
            resultsPane.linesLabel.text.set(score.line1)
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
            resultsPane.linesLabel.text.set(resultsPane.linesLabel.text.getOrCompute() + "\n\n" + score.line2)
        }

        override fun nextStage(): ResultsStage {
            return ScoreFilling()
        }
    }

    private inner class ScoreFilling : ResultsStage() {
        private var soundID: Long = -1L
        
        init {
            timeout = (145f / 60) * (score.scoreInt / 100f)
        }

        override fun isDone(): Boolean {
            return super.isDone() // TODO add shortcut for pressing A/B to skip ahead
        }

        override fun onStart() {
            soundID = playSound(soundFilling)
        }

        override fun update() {
            resultsPane.scoreValue.set(MathUtils.lerp(0f, score.scoreInt.toFloat(), (progress / timeout).coerceIn(0f, 1f)).toInt())
        }

        override fun whenDone() {
            resultsPane.scoreValue.set(score.scoreInt)
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
            resultsPane.rankingPane.visible.set(true)
        }

        override fun nextStage(): ResultsStage? {
            return null
        }
    }
}