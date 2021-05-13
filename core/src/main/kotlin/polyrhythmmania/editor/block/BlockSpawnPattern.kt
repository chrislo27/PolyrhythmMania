package polyrhythmmania.editor.block

import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EntityRowBlock
import polyrhythmmania.world.EventRowBlockExtend
import polyrhythmmania.world.EventRowBlockSpawn
import java.util.*


class BlockSpawnPattern(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {

    companion object {
        val ROW_COUNT: Int = 8
    }

    enum class Type {
        NONE, PLATFORM, PISTON
    }

    val rowATypes: Array<Type> = Array(ROW_COUNT) { Type.NONE }
    val rowDpadTypes: Array<Type> = Array(ROW_COUNT) { Type.NONE }

    init {
        this.width = 4f
        this.defaultText.bind { Localization.getVar("block.spawnPattern.name").use() }
    }

    init {
        // FIXME debug, remove later
        rowATypes[0] = Type.PISTON
        rowATypes[2] = Type.PISTON
        rowATypes[4] = Type.PISTON
        rowATypes[6] = Type.PISTON
//        rowATypes[8] = Type.PLATFORM
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()
        var anyA = false
        var anyDpad = false
        for (index in 0 until ROW_COUNT) {
            val startBeat = b + index * 0.5f
            var type = rowATypes[index]
            if (type != Type.NONE) {
                anyA = true
                events += EventRowBlockSpawn(editor.engine, editor.world.rowA, index,
                        if (type == Type.PLATFORM) EntityRowBlock.Type.PLATFORM else EntityRowBlock.Type.PISTON_A,
                        startBeat)
//                if (type == Type.PISTON)
//                    events += EventRowBlockExtend(editor.engine, editor.world.rowA, index, startBeat + 4f)
            }
            type = rowDpadTypes[index]
            if (type != Type.NONE) {
                anyDpad = true
                events += EventRowBlockSpawn(editor.engine, editor.world.rowDpad, index,
                        if (type == Type.PLATFORM) EntityRowBlock.Type.PLATFORM else EntityRowBlock.Type.PISTON_DPAD,
                        startBeat)
//                if (type == Type.PISTON)
//                    events += EventRowBlockExtend(editor.engine, editor.world.rowDpad, index, startBeat + 4f)
            }
        }

        if (anyA) {
            events += EventRowBlockSpawn(editor.engine, editor.world.rowA, ROW_COUNT,
                    EntityRowBlock.Type.PLATFORM,
                    b + ROW_COUNT * 0.5f, affectThisIndexAndForward = true)
        }
        if (anyDpad) {
            events += EventRowBlockSpawn(editor.engine, editor.world.rowDpad, ROW_COUNT,
                    EntityRowBlock.Type.PLATFORM,
                    b + ROW_COUNT * 0.5f, affectThisIndexAndForward = true)
        }

        return events
    }

    override fun copy(): BlockSpawnPattern {
        return BlockSpawnPattern(editor).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.rowATypes[i] = this.rowATypes[i]
                it.rowDpadTypes[i] = this.rowDpadTypes[i]
            }
        }
    }
}