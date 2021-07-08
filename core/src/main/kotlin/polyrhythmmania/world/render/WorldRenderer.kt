package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.*
import com.badlogic.gdx.utils.Align
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
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.util.MathHelper
import paintbox.util.gdxutils.drawCompressed
import paintbox.util.gdxutils.intersects
import paintbox.util.gdxutils.scaleMul
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.ui.TextboxPane
import polyrhythmmania.util.RodinSpecialChars
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.World


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
    
    var renderUI: Boolean = true
    
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
    
    init {
        val baseMarkup = Markup(mapOf("prmania_icons" to PRManiaGame.instance.fontIcons), TextRun(PRManiaGame.instance.fontGameTextbox, ""))
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
        camera.update()
        batch.projectionMatrix = camera.combined
        batch.begin()

        var entitiesRendered = 0
        this.entitiesRenderedLastCall = 0
        world.sortEntitiesByRenderOrder()

        val camWidth = camera.viewportWidth * camera.zoom
        val camHeight = camera.viewportHeight * camera.zoom
        val leftEdge = camera.position.x - camWidth / 2f
//        val rightEdge = camera.position.x + camWidth
//        val topEdge = camera.position.y + camHeight
        val bottomEdge = camera.position.y - camHeight / 2f
        tmpRect2.set(leftEdge, bottomEdge, camWidth, camHeight)
        val currentTileset = this.tileset
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
        
        batch.projectionMatrix = uiCamera.combined

        if (renderUI) {
            val inputter = engine.inputter
            
            moreTimesVar.set(inputter.practice.moreTimes.getOrCompute())
            val uiSheet: PackedSheet = AssetRegistry["tileset_ui"]

            // Skill star
            val skillStarInput = inputter.skillStarBeat
            if (skillStarInput.isFinite()) {
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
            
            uiSceneRoot.renderAsRoot(batch)
        }

        batch.end()
        batch.projectionMatrix = tmpMatrix

    }

    fun getDebugString(): String {
        return """e: ${world.entities.size}  r: ${entitiesRenderedLastCall}

"""
    }
}