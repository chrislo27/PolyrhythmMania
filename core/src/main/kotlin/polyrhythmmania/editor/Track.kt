package polyrhythmmania.editor

import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType


data class Track(val id: TrackID, val allowedTypes: Set<BlockType>) {

    fun acceptsBlock(block: Block): Boolean {
        val blocksTypes = block.blockTypes
        val thisTypes = this.allowedTypes
        return if (blocksTypes.size < thisTypes.size) {
            blocksTypes.any { it in thisTypes }
        } else {
            thisTypes.any { it in blocksTypes }
        }
    }

}