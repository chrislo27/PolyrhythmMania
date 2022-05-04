package polyrhythmmania.gamemodes.practice

import com.badlogic.gdx.math.MathUtils
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.world.EntityRodPR
import java.util.*


/**
 * A practice section is a looping section of the game. The loop is escaped when the "more times" counter is zero.
 * 
 * The "more times" counter is initially set to a positive value, and is decremented
 * when a "group" of inputs are completed.
 * 
 * The section has an initialization block, a loop block, and an end block. The loop block returns a list of
 * inputs that have to be hit in order for the "more times" counter to decrement.
 * If this input list is empty, only one loop is done.
 * 
 * After every loop, the [EngineInputter] has its inputs cleared via [EngineInputter.clearInputs].
 */
class PracticeSection(engine: Engine) : Block(engine, EnumSet.allOf(BlockType::class.java)) {
    
    companion object {
        val DEFAULT_INIT_BLOCK: PracticeInitBlock = PracticeInitBlock(4f, 1) { _, _ -> }
        val DEFAULT_LOOP_BLOCK: PracticeLoopBlock = PracticeLoopBlock(4f) { _, _ -> emptyList() }
        val DEFAULT_END_BLOCK: PracticeEndBlock = PracticeEndBlock(4f) { _, _ -> }
    }
    
    var initBlock: PracticeInitBlock = DEFAULT_INIT_BLOCK
    var loopBlock: PracticeLoopBlock = DEFAULT_LOOP_BLOCK
    var endBlock: PracticeEndBlock = DEFAULT_END_BLOCK
    
    init { // Debug only, this block is not in the editor
        this.width = 4f
        this.defaultText.set("PracticeSection")
    }

    override fun compileIntoEvents(): List<Event> {
        val baseBeat = this.beat

        return listOf(
                PracticeInitEvent(engine, initBlock).also {
                    it.beat = baseBeat
                },
                PracticeLoopEvent(engine, loopBlock, endBlock).also {
                    it.beat = baseBeat + initBlock.duration
                }
        )
    }

    override fun copy(): PracticeSection {
        return PracticeSection(engine).also {
            this.copyBaseInfoTo(it)
            it.initBlock = this.initBlock
            it.loopBlock = this.loopBlock
            it.endBlock = this.endBlock
        }
    }
    
    class PracticeInitEvent(engine: Engine, val initBlock: PracticeInitBlock)
        : Event(engine) {
        override fun onStart(currentBeat: Float) {
            super.onStart(currentBeat)
            initBlock.block.invoke(engine, this.beat)
            val practice = engine.inputter.practice
            practice.practiceModeEnabled = true
            practice.moreTimes.set(initBlock.moreTimes)
            practice.requiredInputs = emptyList()
        }
    }

    class PracticeLoopEvent(engine: Engine, val loopBlock: PracticeLoopBlock, val endBlock: PracticeEndBlock)
        : Event(engine) {
        override fun onStart(currentBeat: Float) {
            super.onStart(currentBeat)
            val practice = engine.inputter.practice
            engine.inputter.clearInputs(beforeBeat = this.beat)
            if (practice.moreTimes.get() > 0) {
                val inputBeats = loopBlock.block.invoke(engine, this.beat)
                practice.requiredInputs = inputBeats
                // Check existing recorded inputs
                (engine.world.entities.filterIsInstance<EntityRodPR>().flatMap { it.inputTracker.results }.filterNotNull() +
                        engine.inputter.inputResults).toSet().forEach { result ->
                    val required = inputBeats.firstOrNull { 
                        !it.wasHit && it.inputType.isInputEquivalent(result.inputType) && MathUtils.isEqual(result.perfectBeat, it.beat, EngineInputter.BEAT_EPSILON) 
                    }
                    if (required != null) {
                        required.wasHit = true
                        required.hitScore = result.inputScore
                    }
                }
                
                // Add another loop event after the duration of the loop block. If cleared, it runs the endBlock info instead.
                engine.addEvent(PracticeLoopEvent(engine, loopBlock, endBlock).also { 
                    it.beat = this.beat + loopBlock.duration
                })
            } else {
                // Run end block
                endBlock.block.invoke(engine, this.beat)
            }
        }
    }
}

open class PracticeBlock<R>(val duration: Float, val block: (engine: Engine, startBeat: Float) -> R)

class PracticeInitBlock(duration: Float, val moreTimes: Int, block: (engine: Engine, startBeat: Float) -> Unit)
    : PracticeBlock<Unit>(duration, block)
class PracticeLoopBlock(duration: Float, block: (engine: Engine, startBeat: Float) -> List<EngineInputter.RequiredInput>)
    : PracticeBlock<List<EngineInputter.RequiredInput>>(duration, block)
class PracticeEndBlock(duration: Float, block: (engine: Engine, startBeat: Float) -> Unit)
    : PracticeBlock<Unit>(duration, block)
