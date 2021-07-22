package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
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


class BlockSelectiveSpawnPattern(engine: Engine) : Block(engine, BlockSelectiveSpawnPattern.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)
        val ROW_COUNT: Int = 10
        val ALLOWED_CUBE_TYPES: List<CubeType> by lazy { PatternBlockData.SELECTIVE_SPAWN_CUBE_TYPES }
        val ALLOWED_TAIL_END_TYPES: List<CubeType> = listOf(CubeType.NO_CHANGE, CubeType.NONE, CubeType.PLATFORM)
    }

    var patternData: PatternBlockData = PatternBlockData(ROW_COUNT, ALLOWED_CUBE_TYPES, CubeType.NO_CHANGE)
        private set
    var tailEndData: PatternBlockData = PatternBlockData(2, ALLOWED_TAIL_END_TYPES, CubeType.NO_CHANGE)
        private set

    init {
        this.width = 0.5f
        this.defaultText.bind { Localization.getVar("block.selectiveSpawn.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        val world = engine.world
        events += compileRow(b, patternData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A, 0, false)
        events += compileRow(b, patternData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD, 0, false)
        
        events += compileRow(b, tailEndData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A, ROW_COUNT, true)
        events += compileRow(b, tailEndData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD, ROW_COUNT, true)

        return events
    }

    private fun compileRow(beat: Float, rowArray: Array<CubeType>, row: Row, pistonType: EntityPiston.Type,
                           indexOffset: Int, shouldLastAffectAll: Boolean): List<Event> {
        val events = mutableListOf<Event>()
        
        rowArray.forEachIndexed { index, type ->
            val affectThisIndexAndForward = shouldLastAffectAll && index == rowArray.size - 1
            when (type) {
                CubeType.NO_CHANGE -> { /* Do nothing */}
                CubeType.NONE -> {
                    events += EventRowBlockDespawn(engine, row, index + indexOffset, beat, affectThisIndexAndForward = affectThisIndexAndForward)
                }
                CubeType.PISTON, CubeType.PISTON_OPEN -> {
                    events += EventRowBlockSpawn(engine, row, index + indexOffset, pistonType, beat,
                            startPistonExtended = type == CubeType.PISTON_OPEN, affectThisIndexAndForward = affectThisIndexAndForward)
                }
                CubeType.PLATFORM -> {
                    events += EventRowBlockSpawn(engine, row, index + indexOffset, EntityPiston.Type.PLATFORM, beat, affectThisIndexAndForward = affectThisIndexAndForward)
                }
            }
        }

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.selectiveSpawn"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            patternData.createMenuItems(editor, CubeType.NO_CHANGE, 0).forEach { ctxmenu.addMenuItem(it) }
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.selectiveSpawn.tailEnd"), editor.editorPane.palette.markup))
            tailEndData.createMenuItems(editor, CubeType.NO_CHANGE, ROW_COUNT / 2).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockSelectiveSpawnPattern {
        return BlockSelectiveSpawnPattern(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
            for (i in 0 until it.tailEndData.rowCount) {
                it.tailEndData.rowATypes[i] = this.tailEndData.rowATypes[i]
                it.tailEndData.rowDpadTypes[i] = this.tailEndData.rowDpadTypes[i]
            }
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        patternData.writeToJson(obj)
        tailEndData.writeToJson(obj, "tailEndData")
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.patternData = PatternBlockData.readFromJson(obj, ALLOWED_CUBE_TYPES) ?: this.patternData
        this.tailEndData = PatternBlockData.readFromJson(obj, ALLOWED_TAIL_END_TYPES, "tailEndData") ?: this.tailEndData 
        
        // Legacy system loading
        val overwriteA = obj.get("overwriteA")?.takeIf { it.isBoolean }?.asBoolean() ?: true
        val overwriteDpad = obj.get("overwriteDpad")?.takeIf { it.isBoolean }?.asBoolean() ?: true
        if (!overwriteA) {
            val types = this.patternData.rowATypes
            types.forEachIndexed { index, cubeType -> 
                if (cubeType == CubeType.NONE) {
                    types[index] = CubeType.NO_CHANGE
                }
            }
        }
        if (!overwriteDpad) {
            val types = this.patternData.rowDpadTypes
            types.forEachIndexed { index, cubeType ->
                if (cubeType == CubeType.NONE) {
                    types[index] = CubeType.NO_CHANGE
                }
            }
        }
    }
}