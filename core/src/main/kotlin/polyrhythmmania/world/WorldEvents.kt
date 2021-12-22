package polyrhythmmania.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import polyrhythmmania.container.Container
import polyrhythmmania.container.TexturePackSource
import polyrhythmmania.engine.*
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.tileset.TilesetPalette
import kotlin.math.max
import kotlin.math.min


abstract class EventRowBlock(engine: Engine, val row: Row, val index: Int, startBeat: Float,
                             val affectThisIndexAndForward: Boolean)
    : AudioEvent(engine) {

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

class EventRowBlockSpawn(engine: Engine, row: Row, index: Int, val type: EntityPiston.Type, startBeat: Float,
                         affectThisIndexAndForward: Boolean = false,
                         val startPistonExtended: Boolean = false,
) : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {
    
    init {
        this.width = 0.125f
    }

    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        entity.type = type
        entity.pistonState = EntityPiston.PistonState.RETRACTED
        if (startPistonExtended) {
            entity.fullyExtendVanity(engine, currentBeat)
        } else {
            entity.retract()
        }
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        if (min(actualBeat, atBeat) < this.beat + this.width) {
            when (this.type) {
                EntityPiston.Type.PLATFORM -> {
                }
                EntityPiston.Type.PISTON_A -> engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_spawn_a"), SoundInterface.SFXType.NORMAL)
                EntityPiston.Type.PISTON_DPAD -> engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_spawn_d"), SoundInterface.SFXType.NORMAL)
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

    private var shouldPlaySound: Boolean = false
    
    init {
        this.width = 0.125f
    }
    
    override fun onStart(currentBeat: Float) {
        shouldPlaySound = false
        super.onStart(currentBeat)
    }
    
    override fun entityOnUpdate(entity: EntityRowBlock, currentBeat: Float, percentage: Float) {
        entity.despawn(percentage)
        shouldPlaySound = true
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        if (min(actualBeat, atBeat) < this.beat + this.width) {
            if (row.rowBlocks.any { it.active } || shouldPlaySound) {
                engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_despawn"), SoundInterface.SFXType.NORMAL)
            }
        }
    }
}

class EventRowBlockRetract(engine: Engine, row: Row, index: Int, startBeat: Float,
                           affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {

    private var shouldPlaySound: Boolean = false
    
    override fun onStart(currentBeat: Float) {
        shouldPlaySound = false
        super.onStart(currentBeat)
    }
    
    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        if (entity.retract()) {
            shouldPlaySound = true
        }
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        if (min(actualBeat, atBeat) < this.beat + 0.125f) {
            if (row.rowBlocks.any { it.pistonState != EntityPiston.PistonState.RETRACTED } || shouldPlaySound) {
                engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_retract"), SoundInterface.SFXType.NORMAL)
            }
        }
    }
}

/**
 * Used for debugging only.
 */
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
        engine.world.addEntity(EntityRodPR(engine.world, this.beat, row))
        
        if (engine.statisticsMode == StatisticsMode.REGULAR) {
            GlobalStats.rodsDeployed.increment()
            GlobalStats.rodsDeployedPolyrhythm.increment()
        }
    }
}

class EventDeployRodEndless(engine: Engine, val row: Row, startBeat: Float, val lifeLostVar: BooleanVar)
    : Event(engine) {
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.world.addEntity(EntityRodPR(engine.world, this.beat, row, lifeLost = lifeLostVar))

        if (engine.statisticsMode == StatisticsMode.REGULAR) {
            GlobalStats.rodsDeployed.increment()
            GlobalStats.rodsDeployedPolyrhythm.increment()
        }
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

class EventPaletteChange(engine: Engine, startBeat: Float, width: Float,
                         val tilesetCopy: TilesetPalette, val pulseMode: Boolean, val reverse: Boolean)
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
            if (m.enabled.get()) {
                val id = m.id
                val target = colorTargets.getValue(id)
                target.start.set(m.tilesetGetter.invoke(tileset).getOrCompute())
            }
        }
    }

    override fun onUpdateContainer(container: Container, currentBeat: Float) {
        super.onUpdateContainer(container, currentBeat)
        if (container.globalSettings.forceTilesetPalette != ForceTilesetPalette.NO_FORCE) return
        
        var percentage = getBeatPercentage(currentBeat).coerceIn(0f, 1f)
        if (reverse) {
            percentage = 1f - percentage
        }
        val tileset = container.renderer.tileset
        tilesetCopy.allMappings.forEach { m ->
            if (m.enabled.get()) {
                val id = m.id
                val target = colorTargets.getValue(id)

                if (!pulseMode) {
                    target.lerp(percentage)
                } else {
                    if (percentage <= 0.5f) {
                        target.lerp((percentage * 2).coerceIn(0f, 1f))
                    } else {
                        target.lerp(1f - ((percentage - 0.5f) * 2).coerceIn(0f, 1f))
                    }
                }

                val lerped: Color = target.current
                m.tilesetGetter.invoke(tileset).set(lerped)
            }
        }
    }
}

class EventTextbox(engine: Engine, startBeat: Float, duration: Float, val textbox: TextBox)
    : Event(engine) {

    init {
        this.beat = startBeat
        this.width = duration
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (textbox.requiresInput) {
            if (currentBeat in this.beat..(this.beat + 0.25f)) {
                engine.setActiveTextbox(textbox)
            }
        } else {
            if (currentBeat in this.beat..(this.beat + this.width)) {
                engine.setActiveTextbox(textbox)
            }
        }
    }

    override fun onEnd(currentBeat: Float) {
        super.onEnd(currentBeat)
        if (!textbox.requiresInput) {
            engine.removeActiveTextbox(unpauseSoundInterface = true, runTextboxOnComplete = true)
        }
    }
}

class EventChangeTexturePack(engine: Engine, startBeat: Float, val newSource: TexturePackSource)
    : Event(engine) {

    init {
        this.beat = startBeat
        this.width = 0f
    }

    override fun onStartContainer(container: Container, currentBeat: Float) {
        super.onStartContainer(container, currentBeat)
        if (container.globalSettings.forceTexturePack == ForceTexturePack.NO_FORCE) {
            container.setTexturePackFromSource(newSource)
        }
    }
}
