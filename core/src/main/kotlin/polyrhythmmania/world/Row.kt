package polyrhythmmania.world

import com.badlogic.gdx.math.Interpolation
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


class Row(val world: World, val length: Int, val startX: Int, val startY: Int, val startZ: Int) {

    val rowBlocks: List<EntityRowBlock> = List(length) { index ->
        EntityRowBlock(world, startY.toFloat()).apply {
            this.type = EntityRowBlock.Type.PLATFORM
//            this.visible = false
            this.position.x = (startX + index).toFloat()
            this.position.z = startZ.toFloat()
        }
    }

}

abstract class EventRowBlock(engine: Engine, val row: Row, val index: Int) : Event(engine) {
    protected val entity: EntityRowBlock
        get() = row.rowBlocks[index]
}

class EventRowBlockAppear(engine: Engine, val action: Action, row: Row, index: Int)
    : EventRowBlock(engine, row, index) {

    enum class Action {
        ASCEND,
        DESCEND,
    }

    init {
        this.width = 0.25f
    }

    override fun onStart(currentBeat: Float) {
        val entity = this.entity
        when (action) {
            Action.ASCEND -> {
                entity.visible = true
                entity.position.y = entity.baseY - 1
            }
            Action.DESCEND -> {
                entity.visible = true
                entity.position.y = entity.baseY
            }
        }
    }

    override fun onUpdate(currentBeat: Float) {
        val entity = this.entity
        val percent = getBeatPercentage(currentBeat).coerceIn(0f, 1f)
        when (action) {
            Action.ASCEND -> {
                entity.position.y = Interpolation.linear.apply(entity.baseY - 1, entity.baseY, percent)
            }
            Action.DESCEND -> {
                entity.position.y = Interpolation.linear.apply(entity.baseY, entity.baseY - 1, percent)
            }
        }
    }

    override fun onEnd(currentBeat: Float) {
        val entity = this.entity
        when (action) {
            Action.ASCEND -> {
                entity.visible = true
                entity.position.y = entity.baseY
            }
            Action.DESCEND -> {
                entity.visible = false
                entity.position.y = entity.baseY - 1
            }
        }
    }
}

class EventRowBlockRetract(engine: Engine, row: Row, index: Int)
    : EventRowBlock(engine, row, index) {
    
    init {
        this.width = 0f
    }

    override fun onStart(currentBeat: Float) {
        val entity = this.entity
        entity.pistonState = EntityRowBlock.PistonState.RETRACTED
    }
}
