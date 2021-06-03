package polyrhythmmania.world

import com.badlogic.gdx.Gdx
import io.github.chrislo27.paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.soundsystem.BeadsSound


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

    init {
        this.width = 0.125f
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (currentBeat < this.beat + this.width) {
            if (row.rowBlocks.any { it.active }) {
                engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_despawn"))
            }
        }
    }

    override fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {
        entity.despawn(percentage)
    }
}

class EventRowBlockRetract(engine: Engine, row: Row, index: Int, startBeat: Float,
                           affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {
    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)

        if (currentBeat < this.beat + 0.125f) {
            engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_retract"))
        }
    }

    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        entity.retract()
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
