package polyrhythmmania.sidemodes

import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class LoopingEvent(engine: Engine, val duration: Float, val block: (engine: Engine, startBeat: Float) -> Unit)
    : Event(engine) {

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        block.invoke(engine, this.beat)
        engine.addEvent(LoopingEvent(engine, duration, block).also {
            it.beat = this.beat + duration
        })
    }
}

class LoopingEventBlock(engine: Engine, val duration: Float, val block: (engine: Engine, startBeat: Float) -> Unit)
    : Block(engine, EnumSet.allOf(BlockType::class.java)) {

    override fun compileIntoEvents(): List<Event> {
        return listOf(
                LoopingEvent(engine, duration, block)
        )
    }

    override fun copy(): LoopingEventBlock {
        throw NotImplementedError()
    }
}