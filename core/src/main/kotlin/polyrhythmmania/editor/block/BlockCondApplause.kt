package polyrhythmmania.editor.block


import com.eclipsesource.json.JsonObject
import paintbox.font.TextAlign
import paintbox.registry.AssetRegistry
import paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.PlayerLike
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row
import java.util.*


class BlockCondApplause(engine: Engine) : Block(engine, BlockCondApplause.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT, BlockType.FX)
    }

    val rowData: RowBlockData = RowBlockData(RowSetting.ONLY_A)

    init {
        this.width = 1f
        val text = Localization.getVar("block.condApplause.name")
        this.defaultText.bind { text.use() }
        this.defaultTextSecondLine.bind { "[font=rodin]${rowData.rowSetting.use().stringRepresentation}[]" }
        this.secondLineTextAlign = TextAlign.RIGHT
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return listOf(EventCondApplause(engine, b, rowData.rowSetting.getOrCompute()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            rowData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockCondApplause {
        return BlockCondApplause(engine).also {
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        rowData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        rowData.readFromJson(obj)
    }
}

class EventCondApplause(engine: Engine, startBeat: Float, val rowSetting: RowSetting) : Event(engine) {

    init {
        this.beat = startBeat
    }
    
    private fun doesValidRodExistOnRow(currentBeat: Float, row: Row): Boolean {
        val world = engine.world
        return world.entities.filterIsInstance<EntityRodPR>().any { 
            val results = it.inputTracker.results
            it.row == row && it.acceptingInputs && currentBeat - it.deployBeat >= 4.25f && results.size > 0 && results.none { it.inputScore == InputScore.MISS }
        }
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        
        val rowA = engine.world.rowA
        val rowDpad = engine.world.rowDpad
        val shouldPlay: Boolean = when (rowSetting) {
            RowSetting.ONLY_A -> doesValidRodExistOnRow(currentBeat, rowA)
            RowSetting.ONLY_DPAD -> doesValidRodExistOnRow(currentBeat, rowDpad)
            RowSetting.BOTH -> doesValidRodExistOnRow(currentBeat, rowA) && doesValidRodExistOnRow(currentBeat, rowDpad)
        }
        if (shouldPlay) {
            val beadsSound = AssetRegistry.get<BeadsSound>("sfx_applause")
            engine.soundInterface.playAudio(beadsSound)
        }
    }

}