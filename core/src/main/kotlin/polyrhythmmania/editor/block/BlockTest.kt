package polyrhythmmania.editor.block

import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Event
import java.util.*


class BlockTest(editor: Editor) : Block(editor, EnumSet.allOf(BlockType::class.java)) {
    init {
        this.width = 4f
        this.defaultText.set("test block")
    }

    override fun compileIntoEvents(): List<Event> = emptyList()

    override fun copy(): BlockTest {
        return BlockTest(editor).also {
            this.copyBaseInfoTo(it)
        }
    }
}