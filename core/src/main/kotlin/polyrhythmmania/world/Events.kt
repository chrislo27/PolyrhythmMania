package polyrhythmmania.world

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


abstract class EventRowBlock(engine: Engine, val row: Row, val index: Int, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    protected open fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {}
    protected open fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {}
    protected open fun entityOnEnd(entity: EntityRowBlock, currentBeat: Float) {}

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (index in 0 until row.length) {
            entityOnStart(row.rowBlocks[index], currentBeat)
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
            entityOnUpdate(row.rowBlocks[index], currentBeat, percent)
        } else {
            row.rowBlocks.forEach { entity ->
                entityOnUpdate(entity, currentBeat, percent)
            }
        }
    }

    override fun onEnd(currentBeat: Float) {
        super.onEnd(currentBeat)
        if (index in 0 until row.length) {
            entityOnEnd(row.rowBlocks[index], currentBeat)
        } else {
            row.rowBlocks.forEach { entity ->
                entityOnEnd(entity, currentBeat)
            }
        }
    }
}

class EventRowBlockSpawn(engine: Engine, row: Row, index: Int, val type: EntityRowBlock.Type, startBeat: Float)
    : EventRowBlock(engine, row, index, startBeat) {
    init {
        this.width = 0.125f
    }
    
    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        entity.type = type
        entity.pistonState = EntityRowBlock.PistonState.RETRACTED
    }

    override fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {
        entity.spawn(percentage)
    }
}

class EventRowBlockDespawn(engine: Engine, row: Row, index: Int, startBeat: Float) : EventRowBlock(engine, row, index, startBeat) {
    init {
        this.width = 0.125f
    }
    
    override fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {
        entity.despawn(percentage)
    }
}

class EventRowBlockRetract(engine: Engine, row: Row, index: Int, startBeat: Float) : EventRowBlock(engine, row, index, startBeat) {
    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        entity.retract()
    }
}

class EventRowBlockExtend(engine: Engine, row: Row, index: Int, startBeat: Float) : EventRowBlock(engine, row, index, startBeat) {
    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        entity.fullyExtend(currentBeat)
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
