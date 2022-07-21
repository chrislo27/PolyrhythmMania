package polyrhythmmania.editor.block.contextmenu

import com.badlogic.gdx.graphics.g2d.TextureRegion
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.block.SpotlightActionType
import polyrhythmmania.editor.block.data.SpotlightActionData
import polyrhythmmania.editor.pane.EditorPane


class SpotlightActionMenuPane(editorPane: EditorPane, data: SpotlightActionData, clearType: SpotlightActionType, beatIndexStart: Int)
    : AbstractPatternMenuPane<SpotlightActionType, SpotlightActionData>(editorPane, data, clearType, beatIndexStart) {

    override fun getTexRegForType(type: SpotlightActionType, palette: Palette, isA: Boolean): TextureRegion {
        return when (type) {
            SpotlightActionType.TURN_OFF -> palette.blockFlatNoneRegion
            SpotlightActionType.TURN_ON -> palette.blockFlatSpotlightRegion
            SpotlightActionType.NO_CHANGE -> palette.blockFlatNoChangeRegion
        }
    }
}
