package polyrhythmmania.editor.block

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class BlockTest(engine: Engine) : Block(engine, EnumSet.allOf(BlockType::class.java)) {
    init {
        this.width = 4f
        this.defaultText.set("test block")
    }

    override fun compileIntoEvents(): List<Event> = emptyList()

    override fun copy(): BlockTest {
        return BlockTest(engine).also {
            this.copyBaseInfoTo(it)
        }
    }
}