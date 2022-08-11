package polyrhythmmania.storymode.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Event
import polyrhythmmania.gamemodes.*
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.texturepack.TexturePackSource
import polyrhythmmania.world.tileset.TilesetPalette
import java.util.*


abstract class AbstractStoryAsmGameMode(
        main: PRManiaGame
) : AbstractStoryGameMode(main) {
    
    protected data class BouncePattern(val block: Block, val numInputs: Int)
    
    init {
        container.world.worldMode = WorldMode(WorldType.Assemble)
        container.texturePackSource.set(TexturePackSource.StockGBA)
        TilesetPalette.createAssembleTilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }
    
    protected fun addBouncePatternsToContainer(list: List<BouncePattern>) {
        engine.inputter.minimumInputCount = list.sumOf { it.numInputs }
        container.addBlocks(list.map { it.block })
    }
    
    protected fun newBouncePattern(startBeat: Float, startOnLeft: Boolean, numBouncesInclFire: Int,
                                   beatsPerBounce: Float = 1f, firstBeatsPerBounce: Float = beatsPerBounce,
                                   rodID: Int = -1): BouncePattern {
        val indices: List<Int> = buildList {
            var goingRight = startOnLeft
            var next: Int = if (numBouncesInclFire == 1) 2 else (if (startOnLeft) 0 else 3)
            
            for (i in 0 until numBouncesInclFire) {
                this += next
                if (goingRight) {
                    next += 1
                    if (next == 3) {
                        goingRight = false
                    }
                } else {
                    next -= 1
                    if (next == 0) {
                        goingRight = true
                    }
                }
            }
        }
        val block = BlockAsmBouncePattern(startBeat, if (startOnLeft) -1 else 999, indices, beatsPerBounce, firstBeatsPerBounce, rodID)
        return BouncePattern(block, block.getNumInputs())
    }

    override fun initialize() {
        container.addBlock(BlockAsmReset(engine))
    }

    protected inner class BlockAsmBouncePattern(
            startBeat: Float, val startIndex: Int, val bounceIndices: List<Int>,
            val beatsPerBounce: Float = 1f, val firstBeatsPerBounce: Float = beatsPerBounce,
            val rodID: Int = -1
    ) : Block(engine, EnumSet.allOf(BlockType::class.java)) {

        init {
            this.beat = startBeat
        }
        
        fun getNumInputs(): Int {
            return bounceIndices.count { it == 2 } + (if (startIndex == 2) 1 else 0)
        }
        
        override fun compileIntoEvents(): List<Event> {
            val list = mutableListOf<Event>()

            val endsWithPlayer = bounceIndices.last() == 2
            var prevIndex = startIndex
            bounceIndices.forEachIndexed { i, targetIndex ->
                list += EventAsmRodBounce(engine, if (i == 0) (beatsPerBounce - firstBeatsPerBounce) else (i * beatsPerBounce),
                        prevIndex, targetIndex, endsWithPlayer && i == bounceIndices.size - 1,
                        timePerBounce = if (i == 0) firstBeatsPerBounce else beatsPerBounce, targetRodID = rodID)
                
                prevIndex = targetIndex
            }

            val startBeat = this.beat - 1
            list.onEach { it.beat += startBeat }
            if (endsWithPlayer) {
                val inputBeat = bounceIndices.size * beatsPerBounce
                list += EventAsmSpawnWidgetHalves(engine, 0f, combineBeat = startBeat + inputBeat)
                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + inputBeat - (beatsPerBounce * 1))
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + inputBeat)
                list += EventAsmPrepareSfx(engine, startBeat + inputBeat - 2)
            }
            
            return list
        }

        override fun copy(): Block = throw NotImplementedError("Copying not supported")
    }
}
