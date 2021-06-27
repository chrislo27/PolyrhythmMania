package polyrhythmmania.editor.block

import polyrhythmmania.Localization
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EventSkillStar
import java.util.*


class BlockSkillStar(engine: Engine) : Block(engine, EnumSet.of(BlockType.INPUT)) {
    
    init {
        this.width = 0.5f
        val text = Localization.getVar("block.skillStar.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventSkillStar(engine, this.beat).apply { 
            this.beat = -1000f
        })
    }

    override fun copy(): BlockSkillStar {
        return BlockSkillStar(engine).also {
            this.copyBaseInfoTo(it)
        }
    }
}