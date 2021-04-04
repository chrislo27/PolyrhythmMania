package polyrhythmmania.world

import com.badlogic.gdx.math.Vector3
import javafx.scene.Camera
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.WorldRenderer
import java.util.concurrent.CopyOnWriteArrayList


class World {
    
    companion object {
        val DEFAULT_ROW_LENGTH: Int = 16
        val DEFAULT_ROW_COUNT: Int = 2
    }

    val entities: List<Entity> = CopyOnWriteArrayList()
    
    val rows: List<Row> = listOf(Row(this, DEFAULT_ROW_LENGTH, 5, 2, 0), Row(this, DEFAULT_ROW_LENGTH, 5, 2, -3))
    val rowA: Row inline get() = rows[0]
    val rowDpad: Row inline get() = rows[1]
    
    init {
        populateScene()
        rows.forEach { row ->
            row.rowBlocks.forEach { rb -> addEntity(rb) }
        }
    }

    fun addEntity(entity: Entity) {
        if (entity !in entities) {
            (entities as MutableList).add(entity)
        }
    }

    fun removeEntity(entity: Entity) {
        (entities as MutableList).remove(entity)
    }
    
    fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        entities.forEach { entity ->
            entity.engineUpdate(engine, beat, seconds)
        }
        (entities as MutableList).removeIf { it.isKilled }
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
        for (x in -1..4) {
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
                val ent: Entity = EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), if (z == -7) 2f else 3f, z.toFloat())
                })
            }
        }

    }

}