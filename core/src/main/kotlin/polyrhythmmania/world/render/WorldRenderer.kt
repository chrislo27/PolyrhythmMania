package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.*
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.MathHelper
import paintbox.util.gdxutils.*
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.ui.TextboxPane
import polyrhythmmania.util.RodinSpecialChars
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.render.bg.NoOpWorldBackground
import polyrhythmmania.world.render.bg.WorldBackground
import polyrhythmmania.world.tileset.Tileset
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class WorldRenderer(val world: World, val tileset: Tileset) {

    companion object {
        val comparatorRenderOrder: Comparator<Entity> = Comparator<Entity> { o1, o2 ->
            val xyz1 = o1.position.x - o1.position.z - o1.position.y
            val xyz2 = o2.position.x - o2.position.z - o2.position.y
            -xyz1.compareTo(xyz2)
        }

        fun convertWorldToScreen(vec3: Vector3): Vector3 {
            return vec3.apply {
                val oldX = this.x
                val oldY = this.y // + MathHelper.getSineWave((System.currentTimeMillis() * 3).toLong() + (x * -500 - z * 500).toLong(), 2f) * 0.4f
                val oldZ = this.z
                this.x = oldX / 2f + oldZ / 2f
                this.y = oldX * (8f / 32f) + (oldY - 3f) * 0.5f - oldZ * (8f / 32f)
                this.z = 0f
            }
        }

        // For doing entity render culling
        private val tmpVec: Vector3 = Vector3(0f, 0f, 0f)
        private val tmpRect: Rectangle = Rectangle(0f, 0f, 0f, 0f)
        private val tmpRect2: Rectangle = Rectangle(0f, 0f, 0f, 0f)
    }

    val camera: OrthographicCamera = OrthographicCamera().apply {
//        setToOrtho(false, 7.5f, 5f) // GBA aspect ratio
        setToOrtho(false, 5 * (16f / 9f), 5f)
        zoom = 1f
        position.set(zoom * viewportWidth / 2.0f, zoom * viewportHeight / 2.0f, 0f)
//        zoom = 1.5f
        update()
    }
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, 1280f, 720f)
        update()
    }
    private val tmpMatrix: Matrix4 = Matrix4()

    var entitiesRenderedLastCall: Int = 0
        private set
    var entityRenderTimeNano: Long = 0L
        private set
    
    var worldBackground: WorldBackground = NoOpWorldBackground

    var renderUI: Boolean = true
    var showSkillStarSetting: Boolean = PRManiaGame.instance.settings.showSkillStar.getOrCompute()

    val showEndlessModeScore: Var<Boolean> = Var(false)
    val prevHighScore: Var<Int> = Var(-1)
    val dailyChallengeDate: Var<LocalDate?> = Var(null)
    val endlessModeSeed: Var<String?> = Var(null)
    private val currentEndlessScore: Var<Int> = Var(0)
    private val currentEndlessLives: Var<Int> = Var(0)

    private var skillStarSpinAnimation: Float = 0f
    private var skillStarPulseAnimation: Float = 0f

    private val uiSceneRoot: SceneRoot = SceneRoot(uiCamera)
    private val textBoxPane: TextboxPane = TextboxPane()
    private val textBoxLabel: TextLabel = TextLabel("")
    private val textBoxInputLabel: TextLabel = TextLabel(RodinSpecialChars.BORDERED_A, font = PRManiaGame.instance.fontGameTextbox)
    private val perfectPane: Pane = Pane()
    private val perfectIcon: ImageNode
    private val perfectIconFlash: ImageNode
    private val perfectIconFailed: ImageNode
    private val moreTimesLabel: TextLabel = TextLabel("")
    private val moreTimesVar: Var<Int> = Var(0)
    private val endlessModeScorePane: Pane = Pane()
    private val endlessModeScoreLabelScaleXY: FloatVar = FloatVar(1f)
    private val endlessModeScoreLabel: TextLabel
    private val endlessModeGameOverPane: Pane = Pane()
    private val endlessModeGameOverLabel: TextLabel
    
    init {
        val baseMarkup = Markup(mapOf("prmania_icons" to PRManiaGame.instance.fontIcons,
                "moretimes" to PRManiaGame.instance.fontGameMoreTimes),
                TextRun(PRManiaGame.instance.fontGameTextbox, ""), lenientMode = true)

        uiSceneRoot += endlessModeScorePane.apply {
            this.visible.bind { showEndlessModeScore.use() }
            Anchor.TopLeft.configure(this, offsetX = 32f, offsetY = 32f)
//            this.bounds.width.set(400f)
            this.bindWidthToParent(adjust = -64f)
            this.bounds.height.set(200f)

            val vbox = VBox().apply {
                this += Pane().apply {
                    this.bounds.height.set(40f)

                    val prevTextVar: ReadOnlyVar<String> = Var.bind { 
                        val date = dailyChallengeDate.use()
                        val seed = endlessModeSeed.use()
                        if (date != null) {
                            Localization.getVar("play.endless.dailyChallenge", Var { listOf(date.format(DateTimeFormatter.ISO_DATE)) }).use()
                        } else if (seed != null) {
                            Localization.getVar("play.endless.seed", Var { listOf(seed) }).use()
                        } else {
                            Localization.getVar("play.endless.prevHighScore", Var { listOf(prevHighScore.use()) }).use()
                        }
                    }
                    this += TextLabel(binding = { prevTextVar.use() },
                            font = PRManiaGame.instance.fontGameMoreTimes).apply {
                        this.bindWidthToParent(multiplier = 0.4f)
                        this.doXCompression.set(false)
                        this.renderAlign.set(Align.topLeft)
                        this.setScaleXY(0.4f)
                        this.textColor.set(Color().grey(229f / 255f))
                    }

//                    val livesVar: ReadOnlyVar<String> = Localization.getVar("play.endless.lives", Var {
//                        val l = currentEndlessLives.use()
//                        listOf("[font=prmania_icons scale=6 offsety=-0.125]${"R".repeat(l)}[]")
//                    })
//                    val endlessModeLivesLabel = TextLabel(binding = { livesVar.use() }).apply {
//                        this.bounds.width.set(480f)
//                        Anchor.TopRight.configure(this)
//                        this.markup.set(baseMarkup)
//                        this.renderAlign.set(Align.left)
//                        this.textColor.set(Color(1f, 1f, 1f, 1f))
//                        this.setScaleXY(0.5f)
//                    }
//                    this += endlessModeLivesLabel
                }

                val currentScoreVar = Localization.getVar("play.endless.score", Var { listOf(currentEndlessScore.use()) })
                endlessModeScoreLabel = TextLabel(binding = { currentScoreVar.use() },
                        font = PRManiaGame.instance.fontPauseMenuTitle).apply {
                    this.bounds.height.set(100f)
                    this.renderAlign.set(Align.topLeft)
                    this.textColor.set(Color(1f, 1f, 1f, 1f))
                    val scaleMul = 1f / 1.25f
                    this.scaleX.bind { endlessModeScoreLabelScaleXY.useF() * scaleMul }
                    this.scaleY.bind { endlessModeScoreLabelScaleXY.useF() * scaleMul }
                }
                this += endlessModeScoreLabel

                val endlessModeLivesLabel = TextLabel(binding = {
                    val l = currentEndlessLives.use()
                    /* space at start is necessary -> */ " [font=prmania_icons scale=6 offsety=-0.125]${"R".repeat(l)}[]"
                }).apply {
                    this.bounds.height.set(40f)
                    Anchor.TopRight.configure(this)
                    this.markup.set(baseMarkup)
                    this.renderAlign.set(Align.left)
                    this.textColor.set(Color(1f, 1f, 1f, 1f))
                    this.setScaleXY(0.333f)
                }
                this += endlessModeLivesLabel

            }
            this += vbox
        }
        
        uiSceneRoot += moreTimesLabel.apply {
            Anchor.BottomRight.configure(this)
            val locVar = Localization.getVar("practice.moreTimes.times", Var { listOf(moreTimesVar.use()) })
            this.text.bind { locVar.use() }
            this.renderAlign.set(Align.right)
            this.margin.set(Insets(0f, 16f, 0f, 16f))
            this.markup.set(Markup(emptyMap(), TextRun(PRManiaGame.instance.fontGameMoreTimes, "")))
            this.bounds.width.set(510f)
            this.bounds.height.set(86f)
            this.textColor.set(Color.WHITE)
            this.visible.bind { moreTimesVar.use() > 0 }
        }
        perfectIcon = ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")["perfect"])
        perfectIconFailed = ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")["perfect_failed"]).apply {
            this.visible.set(false)
        }
        perfectIconFlash = ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")["perfect_hit"]).apply {
            this.opacity.set(0f)
        }
        uiSceneRoot += perfectPane.apply {
            Anchor.TopLeft.configure(this, offsetX = 32f, offsetY = 32f)
            this.bounds.width.set(500f)
            this.bounds.height.set(64f)
            this += Pane().apply {
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(4f))
                this += perfectIcon
                this += perfectIconFlash
                this += perfectIconFailed
            }
            this += TextLabel(binding = { Localization.getVar("play.perfect").use() },
                    font = PRManiaGame.instance.fontGameMoreTimes).apply {
                Anchor.TopRight.configure(this)
                this.textColor.set(Color.WHITE)
                this.padding.set(Insets(0f, 0f, 5f, 0f))
                this.bindWidthToParent(adjust = -64f)
                this.renderAlign.set(Align.left)
                this.setScaleXY(0.6f)
            }
        }
        
        endlessModeGameOverPane.apply {
            this.visible.set(false)
            this += RectElement(Color(0f, 0f, 0f, 0.5f))
        }
        uiSceneRoot += endlessModeGameOverPane
        
        endlessModeGameOverLabel = TextLabel(binding = { Localization.getVar("play.endless.gameOver").use() },
                font = PRManiaGame.instance.fontPauseMenuTitle).apply {
            Anchor.Centre.configure(this)
            this.bounds.height.set(350f)
            this.textColor.set(Color(81f / 255, 107f / 255, 1f, 1f))
            this.renderAlign.set(Align.center)
        }
        endlessModeGameOverPane += endlessModeGameOverLabel
        
        uiSceneRoot += textBoxPane.apply {
            Anchor.TopCentre.configure(textBoxPane, offsetY = 64f)
            this.bounds.width.set(1000f)
            this.bounds.height.set(150f)
            this.padding.set(Insets(16f, 16f, 62f, 62f))
            this += textBoxLabel.apply {
                this.markup.set(baseMarkup)
                this.renderAlign.set(Align.center)
                this.textAlign.set(TextAlign.LEFT)
            }
            this += textBoxInputLabel.apply {
                this.renderAlign.set(Align.right)
                this.bounds.width.set(48f)
                this.bounds.height.set(48f)
                Anchor.BottomRight.configure(this, offsetX = 48f, offsetY = 10f)
            }
        }
    }

    fun fireSkillStar() {
        skillStarSpinAnimation = 1f
        skillStarPulseAnimation = 2f
    }

    fun resetAnimations() {
        skillStarSpinAnimation = 0f
        skillStarPulseAnimation = 0f
    }

    fun render(batch: SpriteBatch, engine: Engine) {
        tmpMatrix.set(batch.projectionMatrix)
        val camera = this.camera
        camera.update()
        batch.projectionMatrix = camera.combined
        batch.begin()

        // Blending for framebuffers w/ transparency in format. Assumes premultiplied
//        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
//                GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        
        // Background
        worldBackground.render(batch, engine, camera)

        // Entities
        val entityRenderTime = System.nanoTime()
        var entitiesRendered = 0
        val camWidth = camera.viewportWidth * camera.zoom
        val camHeight = camera.viewportHeight * camera.zoom
        val leftEdge = camera.position.x - camWidth / 2f
        val bottomEdge = camera.position.y - camHeight / 2f
        val currentTileset = this.tileset
        
        tmpRect2.set(leftEdge, bottomEdge, camWidth, camHeight)
        this.entitiesRenderedLastCall = 0
        world.sortEntitiesByRenderOrder()
        world.entities.forEach { entity ->
            val convertedVec = convertWorldToScreen(tmpVec.set(entity.position))
            tmpRect.set(convertedVec.x, convertedVec.y, entity.renderWidth, entity.renderHeight)
            // Only render entities that are in scene
            if (tmpRect.intersects(tmpRect2)) {
                entitiesRendered++
                entity.render(this, batch, currentTileset, engine)
            }
        }
        this.entitiesRenderedLastCall = entitiesRendered
        this.entityRenderTimeNano = System.nanoTime() - entityRenderTime

        // UI
        batch.projectionMatrix = uiCamera.combined

        if (renderUI) {
            renderUI(batch, engine)
        }
        
//        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        batch.end()
        batch.projectionMatrix = tmpMatrix
    }

    private fun renderUI(batch: SpriteBatch, engine: Engine) {
        val inputter = engine.inputter

        moreTimesVar.set(inputter.practice.moreTimes.getOrCompute())
        val uiSheet: PackedSheet = AssetRegistry["tileset_ui"]

        // Skill star
        val skillStarInput = inputter.skillStarBeat
        if (skillStarInput.isFinite() && showSkillStarSetting) {
            if (skillStarSpinAnimation > 0) {
                skillStarSpinAnimation -= Gdx.graphics.deltaTime / 1f
                if (skillStarSpinAnimation < 0)
                    skillStarSpinAnimation = 0f
            }
            if (skillStarPulseAnimation > 0) {
                skillStarPulseAnimation -= Gdx.graphics.deltaTime / 0.5f
                if (skillStarPulseAnimation < 0)
                    skillStarPulseAnimation = 0f
            } else {
                // Pulse before skill star input
                val threshold = 0.1f
                for (i in 0 until 4) {
                    val beatPoint = engine.tempos.beatsToSeconds(skillStarInput - i)
                    if (engine.seconds in beatPoint..beatPoint + threshold) {
                        skillStarPulseAnimation = 0.5f
                        break
                    }
                }
            }

            val texColoured = uiSheet["skill_star"]
            val texGrey = uiSheet["skill_star_grey"]

            val scale = Interpolation.exp10.apply(1f, 2f, (skillStarPulseAnimation).coerceAtMost(1f))
            val rotation = Interpolation.exp10Out.apply(0f, 360f, 1f - skillStarSpinAnimation)
            batch.draw(if (inputter.skillStarGotten.getOrCompute()) texColoured else texGrey,
                    1184f, 32f, 32f, 32f, 64f, 64f, scale, scale, rotation)
        }

        val textBox = engine.activeTextBox
        textBoxPane.visible.set(textBox != null)
        if (textBox != null) {
            textBoxLabel.text.set(textBox.textBox.text)
            textBoxInputLabel.text.set(if (textBox.secondsTimer > 0f) "" else {
                if (textBox.isADown || MathHelper.getSawtoothWave(1.25f) < 0.25f)
                    RodinSpecialChars.FILLED_A else RodinSpecialChars.BORDERED_A
            })
        }

        val challenge = inputter.challenge
        if (challenge.goingForPerfect) {
            perfectPane.visible.set(true)
            challenge.hit = (challenge.hit - Gdx.graphics.deltaTime / (if (challenge.failed) 0.5f else 0.125f)).coerceIn(0f, 1f)

            perfectIconFlash.opacity.set(if (challenge.failed) 0f else challenge.hit)
            perfectIcon.visible.set(!challenge.failed)
            perfectIconFailed.visible.set(challenge.failed)

            if (challenge.failed && challenge.hit > 0f) {
                val maxShake = 3
                val x = MathUtils.randomSign() * MathUtils.random(0, maxShake).toFloat()
                val y = MathUtils.randomSign() * MathUtils.random(0, maxShake).toFloat()
                perfectIconFailed.bounds.x.set(x)
                perfectIconFailed.bounds.y.set(y)
            } else {
                perfectIconFailed.bounds.x.set(0f)
                perfectIconFailed.bounds.y.set(0f)
            }
        } else {
            perfectPane.visible.set(false)
        }

        val clearText = inputter.practice.clearText
        if (clearText > 0f) {
            val normalScale = 1f
            val transitionEnd = 0.15f
            val transitionStart = 0.2f
            val scale: Float = when (val progress = 1f - clearText) {
                in 0f..transitionStart -> {
                    Interpolation.exp10Out.apply(normalScale * 2f, normalScale, progress / transitionStart)
                }
                in (1f - transitionEnd)..1f -> {
                    Interpolation.exp10Out.apply(normalScale, normalScale * 1.5f, (progress - (1f - transitionEnd)) / transitionEnd)
                }
                else -> normalScale
            }
            val alpha: Float = when (val progress = 1f - clearText) {
                in 0f..transitionStart -> {
                    Interpolation.exp10Out.apply(0f, 1f, progress / transitionStart)
                }
                in (1f - transitionEnd)..1f -> {
                    Interpolation.exp10Out.apply(1f, 0f, (progress - (1f - transitionEnd)) / transitionEnd)
                }
                else -> 1f
            }
            val white: Float = when (val progress = 1f - clearText) {
                in 0f..transitionStart * 0.75f -> {
                    Interpolation.linear.apply(1f, 0f, progress / (transitionStart * 0.75f))
                }
                else -> 0f
            }

            val paintboxFont = PRManiaGame.instance.fontGamePracticeClear
            paintboxFont.useFont { font ->
                val camera = uiCamera
                font.scaleMul(scale)
                font.setColor(1f, 1f, MathUtils.lerp(0.125f, 1f, white), alpha)
                font.drawCompressed(batch, Localization.getValue("practice.clear"),
                        0f, camera.viewportHeight / 2f + font.capHeight / 2, camera.viewportWidth, Align.center)
                font.scaleMul(1f / scale)
            }

            val newValue = (clearText - Gdx.graphics.deltaTime / 1.5f).coerceAtLeast(0f)
            inputter.practice.clearText = newValue
        }

        if (showEndlessModeScore.getOrCompute()) {
            val endlessScore = engine.inputter.endlessScore
            currentEndlessLives.set(endlessScore.lives.getOrCompute())
            val oldScore = currentEndlessScore.getOrCompute()
            val newScore = endlessScore.score.getOrCompute()
            if (oldScore != newScore) {
                currentEndlessScore.set(newScore)
                val scaleVar = endlessModeScoreLabelScaleXY
                if (newScore > oldScore) {
                    val newScale = 1.25f
                    scaleVar.set(newScale)
                    uiSceneRoot.animations.enqueueAnimation(Animation(Interpolation.pow5In, 0.25f, newScale, 1f, delay = 0.15f), scaleVar)
                } else {
                    uiSceneRoot.animations.cancelAnimationFor(scaleVar)
                    scaleVar.set(1f)
                }
            }

            endlessModeGameOverPane.visible.set(endlessScore.gameOverUIShown.getOrCompute())
        }

        uiSceneRoot.renderAsRoot(batch)
    }

    fun getDebugString(): String {
        return """e: ${world.entities.size}  r: ${entitiesRenderedLastCall} (${(entityRenderTimeNano) / 1_000_000f} ms)
"""
    }
}