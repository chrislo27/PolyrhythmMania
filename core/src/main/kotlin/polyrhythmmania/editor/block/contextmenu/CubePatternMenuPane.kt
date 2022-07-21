package polyrhythmmania.editor.block.contextmenu

import com.badlogic.gdx.graphics.g2d.TextureRegion
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.data.CubePatternData
import polyrhythmmania.editor.pane.EditorPane


class CubePatternMenuPane(editorPane: EditorPane, data: CubePatternData, clearType: CubeType, beatIndexStart: Int)
    : AbstractPatternMenuPane<CubeType, CubePatternData>(editorPane, data, clearType, beatIndexStart) {

    override fun getTexRegForType(type: CubeType, palette: Palette, isA: Boolean): TextureRegion {
        return when (type) {
            CubeType.NONE -> palette.blockFlatNoneRegion
            CubeType.NO_CHANGE -> palette.blockFlatNoChangeRegion
            CubeType.PLATFORM -> palette.blockFlatPlatformRegion
            CubeType.PISTON -> if (isA) palette.blockFlatPistonARegion else palette.blockFlatPistonDpadRegion
            CubeType.PISTON_OPEN -> if (isA) palette.blockFlatPistonAOpenRegion else palette.blockFlatPistonDpadOpenRegion
            CubeType.RETRACT_PISTON -> palette.blockFlatRetractRegion
        }
    }
}
