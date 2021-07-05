package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityPiston
import java.util.*


class BlockSelectiveSpawnPattern(engine: Engine) : Block(engine, EnumSet.of(BlockType.INPUT)) {

    companion object {
        val ROW_COUNT: Int = 10
    }

    var patternData: PatternBlockData = PatternBlockData(ROW_COUNT)
        private set
    val overwriteA: Var<Boolean> = Var(true)
    val overwriteDpad: Var<Boolean> = Var(true)

    init {
        this.width = 0.5f
        this.defaultText.bind { Localization.getVar("block.selectiveSpawn.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        val world = engine.world
        events += compileRow(b, patternData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A)
        events += compileRow(b, patternData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD)

        return events
    }

    private fun compileRow(beat: Float, rowArray: Array<CubeType>, row: Row, pistonType: EntityPiston.Type): List<Event> {
        val events = mutableListOf<Event>()
        
        val ow = (if (pistonType == EntityPiston.Type.PISTON_A) overwriteA else overwriteDpad).getOrCompute()
        rowArray.forEachIndexed { index, type ->
            if (type == CubeType.NONE) {
                if (ow) {
                    events += EventRowBlockDespawn(engine, row, index, beat, false)
                }
            } else {
                events += EventRowBlockSpawn(engine, row, index,
                        if (type == CubeType.PLATFORM) EntityPiston.Type.PLATFORM else pistonType, beat)
            }
        }

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.selectiveSpawn"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            patternData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(overwriteA,
                    Localization.getValue("blockContextMenu.selectiveSpawn.overwriteA"),
                    editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(overwriteDpad,
                    Localization.getValue("blockContextMenu.selectiveSpawn.overwriteDpad"),
                    editor.editorPane.palette.markup))
        }
    }

    override fun copy(): BlockSelectiveSpawnPattern {
        return BlockSelectiveSpawnPattern(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
            it.overwriteA.set(this.overwriteA.getOrCompute())
            it.overwriteDpad.set(this.overwriteDpad.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        patternData.writeToJson(obj)
        obj.add("overwriteA", overwriteA.getOrCompute())
        obj.add("overwriteDpad", overwriteDpad.getOrCompute())
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.patternData = PatternBlockData.readFromJson(obj) ?: this.patternData
        overwriteA.set(obj.getBoolean("overwriteA", true))
        overwriteDpad.set(obj.getBoolean("overwriteDpad", true))
    }
}