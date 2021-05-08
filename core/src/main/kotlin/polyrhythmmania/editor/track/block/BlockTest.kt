package polyrhythmmania.editor.track.block

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.BlockType
import java.util.*


class BlockTest(editor: Editor) : Block(editor, EnumSet.allOf(BlockType::class.java)) {
    init {
        this.width = 4f
        this.defaultText.set("test block")
    }
}