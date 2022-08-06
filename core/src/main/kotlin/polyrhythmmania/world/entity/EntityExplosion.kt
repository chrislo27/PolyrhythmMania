package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion

class EntityExplosion(
        world: World, val secondsStarted: Float,
        val renderScale: Float, val rodOffsetX: Float, val rodOffsetY: Float
) : SpriteEntity(world), TemporaryEntity {

    companion object {
        const val EXPLOSION_DURATION: Float = 8 / 60f
        
        val STATES: List<State> = listOf(
                State(0, 40f / 32f, 24f / 32f),
                State(1, 32f / 32f, 24f / 32f),
                State(2, 24f / 32f, 16f / 32f),
                State(3, 16f / 32f, 16f / 32f),
        )
    }

    data class State(val index: Int, val renderWidth: Float, val renderHeight: Float)

    private var state: State = STATES[0]
    var duration: Float = EXPLOSION_DURATION

    override val renderWidth: Float
        get() = state.renderWidth * renderScale
    override val renderHeight: Float
        get() = state.renderHeight * renderScale
    
    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 0f
    override val renderSortOffsetZ: Float get() = 0f
    
    private var percentageLife: Float = 0f

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return tileset.explosionFrames[state.index]
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (isKilled) return
        val percentage = this.percentageLife
        if (percentage < 1f) {
            val index = (percentage * STATES.size).toInt()
            state = STATES[index]

            val tr = getTintedRegion(tileset, 0)
            if (tr != null) {
                val centreOfExplosionX = 10f / 32f // X-centre of the explosion at normal 1.0 scaling is 10 px right
                val baseOfExplosionY = 3f / 32f // Base of the explosion at normal 1.0 scaling is 3 px up

                drawTintedRegion(batch, vec, tileset, tr, (centreOfExplosionX) * renderScale - (renderWidth / 2f) + rodOffsetX, baseOfExplosionY * renderScale + rodOffsetY, renderWidth, renderHeight)
            }


            // Debug bounds rendering
//            batch.setColor(0.8f, 0.8f, 1f, 0.75f)
//            batch.fillRect(vec.x, vec.y, renderWidth, renderHeight)
//            batch.setColor(0f, 0f, 1f, 0.75f)
//            batch.fillRect(vec.x, vec.y, 0.1f, 0.1f)
//            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        if (isKilled) return
        
        val secondsElapsed = engine.seconds - secondsStarted
        val percentage = (secondsElapsed / duration).coerceIn(0f, 1f)
        this.percentageLife = percentage
        if (percentage >= 1f) {
            kill()
        }
    }
}
