package polyrhythmmania.editor.block

import polyrhythmmania.editor.Editor
import java.util.*


class BlockTest(editor: Editor) : Block(editor, EnumSet.allOf(BlockType::class.java)) {
    init {
        this.width = 4f
        this.defaultText.set("test block")
    }
}