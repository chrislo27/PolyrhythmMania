package polyrhythmmania.editor.block

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class GenericBlock(engine: Engine, val shouldOffsetEventsByThisBlockBeat: Boolean, val compileIntoEvents: Block.() -> List<Event>)
    : Block(engine, EnumSet.allOf(BlockType::class.java)) {
    
    override fun compileIntoEvents(): List<Event> = this.compileIntoEvents.invoke(this).onEach { evt ->
        if (shouldOffsetEventsByThisBlockBeat) {
            evt.beat += this.beat
        }
    }

    override fun copy(): GenericBlock {
        return GenericBlock(engine, shouldOffsetEventsByThisBlockBeat, compileIntoEvents)
    }
}
