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
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.EventRowBlockSpawn
import polyrhythmmania.world.Row
import java.util.*


open class BlockSpawnPattern(engine: Engine) : Block(engine, BLOCK_TYPES) {

    companion object {
        val ROW_COUNT: Int = 10
        val ALLOWED_CUBE_TYPES: List<CubeType> by lazy { CubePatternData.GENERAL_CUBE_TYPES }
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)

        /**
         * Returns a float array of size [rowArray] indicating the timing offset in beats for each block.
         */
        fun computeTimingOffsetsForRow(rowArray: List<CubeType>, beatsPerBlock: Float): FloatArray {
            /*
             - Let f(x) be the default time a block shows up based on its position x: f(x) = x * beatsPerBlock
             - Pistons always show up at the time based on their position. Always uses f(x)
             - Platforms:
               - Figure out the contiguous section of platforms
               - If the section ends with a piston, then the section of platforms appears when that piston does (use f(x) where x = piston)
               - Otherwise, the section appears when the first platform in the section appears (use f(x) where x = first of the section)
             - Tail end handling:
               - If the very last index, l...
                 - is a piston: fill in the rest of the blocks 2 time-units later (use f(l + 2)) (Piston X Platform Platform Platform...)
                 - is a platform: fill in the rest of the blocks at the same time of l (NOT f(l), but whatever was computed for it earlier) (Platform Platform Platform...)
                 - is nothing: fill in the rest of the blocks at the time of the first end index (use f(l + 1))
             */
            
            val timings = FloatArray(rowArray.size) { idx -> idx * beatsPerBlock }

            // Search for contiguous platform sections
            var index = 0
            val maxSearchIndex = rowArray.size
            while (index in 0 until maxSearchIndex) {
                val type = rowArray[index]
                if (type == CubeType.PLATFORM && index < maxSearchIndex - 1) {
                    // Find contiguous section of platforms
                    val startIndexOfPlatforms = index
                    var subindex = startIndexOfPlatforms + 1
                    var endType: CubeType = rowArray[subindex]
                    while (subindex in 0 until maxSearchIndex) { // While subindex is still in searchable region
                        endType = rowArray[subindex]
                        if (endType != CubeType.PLATFORM) break // Break at the first non-platform
                        subindex++
                    }
                    // At the end, subindex = index where we ran out or it is not a platform, endType = rowArray[subindex]

                    when (endType) {
                        CubeType.PISTON -> {
                            // Ends with a piston. All timings for the platform match the piston's
                            for (i in index until subindex) {
                                timings[i] = timings[subindex]
                            }
                        }
                        else -> {
                            // Set all timings for the platform to the firs tof the platform
                            for (i in index until subindex) {
                                timings[i] = timings[index]
                            }
                        }
                    }
                    
                    index = subindex + 1 // Jump ahead past the ending. Don't need to check subindex since it won't be a platform
                } else {
                    index++
                }
            }
            
            return timings
        } 
    }

    var patternData: CubePatternData = CubePatternData(ROW_COUNT, ALLOWED_CUBE_TYPES, CubeType.NONE).also { patternData ->
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
    
    open fun getBeatsPerBlock(): Float = 0.5f

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

    private fun compileRow(beat: Float, rowArray: List<CubeType>, row: Row, pistonType: EntityPiston.Type, beatsPerBlock: Float): List<Event> {
        val events = mutableListOf<Event>()
        val timings: FloatArray = computeTimingOffsetsForRow(rowArray, beatsPerBlock)

        // Populate events and handle tail end platforms
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
                    CubeType.NO_CHANGE, CubeType.RETRACT_PISTON -> {
                        // NO-OP
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
                    CubeType.PLATFORM -> {
                        val next = ind + 1
                        if (next < row.length) {
                            events += EventRowBlockSpawn(engine, row, next, EntityPiston.Type.PLATFORM,
                                    beat + b, affectThisIndexAndForward = true)
                        }
                    }
                    CubeType.NONE -> {
                        val next = ind + 1
                        if (next < row.length) {
                            events += EventRowBlockSpawn(engine, row, next, EntityPiston.Type.PLATFORM,
                                    beat + next * beatsPerBlock, affectThisIndexAndForward = true)
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
        this.patternData = CubePatternData.readFromJson(obj, ALLOWED_CUBE_TYPES) ?: this.patternData 
        val disableTailEndValue = obj.get("disableTailEnd")
        if (disableTailEndValue != null && disableTailEndValue.isBoolean) {
            disableTailEnd.set(disableTailEndValue.asBoolean())
        }
    }
}