package polyrhythmmania.world

import com.badlogic.gdx.math.Vector3
import javafx.scene.Camera
import polyrhythmmania.world.render.WorldRenderer


class World {

    val entities: List<Entity> = ArrayList()

    init {
        populateScene()
    }

    fun addEntity(entity: Entity) {
        if (entity !in entities) {
            (entities as MutableList).add(entity)
        }
    }

    fun removeEntity(entity: Entity) {
        (entities as MutableList).remove(entity)
    }

    fun sortEntitiesByRenderOrder() {
        (entities as MutableList).sortWith(WorldRenderer.comparatorRenderOrder)
    }

    private fun populateScene() {
        // Main floor
        for (x in -1..20) {
            for (z in -6 until 3) {
                val ent: Entity = if (z == 0 || z == -3) {
                    EntityPlatform(this, x == 4)
                } else EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 1f, z.toFloat())
                })
            }
        }

        // Raised platforms
        for (x in -1..20) {
            if (x in 5..12) continue
            for (zMul in 0..1) {
                val ent: Entity = EntityPlatform(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 2f, zMul * -3f)
                })
            }
        }

        // Bottom floor
        for (x in -1..20) {
            for (z in 3..7) {
                val ent: Entity = EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
        }

        // Upper steps
        for (x in -1..20) {
            for (z in -7 downTo -9) {
                val ent: Entity = EntityCube(this, false)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), if (z == -7) 2f else 3f, z.toFloat())
                })
            }
        }

        // Remove entities that are not in scene
        val tmpVec = Vector3()
        val leftEdge = 0f
        val rightEdge = (5 * 16f / 9f)
        val topEdge = (5f)
        val bottomEdge = (0f)
        entities.filterIsInstance<SimpleRenderedEntity>().filterNot { ent ->
            val convertedVec = tmpVec.set(ent.position).apply {
                val oldX = this.x
                val oldY = this.y
                val oldZ = this.z
                this.x = oldX / 2f + oldZ / 2f
                this.y = oldX * (8f / 32f) + (oldY - 3f) * 0.5f - oldZ * (8f / 32f)
                this.z = 0f
            }

            ((convertedVec.x + ent.getWidth()) in leftEdge..rightEdge || (convertedVec.x) in leftEdge..rightEdge)
                    && ((convertedVec.y + ent.getHeight()) in bottomEdge..topEdge || convertedVec.y in bottomEdge..topEdge)
        }.toList().forEach { ent ->
            removeEntity(ent)
        }
    }

}