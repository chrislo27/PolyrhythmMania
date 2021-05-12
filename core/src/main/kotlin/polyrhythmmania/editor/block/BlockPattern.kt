package polyrhythmmania.editor.block

import polyrhythmmania.editor.Editor
import java.util.*


class BlockPattern(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {
    init {
        this.width = 8f
        this.defaultText.set("Pattern")
    }
}