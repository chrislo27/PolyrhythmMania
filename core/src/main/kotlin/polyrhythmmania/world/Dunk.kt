package polyrhythmmania.world

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.*
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.gamemodes.EventIncrementEndlessScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.util.Semitones
import polyrhythmmania.world.entity.EntityExplosion
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.entity.EntityRod
import polyrhythmmania.world.entity.EntityRod.Companion.MIN_COLLISION_UPDATE_RATE
import polyrhythmmania.world.entity.SpriteEntity
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.render.bg.WorldBackground
import kotlin.math.floor


class EntityDunkBasketBack(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketBack
    }
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val xOff = -0.5f
        vec.x += xOff
        vec.y += xOff * 0.5f
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            drawTintedRegion(batch, vec, tileset, tr, (1f / 32f) * 0, 0f, renderWidth, renderHeight)
        }
    }
}

class EntityDunkBasketFront(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketFront
    }
}

class EntityDunkBasketFrontFaceZ(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketFrontFaceZ
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val xOff = -0.5f
        vec.x += xOff
        vec.y += xOff * 0.5f + (0.5f)
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            drawTintedRegion(batch, vec, tileset, tr, (1f / 32f) * 0, 0f, renderWidth, renderHeight)
        }
    }
}
class EntityDunkBasketRear(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketRear
    }
}

class EntityDunkBacking(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBacking
    }
}


class EntityRodDunk(world: World, deployBeat: Float) : EntityRod(world, deployBeat) {

    private val killAfterBeats: Float = 7f
    private val startingX: Float = getPosXFromBeat(0f)

    private var explodeAtSec: Float = Float.MAX_VALUE
    var exploded: Boolean = false
        private set

    private var playedDunkSfx: Boolean = false
    private var inputWasSuccessful: Boolean = false
    private var inputAccepted: Boolean = false
    
    val acceptingInputs: Boolean
        get() = !collision.collidedWithWall && !exploded && !inputAccepted

    init {
        this.position.x = startingX
        this.position.z = -1f
        this.position.y = 2f + 1
    }

    fun getCurrentIndex(posX: Float = this.position.x): Float = posX - 4f
    fun getCurrentIndexFloor(posX: Float = this.position.x): Int = floor(getCurrentIndex(posX)).toInt()

    fun getPosXFromBeat(beatsFromDeploy: Float): Float {
        return (4f + 0.5f - 3 * xUnitsPerBeat) + (beatsFromDeploy) * xUnitsPerBeat - (6 / 32f)
    }

    fun explode(engine: Engine, playSound: Boolean) {
        if (isKilled || exploded) return
        exploded = true
        world.addEntity(EntityExplosion(world, engine.seconds, this.renderScale, this.offsetX, this.offsetY).also {
            it.position.set(this.position)
        })
        if (playSound) {
            playSfxExplosion(engine)
        }
        engine.inputter.missed()
        if (engine.areStatisticsEnabled) {
            engine.inputter.inputCountStats.total++
            engine.inputter.inputCountStats.missed++
            GlobalStats.rodsExploded.increment()
            GlobalStats.rodsMissedDunk.increment()
        }
    }

    fun bounce(engine: Engine, inputResult: InputResult) {
        if (inputResult.inputScore == InputScore.MISS || inputAccepted) return

        fun indexToX(index: Float): Float = index + world.dunkPiston.position.x

        val inputter = engine.inputter
        val modifiers = engine.modifiers
        val inputSuccessful = !inputter.inputChallenge.isInputScoreMiss(inputResult.inputScore)
        val lerpResult = (inputResult.accuracyPercent + 1f) / 2f // 0.5 = perfect
        val endX = if (inputSuccessful) indexToX(MathUtils.random(4.4f, 5.2f)) else indexToX(MathUtils.lerp(3f, 4f, lerpResult))
        val endY = if (inputSuccessful) 5f else this.position.y
        val xDistance = endX - this.position.x
        val calculatedHeight = this.position.y + 1f + xDistance
        val maxHeight = if (inputSuccessful) (calculatedHeight + MathUtils.lerp(-0.2f, 0.2f, lerpResult)) else (calculatedHeight)
        val prevBounce = collision.bounce
        collision.bounce = Bounce(this, maxHeight, this.position.x, this.position.y, endX, endY, prevBounce)

        val dunkBeat = inputResult.perfectBeat + 2f
        if (inputSuccessful) {
            this.inputWasSuccessful = true
            engine.addEvent(EventIncrementEndlessScore(engine) { newScore ->
                if (engine.areStatisticsEnabled) {
                    inputter.inputCountStats.total++
                    if (inputResult.inputScore == InputScore.ACE) {
                        inputter.inputCountStats.aces++
                    } else {
                        if (inputResult.accuracyPercent < 0f) {
                            inputter.inputCountStats.early++
                        } else {
                            inputter.inputCountStats.late++
                        }
                    }
                    GlobalStats.rodsDunkedDunk.increment()
                }
                
                val increaseLivesEvery = 4
                val increaseSpeedEvery = 8

                if (newScore % increaseLivesEvery == 0) {
                    // Increment lives
                    engine.addEvent(EventPlaySFX(engine, dunkBeat, "sfx_practice_moretimes_2"))
                    val endlessScore = modifiers.endlessScore
                    val currentLives = endlessScore.lives.get()
                    val newLives = (currentLives + 1).coerceIn(0, endlessScore.maxLives.get())
                    if (newLives > currentLives) {
                        endlessScore.lives.set(newLives)
                        if (engine.areStatisticsEnabled) {
                            GlobalStats.livesGainedDunk.increment()
                        }
                    }
                } else {
                    engine.addEvent(EventPlaySFX(engine, dunkBeat, "sfx_practice_moretimes_1"))
                }
                
                if (newScore % increaseSpeedEvery == 0) {
                    val maxValue = 12
                    val semitone = (newScore / increaseSpeedEvery).coerceAtMost(maxValue)
                    val pitch = Semitones.getALPitch(semitone)
                    engine.addEvent(EventChangePlaybackSpeed(engine, pitch).apply { 
                        this.beat = dunkBeat
                    })

//                    if (engine.areStatisticsEnabled && semitone >= maxValue) {
//                        Achievements.awardAchievement(Achievements.dunkReachMaxSpeed)
//                    }
                }
            }.apply { 
                this.beat = dunkBeat
            })
        } else {
            this.inputWasSuccessful = false
            engine.addEvent(object : Event(engine) {
                init {
                    this.beat = dunkBeat
                }

                override fun onStart(currentBeat: Float) {
                    super.onStart(currentBeat)
                    explode(engine, true)
                }
            })
        }
        
        inputAccepted = true
    }


    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        if (!isKilled && !playedDunkSfx) {
            playedDunkSfx = true
            engine.addEvent(object : EventPlaySFX(engine, deployBeat + 2f, "sfx_dunk_dunk") {
                override fun onAudioStart(atBeat: Float, actualBeat: Float) {
                    if (!this@EntityRodDunk.isKilled) {
                        super.onAudioStart(atBeat, actualBeat)
                    }
                }
            })
        }

        if (seconds >= explodeAtSec && !exploded) {
            explode(engine, true)
        } else if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            if (!inputWasSuccessful) {
                explode(engine, true)
            }
            kill()
        }
    }

    /**
     * Collision:
     * - Collision is checked per engine update with a minimum of [MIN_COLLISION_UPDATE_RATE] per engine second
     * - Rods move in a straight line on the X axis unless horizontally stopped
     * - Rods can be horizontally stopped if it collides with the side wall of a block (incl extended pistons)
     *   - Side collision is simply if (currentIndexFloat - currentIndex) >= 0.7f and the next block's Y > rod's Y pos
     * - When a bouncer bounces a rod, the bouncer determines where the landing point is at moment of bounce
     * - This puts the rod into a "bounce" state where it is not
     * - Rods can be moved up and down by movement of the platform/piston
     */
    override fun collisionCheck(engine: Engine, beat: Float, seconds: Float, deltaSec: Float) {
        if (exploded) return

        val prevPosX = this.position.x
        val prevPosY = this.position.y

        // Do collision check. Only works on the EntityRowBlocks for the given row.
        // 1. Stops instantly if it hits a prematurely deployed piston
        // 2. Stops if it hits a block when falling
        val beatsFromDeploy = beat - deployBeat
        val targetX = getPosXFromBeat(beatsFromDeploy)
        // The index that the rod is on
        val currentIndexFloat = getCurrentIndex(prevPosX)
        val currentIndex = floor(currentIndexFloat).toInt()
        val collision = this.collision

        // Check for wall stop
        val dunkPiston = world.dunkPiston
        if (!collision.collidedWithWall && (currentIndexFloat - currentIndex) >= 0.80f && currentIndex >= -1) {
            val nextIndex = currentIndex + 1

            // ULTRA HACKY!
            if (nextIndex == 0) { // Dunk piston
                val next = dunkPiston
                val heightOfNext = next.collisionHeight
                if (next.active && prevPosY in next.position.y..(next.position.y + heightOfNext - (1f / 32f))) {
                    collision.collidedWithWall = true
                    this.position.x = currentIndex + 0.7f + next.position.x

                    playSfxSideCollision(engine)

                    if (explodeAtSec == Float.MAX_VALUE) {
                        explodeAtSec = seconds + EXPLODE_DELAY_SEC
                    }
                }
            } else if (nextIndex >= 5) { // Anything past the hoop pole
                collision.collidedWithWall = true
                this.position.x = currentIndex + 0.7f + (5 - 1)

                playSfxSideCollision(engine)
            }
        }

        // If not already collided with a wall, move X
        if (!collision.collidedWithWall) {
            this.position.x = targetX
        }

        // Control the Y position
        if (currentIndex < 0) {
            // Set to row height when not in the block area
            this.position.y = 2f + 1
            this.collision.velocityY = 0f
        } else {
            if ((collision.bounce?.endX ?: Float.MAX_VALUE) < this.position.x) {
                collision.bounce = null
            }
            val currentBounce: Bounce? = collision.bounce

            if (currentBounce != null && !collision.collidedWithWall) {
                val posX = this.position.x
                this.position.y = currentBounce.getYFromX(posX)
                collision.velocityY = (this.position.y - prevPosY) / deltaSec
            } else {
                val floorBelow: Float = when {
                    currentIndex < 0 -> 3f
                    currentIndex == 0 -> (dunkPiston.position.y + dunkPiston.collisionHeight)
                    currentIndex >= 4 -> -1E8f
                    else -> 2f
                }
                if (floorBelow >= this.position.y) { // Push the rod up to the floor height and kill velocityY
                    collision.velocityY = 0f
                    this.position.y = floorBelow
                } else {
                    collision.velocityY += GRAVITY * deltaSec

                    val veloY = collision.velocityY
                    if (veloY != 0f) {
                        val futureY = this.position.y + veloY * deltaSec
                        if (futureY < floorBelow) {
                            this.position.y = floorBelow
                            playSfxLand(engine)
                            collision.velocityY = 0f
                        } else {
                            this.position.y = futureY
                        }
                    }
                }
                
                // Auto-inputs
                if (engine.autoInputs) {
                    if (collision.velocityY == 0f && currentIndexFloat in 0.25f..0.65f) {
                        dunkPiston.fullyExtend(engine, beat)
                    }
                }
            }
        }
    }

    override fun onRemovedFromWorld(engine: Engine) {
        super.onRemovedFromWorld(engine)
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        if (!exploded) super.render(renderer, batch, tileset)
    }
}

class EntityPistonDunk(world: World)
    : EntityPiston(world) {
    
    private var retractAfter: Float = Float.MAX_VALUE
    
    override fun fullyExtend(engine: Engine, beat: Float) {
        super.fullyExtend(engine, beat)
        retractAfter = beat + 0.5f

        when (type) {
            Type.PLATFORM -> {
            }
            Type.PISTON_A -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_a"), SoundInterface.SFXType.PLAYER_INPUT)
            Type.PISTON_DPAD -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_d"), SoundInterface.SFXType.PLAYER_INPUT)
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        if (beat >= retractAfter) {
            retractAfter = Float.MAX_VALUE
            retract()
        }
    }
}


object DunkWorldBackground : WorldBackground() {
    override fun render(batch: SpriteBatch, world: World, camera: OrthographicCamera) {
        val tex: Texture = AssetRegistry["dunk_background"]
        batch.draw(tex, camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight)
    }
}
