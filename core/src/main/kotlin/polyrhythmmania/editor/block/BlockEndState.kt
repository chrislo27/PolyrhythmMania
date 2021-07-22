package polyrhythmmania.editor.block

import polyrhythmmania.Localization
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventEndState
import java.util.*


class BlockEndState(engine: Engine) : Block(engine, BlockEndState.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.allOf(BlockType::class.java)
    }
    
    init {
        this.width = 2f
        val text = Localization.getVar("block.endState.name")
        this.defaultText.bind { text.use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        return listOf(EventEndState(engine, this.beat))
    }

    override fun copy(): BlockEndState {
        return BlockEndState(engine).also {
            this.copyBaseInfoTo(it)
        }
    }

}