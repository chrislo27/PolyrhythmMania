package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import io.github.chrislo27.paintbox.util.gdxutils.intersects
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.Entity
import polyrhythmmania.world.World


class WorldRenderer(val world: World, var tileset: Tileset) {

    companion object {
        val comparatorRenderOrder: Comparator<Entity> = object : Comparator<Entity> {
            private val tmpVec = Vector3()
            private val tmpVec2 = Vector3()
            override fun compare(o1: Entity, o2: Entity): Int {
                val xyz1 = o1.position.x - o1.position.z - o1.position.y
                val xyz2 = o2.position.x - o2.position.z - o2.position.y
                return -xyz1.compareTo(xyz2)
            }
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
    private val tmpMatrix: Matrix4 = Matrix4()

    var entitiesRenderedLastCall: Int = 0
        private set

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
        world.entities.forEach { entity ->
            val convertedVec = convertWorldToScreen(tmpVec.set(entity.position))
            tmpRect.set(convertedVec.x, convertedVec.y, entity.getRenderWidth(), entity.getRenderHeight())
            // Only render entities that are in scene
            if (tmpRect.intersects(tmpRect2)) {
                entitiesRendered++
                entity.render(this, batch, tileset, engine)
            }
        }
        this.entitiesRenderedLastCall = entitiesRendered

        batch.end()
        batch.projectionMatrix = tmpMatrix

    }

    fun getDebugString(): String {
        return """e: ${world.entities.size}  r: ${entitiesRenderedLastCall}

"""
    }
}