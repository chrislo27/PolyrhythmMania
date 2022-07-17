package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockDeployRod
import polyrhythmmania.editor.block.BlockDespawnPattern
import polyrhythmmania.editor.block.storymode.BlockDeployRodStoryMode
import polyrhythmmania.engine.input.InputTimingRestriction


class TestStoryDefectiveGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    init {
        engine.modifiers.defectiveRodsMode.enabled.set(true)
    }

    override fun initialize() {
        super.initialize()
        
        val oldDeploy = container.blocks.filterIsInstance<BlockDeployRod>()
        val newDeploy: List<BlockDeployRodStoryMode> = oldDeploy.map { block ->
            BlockDeployRodStoryMode(block.engine).apply { 
                this.beat = block.beat
                this.rowData.rowSetting.set(block.rowData.rowSetting.getOrCompute())
                
                this.defective.set(true)
            }
        }
        container.removeBlocks(oldDeploy)
        container.addBlocks(newDeploy)
    }
}