package polyrhythmmania.editor.block

import polyrhythmmania.engine.Engine
import java.util.*


class BlockDeployRodStoryMode(engine: Engine, blockTypes: EnumSet<BlockType> = BlockDeployRod.BLOCK_TYPES)
    : BlockDeployRod(engine, blockTypes) {

    init {
        this.defaultText.set("Rod (SM)")
    }
    
    override fun copy(): BlockDeployRodStoryMode {
        return BlockDeployRodStoryMode(engine).also {
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
        }
    }
}