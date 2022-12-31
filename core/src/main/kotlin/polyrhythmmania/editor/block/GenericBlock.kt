package polyrhythmmania.editor.block

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class GenericBlock(engine: Engine, val compileIntoEvents: Block.() -> List<Event>)
    : Block(engine, EnumSet.allOf(BlockType::class.java)) {
    
    override fun compileIntoEvents(): List<Event> = this.compileIntoEvents.invoke(this)

    override fun copy(): GenericBlock {
        return GenericBlock(engine, compileIntoEvents)
    }
}
