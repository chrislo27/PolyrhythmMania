package polyrhythmmania.world.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import io.github.chrislo27.paintbox.util.MathHelper
import polyrhythmmania.world.Entity
import polyrhythmmania.world.World


class WorldRenderer(val world: World, var tileset: Tileset) {

    companion object {
        val comparatorRenderOrder: Comparator<Entity> = Comparator { o1: Entity, o2: Entity ->
            if (o1.position.z < o2.position.z) {
                -1
            } else if (o1.position.z > o2.position.z) {
                1
            } else {
                if (o1.position.x > o2.position.x) {
                    -1
                } else if (o1.position.x < o2.position.x) {
                    1
                } else {
                    if (o1.position.y < o2.position.y) {
                        -1
                    } else if (o1.position.y > o2.position.y) {
                        1
                    } else {
                        0
                    }
                }
            }
        }
    }
    
    private val camera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, 7.5f, 5f)
        position.set(zoom * viewportWidth / 2.0f, zoom * viewportHeight / 2.0f, 0f)
        update()
    }
    private val tmpMatrix: Matrix4 = Matrix4()

    fun render(batch: SpriteBatch) {
        tmpMatrix.set(batch.projectionMatrix)
        camera.update()
        batch.projectionMatrix = camera.combined
        batch.begin()

        world.sortEntitiesByRenderOrder()

        world.entities.forEach { entity ->
            entity.render(this, batch, tileset)
        }

        batch.end()
        batch.projectionMatrix = tmpMatrix
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

}