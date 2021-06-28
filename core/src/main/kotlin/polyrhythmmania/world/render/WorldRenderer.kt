package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.util.gdxutils.intersects
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.ui.TextboxPane
import polyrhythmmania.world.Entity
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
    private val textBoxLabel: TextLabel = TextLabel("", font = PRManiaGame.instance.fontGameTextbox)
    
    init {
        uiSceneRoot += textBoxPane.apply { 
            Anchor.TopCentre.configure(textBoxPane, offsetY = 64f)
            this.bounds.width.set(1000f)
            this.bounds.height.set(150f)
            this.padding.set(Insets(16f, 16f, 32f, 32f))
            this += textBoxLabel.apply { 
                this.renderAlign.set(Align.center)
                this.textAlign.set(TextAlign.LEFT)
            }
        }
    }
    
    fun fireSkillStar() {
        skillStarSpinAnimation = 1f
        skillStarPulseAnimation = 2f
    }
    
    fun resetSkillStar() {
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
            val uiSheet: PackedSheet = AssetRegistry["tileset_ui"]

            // Skill star
            val skillStarInput = engine.inputter.skillStarBeat
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
                batch.draw(if (engine.inputter.skillStarGotten.getOrCompute()) texColoured else texGrey,
                        1184f, 32f, 32f, 32f, 64f, 64f, scale, scale, rotation)
            }
            
            val textBox = engine.activeTextBox
            textBoxPane.visible.set(textBox != null)
            if (textBox != null) {
                textBoxLabel.text.set(textBox.textBox.text)
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