package polyrhythmmania.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import polyrhythmmania.container.Container
import polyrhythmmania.engine.*
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.gamemodes.EventIncrementEndlessScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.util.Semitones
import polyrhythmmania.world.entity.*
import polyrhythmmania.world.entity.EntityRod.Companion.MIN_COLLISION_UPDATE_RATE
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.render.bg.WorldBackground
import kotlin.math.floor
import kotlin.math.roundToInt


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
    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 0f
    override val renderSortOffsetZ: Float get() = -1f

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
    
    companion object {
        private const val KILL_AFTER_BEATS_NORMAL: Float = 5f
        private const val KILL_AFTER_BEATS_SUCCESSFUL: Float = 8f
    }

    private var killAfterBeats: Float = KILL_AFTER_BEATS_NORMAL
    private val startingX: Float = getPosXFromBeat(0f)

    private var explodeAtSec: Float = Float.MAX_VALUE
    var exploded: Boolean = false
        private set

    private val tintColorOverrideBorder: Color = Color(1f, 1f, 1f, 1f)
    private val tintColorOverrideFill: Color = Color(1f, 1f, 1f, 1f)
    
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
    
    private fun getFadeInAlpha(): Float {
        val lastBeat = this.collisionUpdateLastBeat
        val duration = 1f / 3f
        return Interpolation.linear.apply(0f, 1f, ((lastBeat - deployBeat) / duration).coerceIn(0f, 1f))
    }

    override fun getTintColorOverrideBorder(region: TintedRegion): Color {
        return this.tintColorOverrideBorder.set(region.color.getOrCompute()).apply {
            this.a *= getFadeInAlpha()
        }
    }

    override fun getTintColorOverrideFill(region: TintedRegion): Color {
        return this.tintColorOverrideFill.set(region.color.getOrCompute()).apply {
            this.a *= getFadeInAlpha()
        }
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
        
        val endlessScore = engine.modifiers.endlessScore
        if (endlessScore.enabled.get()) {
            val oldLives = endlessScore.lives.get()
            endlessScore.triggerEndlessLifeLost(engine.inputter) // This intentionally does not normally automatically trigger in EndlessScore
            if (engine.areStatisticsEnabled && endlessScore.lives.get() < oldLives) {
                GlobalStats.livesLostDunk.increment()
            }
        }
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
        val endX = if (inputSuccessful) indexToX(MathUtils.random(4.5f, 4.8f)) else indexToX(MathUtils.lerp(3f, 4f, lerpResult))
        val endY = if (inputSuccessful) 5f else this.position.y
        val xDistance = endX - this.position.x
        val calculatedHeight = this.position.y + 1f + xDistance
        val maxHeight = if (inputSuccessful) (calculatedHeight + MathUtils.lerp(-0.15f, 0.25f, lerpResult)) else (calculatedHeight)
        val prevBounce = collision.bounce
        collision.bounce = Bounce(this, maxHeight, this.position.x, this.position.y, endX, endY, prevBounce)

        val dunkBeat = inputResult.perfectBeat + 2f
        if (inputSuccessful) {
            this.inputWasSuccessful = true
            this.killAfterBeats = KILL_AFTER_BEATS_SUCCESSFUL

            engine.addEvent(EventPlaySFX(engine, dunkBeat, "sfx_dunk_basket_swoosh"))
            
            if (engine.modifiers.endlessScore.enabled.get()) {
                engine.addEvent(object : Event(engine) { // Non-endless statistics are handled already via InputResult
                    override fun onStart(currentBeat: Float) {
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
                    }
                }.apply {
                    this.beat = dunkBeat
                })
                engine.addEvent(EventIncrementEndlessScore(engine) { newScore ->
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
                        if (semitone < maxValue) {
                            engine.addEvent(EventChangePlaybackSpeed(engine, pitch).apply {
                                this.beat = dunkBeat
                            })
                            engine.addEvent(object : Event(engine) {
                                override fun onStartContainer(container: Container, currentBeat: Float) {
                                    container.renderer.endlessModeRendering.triggerSpeedUpText()
                                }
                            }.apply {
                                this.beat = dunkBeat
                            })
                        }

//                      if (engine.areStatisticsEnabled && semitone >= maxValue) {
//                          Achievements.awardAchievement(Achievements.dunkReachMaxSpeed)
//                      }
                    }
                }.apply {
                    this.beat = dunkBeat
                })
            } else {
                engine.addEvent(EventPlaySFX(engine, dunkBeat + 0.5f, "sfx_dunk_ok1"))
                engine.addEvent(EventPlaySFX(engine, dunkBeat + 1.0f, "sfx_dunk_ok2"))
            }
            
            // Star particles
            engine.addEvent(object : Event(engine) {
                override fun onStart(currentBeat: Float) {
                    val initialVelocity = Vector3(0f, 2f, -0.75f)
                    val initialPosition = Vector3(8.5f, 6f, -0.75f)
                    val rotationAxis = Vector3(0f, 1f, 0f)

                    // 0 degrees is -Z, positive goes counterclockwise with 90 = -X, 180 = +Z
                    fun addStar(degrees: Float, after: (pos: Vector3, velo: Vector3) -> Unit) {
                        val pos = initialPosition.cpy().add(Vector3(0f, 0f, -1f).rotate(rotationAxis, degrees).scl(0.75f))
                        val velo = initialVelocity.cpy().rotate(rotationAxis, degrees)
                        
                        after(pos, velo)

                        engine.world.addEntity(EntityDunkStarParticle(engine.world, dunkBeat, pos, velo))
                    }
                    
                    fun jitter(vec: Vector3, scale: Float) {
                        vec.x += MathUtils.random(0f, 1f) * MathUtils.randomSign() * scale
                        vec.y += MathUtils.random(0f, 1f) * MathUtils.randomSign() * scale
                        vec.z += MathUtils.random(0f, 1f) * MathUtils.randomSign() * scale
                    }
                    
                    addStar(0f) { pos, velo ->
                        velo.y *= 1.25f
                        velo.z *= 1.5f
                        jitter(velo, 0.125f)
                    }
                    addStar(270f) { pos, velo ->
                        velo.y *= 1f
                        velo.x *= 2f
                        jitter(velo, 0.125f)
                    }
                    addStar(90f) { pos, velo ->
                        velo.y *= 0.9f
                        velo.x *= 1.1f
                        jitter(velo, 0.125f)
                    }
                    addStar(180f) { pos, velo ->
                        velo.y *= 0.9f
                        velo.x *= 1.1f
                        jitter(velo, 0.125f)
                    }
                }
            }.apply { 
                this.beat = dunkBeat
            })
        } else {
            this.inputWasSuccessful = false
            engine.addEvent(object : Event(engine) {
                override fun onStart(currentBeat: Float) {
                    super.onStart(currentBeat)
                    explode(engine, true)
                }
            }.apply { 
                this.beat = dunkBeat
            })
        }
        
        inputAccepted = true
    }


    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        if (!isKilled && !playedDunkSfx) {
            playedDunkSfx = true
            engine.addEvent(object : EventPlaySFX(engine, deployBeat + 2f, "sfx_dunk_dunk_callout") {
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

                // Don't play side collision if NOT endless AND input was successful
                if (engine.modifiers.endlessScore.enabled.get() || !this.inputWasSuccessful) {
                    playSfxSideCollision(engine)
                }
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

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        if (!exploded) {
            super.render(renderer, batch, tileset)
        }
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
            Type.PISTON_A, Type.PISTON_DPAD -> {
                val aSound = AssetRegistry.get<BeadsSound>("sfx_input_a")
                val dpadSound = AssetRegistry.get<BeadsSound>("sfx_input_d")
                engine.soundInterface.playAudio(aSound, SoundInterface.SFXType.PLAYER_INPUT)
                engine.soundInterface.playAudio(dpadSound, SoundInterface.SFXType.PLAYER_INPUT) { player ->
                    player.gain = 0.5f
                }
            }
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

class EntityDunkStarParticle(world: World, val beatStarted: Float, val startPos: Vector3, val velocity: Vector3)
    : SpriteEntity(world), TemporaryEntity {
    
    private var lastBeat: Float = beatStarted
    private var duration: Float = 1f
    private var percentage: Float = 0f
    
    private val flip: Boolean = MathUtils.randomBoolean() 
    private val animationOffset: Float = MathUtils.random(0f, 1f) 
    
    private val renderScale: Float = 1f
    override val renderWidth: Float get() = (9f / 32f) * renderScale * (if (flip) -1 else 1)
    override val renderHeight: Float get() = (9f / 32f) * renderScale
    override val pxOffsetX: Float = if (flip) (-renderWidth) else 0f
    override val pxOffsetY: Float = 0f

    init {
        this.position.set(startPos)
        this.tint = Color(1f, 1f, 1f, 1f)
    }
    
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        val animation = tileset.dunkStarAnimation
        return animation[((percentage + animationOffset) * animation.size).toInt() % animation.size]
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        if (isKilled) return

        val beatsElapsed = engine.beat - beatStarted
        val percentage = (beatsElapsed / duration).coerceIn(0f, 1f)
        this.percentage = percentage
        
        val deltaBeat = engine.beat - lastBeat
        this.lastBeat = engine.beat
        val velocitySpeed = 1f / 0.85f
        velocity.y += -4f * deltaBeat * velocitySpeed
        this.position.mulAdd(velocity, deltaBeat * velocitySpeed)
        
        val fadeThreshold = 0.75f
        if (percentage >= fadeThreshold) {
            this.tint?.a = Interpolation.linear.apply(1f, 0f, (percentage - fadeThreshold) / (1f - fadeThreshold))
        }
        
        if (percentage >= 1f) {
            kill()
        }
    }
}


object DunkWorldBackground : WorldBackground() {
    override fun render(batch: SpriteBatch, world: World, camera: OrthographicCamera) {
        val tex: Texture = AssetRegistry["dunk_background"]
        batch.draw(tex, camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight)
    }
}
