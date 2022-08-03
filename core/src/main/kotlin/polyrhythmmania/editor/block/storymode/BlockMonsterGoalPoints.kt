package polyrhythmmania.editor.block.storymode

import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.modifiers.EventMonsterGoalPoint
import java.util.*


abstract class BlockMonsterGoalPoints(engine: Engine, val start: Boolean) : Block(engine, BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.allOf(BlockType::class.java)
    }
    
    init {
        this.width = 2f
        this.defaultText.set("Monster Goal\n${if (start) "START" else "END"}")
    }
    
    override fun compileIntoEvents(): List<Event> {
        return listOf(EventMonsterGoalPoint(engine, this.beat, this.start))
    }

    abstract override fun copy(): BlockMonsterGoalPoints
}

class BlockMonsterGoalPointStart(engine: Engine) : BlockMonsterGoalPoints(engine, true) {
    override fun copy(): BlockMonsterGoalPointStart {
        return BlockMonsterGoalPointStart(engine).also {
            this.copyBaseInfoTo(it)
        }
    }
}

class BlockMonsterGoalPointEnd(engine: Engine) : BlockMonsterGoalPoints(engine, false) {
    override fun copy(): BlockMonsterGoalPointEnd {
        return BlockMonsterGoalPointEnd(engine).also {
            this.copyBaseInfoTo(it)
        }
    }
}
