package polyrhythmmania.editor.track

import polyrhythmmania.editor.track.block.Block


data class Track(val id: String, val allowedTypes: Set<BlockType>) {

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