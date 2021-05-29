package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventEndState
import java.util.*


class BlockEndState(editor: Editor) : Block(editor, EnumSet.allOf(BlockType::class.java)) {

    init {
        this.width = 2f
        val text = Localization.getVar("block.endState.name")
        this.defaultText.bind { text.use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        return listOf(EventEndState(editor.engine, this.beat))
    }

    override fun copy(): BlockEndState {
        return BlockEndState(editor).also {
            this.copyBaseInfoTo(it)
        }
    }

}