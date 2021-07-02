package polyrhythmmania.world

import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.TilesetConfig
import polyrhythmmania.world.render.WorldRenderer
import java.util.concurrent.CopyOnWriteArrayList


class World {
    
    companion object {
        val DEFAULT_ROW_LENGTH: Int = 16
        val DEFAULT_ROW_COUNT: Int = 2
    }
    
    val tilesetConfig: TilesetConfig = TilesetConfig.createGBA1TilesetConfig()
    var worldMode: WorldMode = WorldMode.POLYRHYTHM
    
    val entities: List<Entity> = CopyOnWriteArrayList()
    
    val rows: List<Row> = listOf(
            Row(this, DEFAULT_ROW_LENGTH, 5, 2, 0, false),
            Row(this, DEFAULT_ROW_LENGTH, 5, 2, -3, true)
    )
    val rowA: Row get() = rows[0]
    val rowDpad: Row get() = rows[1]
    
    init {
        resetWorld()
    }

    fun addEntity(entity: Entity) {
        if (entity !in entities) {
            (entities as MutableList).add(entity)
        }
    }

    fun removeEntity(entity: Entity) {
        (entities as MutableList).remove(entity)
    }
    
    fun clearEntities() {
        (entities as MutableList).clear()
    }
    
    fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        val entities = this.entities as MutableList
        entities.forEach { entity ->
            entity.engineUpdate(engine, beat, seconds)
        }
        
        // Remove killed entities
        val toRemove = entities.filter { it.isKilled }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { it.onRemovedFromWorld(engine) }
            entities.removeAll(toRemove)
        }
    }

    fun sortEntitiesByRenderOrder() {
        (entities as MutableList).sortWith(WorldRenderer.comparatorRenderOrder)
    }
    
    // ------------------------------------------------------------------------------------------------------
    
    fun resetWorld() {
        populateScene()
        rows.forEach(Row::initWithWorld)
    }

    private fun populateScene() {
        clearEntities()
        
        // Main floor
        for (x in -1..20) {
            for (z in -6 until 3) {
                val ent: Entity = if (z == 0 || z == -3) {
                    EntityPlatform(this, x == 4)
                } else if ((z == 1 || z == -2) && x >= 5 && rows.any { r -> x in r.startX until (r.startX + r.length) }) {
                    EntityCube(this, withBorder = true)
                } else EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 1f, z.toFloat())
                })
            }
        }

        // Raised platforms
        for (x in -3..4) {
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
        
        // Button signs
        val signs = mutableListOf<EntitySign>()
        signs += EntitySign(this, EntitySign.Type.A).apply {
            this.position.set(7f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.BO).apply {
            this.position.set(8f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.TA).apply {
            this.position.set(9f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.N).apply {
            this.position.set(10f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.DPAD).apply {
            this.position.set(7f, 2f, -2f)
        }
        signs += EntitySign(this, EntitySign.Type.BO).apply {
            this.position.set(8f, 2f, -2f)
        }
        signs += EntitySign(this, EntitySign.Type.TA).apply {
            this.position.set(9f, 2f, -2f)
        }
        signs += EntitySign(this, EntitySign.Type.N).apply {
            this.position.set(10f, 2f, -2f)
        }
        signs.forEach { sign ->
            sign.position.x += (12 / 32f)
            sign.position.z += (8 / 32f)
            addEntity(sign)
        }


        val settings = PRManiaGame.instance.settings
        if (settings.showInputFeedbackBar.getOrCompute()) {
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.LEFT, EntityInputFeedback.BARELY_COLOUR, 0).apply {
                this.position.set(7f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, 1).apply {
                this.position.set(8f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.ACE_COLOUR, 2).apply {
                this.position.set(9f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, 3).apply {
                this.position.set(10f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.RIGHT, EntityInputFeedback.BARELY_COLOUR, 4).apply {
                this.position.set(11f, 1f, 3f)
            })
        }
        
    }

}