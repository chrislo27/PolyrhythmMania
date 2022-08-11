package polyrhythmmania.storymode.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Event
import polyrhythmmania.gamemodes.EventDeployRodDunk
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.texturepack.TexturePackSource
import polyrhythmmania.world.tileset.TilesetPalette
import java.util.*


abstract class AbstractStoryDunkGameMode(
        main: PRManiaGame
) : AbstractStoryGameMode(main) {
    
    init {
        container.world.worldMode = WorldMode(WorldType.Dunk)
        container.texturePackSource.set(TexturePackSource.StockGBA)
        TilesetPalette.createGBA1TilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }
    
    protected open fun newDunkPattern(startBeat: Float): Block {
        return object : Block(engine, EnumSet.noneOf(BlockType::class.java)) {
            override fun compileIntoEvents(): List<Event> {
                return listOf(EventDeployRodDunk(this.engine, this.beat))
            }

            override fun copy(): Block = throw NotImplementedError()
        }.apply { 
            this.beat = startBeat - 2
        }
    }
    
    protected fun addDunkPatternBlocks(list: List<Block>) {
        engine.inputter.minimumInputCount = list.size
        container.addBlocks(list)
    }

    override fun initialize() {
    }
}