package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.EventRowBlockSpawn
import polyrhythmmania.world.Row
import java.util.*


open class BlockSpawnPattern(engine: Engine) : Block(engine, BLOCK_TYPES) {

    companion object {
        val ROW_COUNT: Int = 10
        val ALLOWED_CUBE_TYPES: List<CubeType> by lazy { PatternBlockData.GENERAL_CUBE_TYPES }
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)
    }

    var patternData: PatternBlockData = PatternBlockData(ROW_COUNT, ALLOWED_CUBE_TYPES, CubeType.NONE).also { patternData ->
        // Default settings.
        patternData.rowATypes[0] = CubeType.PISTON
        patternData.rowATypes[2] = CubeType.PISTON
        patternData.rowATypes[4] = CubeType.PISTON
        patternData.rowATypes[6] = CubeType.PISTON
        patternData.rowATypes[8] = CubeType.PLATFORM
        patternData.rowATypes[9] = CubeType.PLATFORM
    }
        private set
    val disableTailEnd: BooleanVar = BooleanVar(false)

    init {
        this.width = 4f
        this.defaultText.bind { Localization.getVar("block.spawnPattern.name").use() }
    }
    
    protected open fun getBeatsPerBlock(): Float = 0.5f

    fun compileIntoEvents(rowDelayA: Float, rowDelayDpad: Float): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        val world = engine.world
        val beatsPerBlock = getBeatsPerBlock()
        events += compileRow(b + rowDelayA, patternData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A, beatsPerBlock)
        events += compileRow(b + rowDelayDpad, patternData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD, beatsPerBlock)

        return events
    }

    override fun compileIntoEvents(): List<Event> {
        return compileIntoEvents(0f, 0f)
    }

    private fun compileRow(beat: Float, rowArray: Array<CubeType>, row: Row, pistonType: EntityPiston.Type, beatsPerBlock: Float): List<Event> {
        val events = mutableListOf<Event>()

        /*
        - Pistons show up at the time based on their position
        - Platforms show up when:
          - If the section encounters a piston, when said piston appears
          - Otherwise, when the FIRST platform should appear
        - If the very last index
          - is a piston: fill in the rest of the blocks 2 after
          - is a platform: fill in the rest of the blocks at the same time
          - is nothing: fill in the rest of the blocks at the time of the first end index
        */

        val timings: FloatArray = FloatArray(rowArray.size) { it * beatsPerBlock }

        var index: Int = 0
        while (index in 0 until rowArray.size) {
            val type = rowArray[index]
            if (type == CubeType.PLATFORM && index < rowArray.size - 1) {
                // Find contiguous section
                var subindex = index + 1
                var endType: CubeType = rowArray[subindex]
                while (subindex in 0 until rowArray.size) {
                    endType = rowArray[subindex]
                    if (endType != CubeType.PLATFORM) break
                    subindex++
                }

                when (endType) {
                    CubeType.PISTON -> {
                        for (i in index until subindex) {
                            timings[i] = timings[subindex]
                        }
                        index = subindex
                    }
                    else -> {
                        for (i in index until subindex) {
                            timings[i] = timings[index]
                        }
                        index = subindex
                    }
                }
            }

            index++
        }

        var anyNotNone = false
        timings.forEachIndexed { ind, b ->
            val cube = rowArray[ind]
            if (cube != CubeType.NONE) {
                anyNotNone = true
                events += EventRowBlockSpawn(engine, row, ind,
                        if (cube == CubeType.PLATFORM) EntityPiston.Type.PLATFORM else pistonType,
                        beat + b)
            }

            if (ind == timings.size - 1 && anyNotNone && !disableTailEnd.get()) {
                when (cube) {
                    CubeType.NO_CHANGE -> {}
                    CubeType.NONE -> {
                        val next = ind + 1
                        if (next < row.length) {
                            events += EventRowBlockSpawn(engine, row, next, EntityPiston.Type.PLATFORM,
                                    beat + next * beatsPerBlock, affectThisIndexAndForward = true)
                        }
                    }
                    CubeType.PLATFORM -> {
                        val next = ind + 1
                        if (next < row.length) {
                            events += EventRowBlockSpawn(engine, row, next, EntityPiston.Type.PLATFORM,
                                    beat + b, affectThisIndexAndForward = true)
                        }
                    }
                    CubeType.PISTON, CubeType.PISTON_OPEN -> {
                        val next = ind + 2
                        if (next < row.length) {
                            events += EventRowBlockSpawn(
                                engine, row, next, EntityPiston.Type.PLATFORM,
                                beat + next * beatsPerBlock, affectThisIndexAndForward = true,
                                startPistonExtended = cube == CubeType.PISTON_OPEN
                            )
                        }
                    }
                }
            }
        }

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spawnPattern"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            patternData.createMenuItems(editor, CubeType.NONE, 0).forEach { ctxmenu.addMenuItem(it) }
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(disableTailEnd,
                    Localization.getValue("blockContextMenu.spawnPattern.deployTailEnd"),
                    editor.editorPane.palette.markup))
        }
    }

    override fun copy(): BlockSpawnPattern {
        return BlockSpawnPattern(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
            it.disableTailEnd.set(this.disableTailEnd.get())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        patternData.writeToJson(obj)
        if (disableTailEnd.get()) {
            obj.add("disableTailEnd", true)
        }
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.patternData = PatternBlockData.readFromJson(obj, ALLOWED_CUBE_TYPES) ?: this.patternData 
        val disableTailEndValue = obj.get("disableTailEnd")
        if (disableTailEndValue != null && disableTailEndValue.isBoolean) {
            disableTailEnd.set(disableTailEndValue.asBoolean())
        }
    }
}