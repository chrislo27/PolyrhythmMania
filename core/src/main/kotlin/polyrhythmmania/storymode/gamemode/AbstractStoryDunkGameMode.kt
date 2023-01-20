package polyrhythmmania.storymode.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.GenericBlock
import polyrhythmmania.gamemodes.EventDeployRodDunk
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.texturepack.TexturePackSource
import polyrhythmmania.world.tileset.TilesetPalette


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
        return GenericBlock(engine, shouldOffsetEventsByThisBlockBeat = false) {
            listOf(EventDeployRodDunk(this.engine, this.beat))
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

    override fun createGlobalContainerSettings(): GlobalContainerSettings {
        return super.createGlobalContainerSettings().copy(forceTexturePack = ForceTexturePack.FORCE_GBA)
    }
}