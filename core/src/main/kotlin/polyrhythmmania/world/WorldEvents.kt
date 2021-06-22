package polyrhythmmania.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.registry.AssetRegistry
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.world.render.TilesetConfig


abstract class EventRowBlock(engine: Engine, val row: Row, val index: Int, startBeat: Float,
                             val affectThisIndexAndForward: Boolean)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    protected open fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {}
    protected open fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {}
    protected open fun entityOnEnd(entity: EntityRowBlock, currentBeat: Float) {}

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (index in 0 until row.length) {
            if (index < row.length - 1 && affectThisIndexAndForward) {
                for (i in index until row.length) {
                    entityOnStart(row.rowBlocks[i], currentBeat)
                }
            } else {
                entityOnStart(row.rowBlocks[index], currentBeat)
            }
        } else {
            row.rowBlocks.forEach { entity ->
                entityOnStart(entity, currentBeat)
            }
        }
    }

    override fun onUpdate(currentBeat: Float) {
        super.onUpdate(currentBeat)
        val percent = getBeatPercentage(currentBeat).coerceIn(0f, 1f)
        if (index in 0 until row.length) {
            if (index < row.length - 1 && affectThisIndexAndForward) {
                for (i in index until row.length) {
                    entityOnUpdate(row.rowBlocks[i], currentBeat, percent)
                }
            } else {
                entityOnUpdate(row.rowBlocks[index], currentBeat, percent)
            }
        } else {
            row.rowBlocks.forEach { entity ->
                entityOnUpdate(entity, currentBeat, percent)
            }
        }
    }

    override fun onEnd(currentBeat: Float) {
        super.onEnd(currentBeat)
        if (index in 0 until row.length) {
            if (index < row.length - 1 && affectThisIndexAndForward) {
                for (i in index until row.length) {
                    entityOnEnd(row.rowBlocks[i], currentBeat)
                }
            } else {
                entityOnEnd(row.rowBlocks[index], currentBeat)
            }
        } else {
            row.rowBlocks.forEach { entity ->
                entityOnEnd(entity, currentBeat)
            }
        }
    }
}

class EventRowBlockSpawn(engine: Engine, row: Row, index: Int, val type: EntityRowBlock.Type, startBeat: Float,
                         affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {
    init {
        this.width = 0.125f
    }

    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        entity.type = type
        entity.pistonState = EntityRowBlock.PistonState.RETRACTED

        if (currentBeat < this.beat + this.width) {
            when (this.type) {
                EntityRowBlock.Type.PLATFORM -> {
                }
                EntityRowBlock.Type.PISTON_A -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_spawn_a"))
                EntityRowBlock.Type.PISTON_DPAD -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_spawn_d"))
            }
        }
    }

    override fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {
        entity.spawn(percentage)
    }
}

class EventRowBlockDespawn(engine: Engine, row: Row, index: Int, startBeat: Float,
                           affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {

    /**
     * Holds the result of [EntityRowBlock.despawn] from [entityOnUpdate] for checking if the sound should play.
     */
    private var anyBlocksAffected: Boolean = false
    
    init {
        this.width = 0.125f
    }

    override fun onStart(currentBeat: Float) {
        this.anyBlocksAffected = false
        super.onStart(currentBeat)
    }

    override fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {
        val anyAffected = entity.despawn(percentage)
        if (anyAffected) this.anyBlocksAffected = true
    }

    override fun onUpdate(currentBeat: Float) {
        val oldAnyBlocksAffected = anyBlocksAffected
        super.onUpdate(currentBeat)
        if (!oldAnyBlocksAffected && anyBlocksAffected) { // Condition fulfilled if entities were updated on the first update
            if (currentBeat < this.beat + this.width) {
                if (row.rowBlocks.any { it.active }) {
                    engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_despawn"))
                }
            }
        }
    }
}

class EventRowBlockRetract(engine: Engine, row: Row, index: Int, startBeat: Float,
                           affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {
    
    /**
     * Holds the result of [EntityRowBlock.retract] from [entityOnUpdate] for checking if the sound should play.
     */
    private var anyBlocksAffected: Boolean = false
    
    override fun onStart(currentBeat: Float) {
        this.anyBlocksAffected = false
        super.onStart(currentBeat)

        if (anyBlocksAffected && currentBeat < this.beat + 0.125f) {
            engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_retract"))
        }
    }

    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        val result = entity.retract()
        if (result) {
            anyBlocksAffected = true
        }
    }
}

class EventRowBlockExtend(engine: Engine, row: Row, index: Int, startBeat: Float,
                          affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {

    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        if (engine.autoInputs) {
            entity.fullyExtend(engine, currentBeat)
        }
    }
}

class EventDeployRod(engine: Engine, val row: Row, startBeat: Float) : Event(engine) {
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.world.addEntity(EntityRod(engine.world, this.beat, row))
    }
}

class EventEndState(engine: Engine, startBeat: Float) : Event(engine) {
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        Gdx.app.postRunnable {
            engine.endSignalReceived.set(true)
        }
    }
}

class EventTilesetChange(engine: Engine, startBeat: Float, width: Float,
                         val tilesetCopy: TilesetConfig)
    : Event(engine) {
    
    private data class ColorTarget(val start: Color, val end: Color, val current: Color = start.cpy()) {
        fun lerp(percentage: Float): Color {
            current.set(start).lerp(end, percentage)
            return current
        }
    }
    
    private val colorTargets: Map<String, ColorTarget> = tilesetCopy.allMappings.associate { 
        it.id to ColorTarget(Color(1f, 1f, 1f, 1f), it.color.getOrCompute().cpy()) 
    }
    
    init {
        this.beat = startBeat
        this.width = width
    }

    override fun onStartContainer(container: Container, currentBeat: Float) {
        super.onStartContainer(container, currentBeat)
        val tileset = container.renderer.tileset
        tilesetCopy.allMappings.forEach { m ->
            val id = m.id
            val target = colorTargets.getValue(id)
            target.start.set(m.tilesetGetter.invoke(tileset).getOrCompute())
        }
    }

    override fun onUpdateContainer(container: Container, currentBeat: Float) {
        super.onUpdateContainer(container, currentBeat)
        val percentage = getBeatPercentage(currentBeat).coerceIn(0f, 1f)
        val tileset = container.renderer.tileset
        tilesetCopy.allMappings.forEach { m ->
            val id = m.id
            val target = colorTargets.getValue(id)
            m.tilesetGetter.invoke(tileset).set(target.lerp(percentage))
        }
    }
}
