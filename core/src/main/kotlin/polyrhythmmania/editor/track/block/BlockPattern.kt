package polyrhythmmania.editor.track.block

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.BlockType
import java.util.*


class BlockPattern(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {
    init {
        this.width = 8f
        this.defaultText.set("Pattern")
    }
}