package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
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
        val ROW_COUNT: Int = 10
        val ALLOWED_CUBE_TYPES: List<CubeType> by lazy { PatternBlockData.SELECTIVE_SPAWN_CUBE_TYPES }
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)
    }

    var patternData: PatternBlockData = PatternBlockData(ROW_COUNT, ALLOWED_CUBE_TYPES, CubeType.NO_CHANGE)
        private set
//    val overwriteA: Var<Boolean> = Var(true)
//    val overwriteDpad: Var<Boolean> = Var(true)

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
        
        rowArray.forEachIndexed { index, type ->
            when (type) {
                CubeType.NO_CHANGE -> { /* Do nothing */}
                CubeType.NONE -> {
                    events += EventRowBlockDespawn(engine, row, index, beat, false)
                }
                CubeType.PISTON, CubeType.PISTON_OPEN -> {
                    events += EventRowBlockSpawn(engine, row, index, pistonType, beat,
                            startPistonExtended = type == CubeType.PISTON_OPEN)
                }
                CubeType.PLATFORM -> {
                    events += EventRowBlockSpawn(engine, row, index, EntityPiston.Type.PLATFORM, beat)
                }
            }
        }

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.selectiveSpawn"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            patternData.createMenuItems(editor, CubeType.NO_CHANGE).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockSelectiveSpawnPattern {
        return BlockSelectiveSpawnPattern(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        patternData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.patternData = PatternBlockData.readFromJson(obj, ALLOWED_CUBE_TYPES) ?: this.patternData
        
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