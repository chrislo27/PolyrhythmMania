package polyrhythmmania.sidemodes

import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class LoopingEvent(engine: Engine, val duration: Float, val block: (engine: Engine, startBeat: Float) -> Boolean)
    : Event(engine) {

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val continueLoop = block.invoke(engine, this.beat)
        if (continueLoop) {
            engine.addEvent(LoopingEvent(engine, duration, block).also {
                it.beat = this.beat + duration
            })
        }
    }
}

class LoopingEventBlock(engine: Engine, val duration: Float, val block: (engine: Engine, startBeat: Float) -> Boolean)
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