package polyrhythmmania.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.binding.BooleanVar
import paintbox.registry.AssetRegistry
import paintbox.util.Vector3Stack
import polyrhythmmania.container.Container
import polyrhythmmania.world.texturepack.TexturePackSource
import polyrhythmmania.engine.*
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TilesetPalette
import kotlin.math.min


abstract class EventRowBlock(engine: Engine, val row: Row, val index: Int, startBeat: Float,
                             val affectThisIndexAndForward: Boolean)
    : AudioEvent(engine) {
    
    var silent: Boolean = false

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

class EventRowBlockSpawn(
        engine: Engine, row: Row, index: Int, val type: EntityPiston.Type, startBeat: Float,
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
        if (!this.silent && min(actualBeat, atBeat) < this.beat + this.width) {
            when (val t = this.type) {
                EntityPiston.Type.PLATFORM -> {
                }
                EntityPiston.Type.PISTON_A, EntityPiston.Type.PISTON_DPAD -> {
                    val snd = AssetRegistry.get<BeadsSound>(if (t == EntityPiston.Type.PISTON_A) "sfx_spawn_a" else "sfx_spawn_d")
                    engine.soundInterface.playAudioNoOverlap(snd, SoundInterface.SFXType.NORMAL)
                }
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
        if (!silent && min(actualBeat, atBeat) < this.beat + this.width) {
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
        if (!silent && min(actualBeat, atBeat) < this.beat + 0.125f) {
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

class EventDeployRod(
        engine: Engine, val row: Row, startBeat: Float,
        val isDefective: Boolean = false,
        val after: (EntityRodPR) -> Unit = {}
) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.world.addEntity(EntityRodPR.createRod(engine.world, this.beat, row, this.isDefective).also(after))
        
        if (engine.areStatisticsEnabled) {
            GlobalStats.rodsDeployed.increment()
            GlobalStats.rodsDeployedPolyrhythm.increment()
            if (this.isDefective) {
                GlobalStats.defectiveRodsDeployed.increment()
            }
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
        engine.world.addEntity(EntityRodPR.createRodForEndless(engine.world, this.beat, row, lifeLostVar))

        if (engine.areStatisticsEnabled) {
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

class EventPaletteChange(
        engine: Engine, startBeat: Float, val paletteTransition: PaletteTransition,
        val tilesetCopy: TilesetPalette
) : Event(engine) {
    
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
        this.width = paletteTransition.duration
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
        
        val percentage = this.paletteTransition.translatePercentage(getBeatPercentage(currentBeat)).coerceIn(0f, 1f)
        val tileset = container.renderer.tileset
        tilesetCopy.allMappings.forEach { m ->
            if (m.enabled.get()) {
                val id = m.id
                val target = colorTargets.getValue(id)
                
                target.lerp(percentage)

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

class EventZoomCamera(
        engine: Engine, startBeat: Float, val transition: PaletteTransition,
        val startZoom: Float, val endZoom: Float
) : Event(engine) {

    private lateinit var camera: OrthographicCamera

    init {
        this.beat = startBeat
        this.width = transition.duration
    }

    override fun onStartContainer(container: Container, currentBeat: Float) {
        super.onStartContainer(container, currentBeat)
        this.camera = container.renderer.camera
        this.camera.zoom = this.startZoom
    }

    override fun onUpdateContainer(container: Container, currentBeat: Float) {
        super.onUpdateContainer(container, currentBeat)

        val percentage = this.transition.translatePercentage(getBeatPercentage(currentBeat)).coerceIn(0f, 1f)
        this.camera.zoom = MathUtils.lerp(this.startZoom, this.endZoom, percentage)
    }

    override fun onEndContainer(container: Container, currentBeat: Float) {
        super.onEndContainer(container, currentBeat)
        this.camera.zoom = this.endZoom
    }
}
