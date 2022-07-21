package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.data.CubePatternData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityPiston
import java.util.*


class BlockSelectiveSpawnPattern(engine: Engine) : Block(engine, BlockSelectiveSpawnPattern.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)
        val ROW_COUNT: Int = 10
        val ALLOWED_CUBE_TYPES: List<CubeType> by lazy { CubePatternData.SELECTIVE_SPAWN_CUBE_TYPES }
        val ALLOWED_TAIL_END_TYPES: List<CubeType> = listOf(CubeType.NO_CHANGE, CubeType.NONE, CubeType.PLATFORM)
    }

    var patternData: CubePatternData = CubePatternData(ROW_COUNT, ALLOWED_CUBE_TYPES, CubeType.NO_CHANGE)
        private set
    var tailEndData: CubePatternData = CubePatternData(2, ALLOWED_TAIL_END_TYPES, CubeType.NO_CHANGE)
        private set
    val isSilent: BooleanVar = BooleanVar(false)

    init {
        this.width = 0.5f
        this.defaultText.bind { Localization.getVar("block.selectiveSpawn.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        val world = engine.world
        val silent = this.isSilent.get()
        events += compileRow(b, patternData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A, 0, false, silent)
        events += compileRow(b, patternData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD, 0, false, silent)
        
        events += compileRow(b, tailEndData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A, ROW_COUNT, true, silent)
        events += compileRow(b, tailEndData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD, ROW_COUNT, true, silent)

        return events
    }

    private fun compileRow(beat: Float, rowArray: MutableList<CubeType>, row: Row, pistonType: EntityPiston.Type,
                           indexOffset: Int, shouldLastAffectAll: Boolean, silent: Boolean): List<Event> {
        val events = mutableListOf<Event>()
        
        rowArray.forEachIndexed { index, type ->
            val affectThisIndexAndForward = shouldLastAffectAll && index == rowArray.size - 1
            when (type) {
                CubeType.NO_CHANGE -> { /* Do nothing */ }
                CubeType.NONE -> {
                    events += EventRowBlockDespawn(engine, row, index + indexOffset, beat, affectThisIndexAndForward = affectThisIndexAndForward).apply { 
                        this.silent = silent
                    }
                }
                CubeType.PISTON, CubeType.PISTON_OPEN -> {
                    events += EventRowBlockSpawn(engine, row, index + indexOffset, pistonType, beat,
                            startPistonExtended = type == CubeType.PISTON_OPEN, affectThisIndexAndForward = affectThisIndexAndForward).apply {
                        this.silent = silent
                    }
                }
                CubeType.PLATFORM -> {
                    events += EventRowBlockSpawn(engine, row, index + indexOffset, EntityPiston.Type.PLATFORM, beat, affectThisIndexAndForward = affectThisIndexAndForward).apply {
                        this.silent = silent
                    }
                }
                CubeType.RETRACT_PISTON -> {
                    events += EventRowBlockRetract(engine, row, index + indexOffset, beat, affectThisIndexAndForward = affectThisIndexAndForward).apply {
                        this.silent = silent
                    }
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
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(isSilent,
                    Localization.getValue("blockContextMenu.selectiveSpawn.silent"),
                    editor.editorPane.palette.markup).apply {
                this.createTooltip = { 
                    it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.selectiveSpawn.silent.tooltip")))
                }
            })
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
            it.isSilent.set(this.isSilent.get())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        patternData.writeToJson(obj)
        tailEndData.writeToJson(obj, "tailEndData")
        if (isSilent.get()) {
            obj.add("silentMode", true)
        }
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.patternData = CubePatternData.readFromJson(obj, ALLOWED_CUBE_TYPES) ?: this.patternData
        this.tailEndData = CubePatternData.readFromJson(obj, ALLOWED_TAIL_END_TYPES, "tailEndData") ?: this.tailEndData
        this.isSilent.set(obj.getBoolean("silentMode", false))
        
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