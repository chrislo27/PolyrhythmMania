package polyrhythmmania.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import paintbox.util.gdxutils.drawQuad
import polyrhythmmania.animation.Animation
import polyrhythmmania.animation.AnimationPlayer
import polyrhythmmania.animation.Step
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.engine.input.InputThresholds
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.gamemodes.EventAsmAssemble
import polyrhythmmania.gamemodes.SidemodeAssets
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.util.WaveUtils
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.entity.EntityRod
import polyrhythmmania.world.entity.SpriteEntity
import polyrhythmmania.world.entity.TemporaryEntity
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.render.bg.WorldBackground
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import kotlin.math.*



object AssembleWorldBackground : WorldBackground() {

    private val gradientStart: Color = Color.valueOf("1B6B17")
    private val gradientEnd: Color = Color.BLACK.cpy()
    
    override fun render(batch: SpriteBatch, world: World, engine: Engine, camera: OrthographicCamera) {
        batch.drawQuad(0f, camera.viewportHeight * 0.25f, gradientEnd, 
                camera.viewportWidth, camera.viewportHeight * 0.25f, gradientEnd,
                camera.viewportWidth, camera.viewportHeight, gradientStart,
                0f, camera.viewportHeight, gradientStart)
    }
}

open class EntityAsmCube(world: World)
    : SpriteEntity(world) {
    
    constructor(world: World, tint: Color) : this(world) {
        this.tint = tint
    }
    
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return tileset.asmCube
    }
}

open class EntityAsmLane(world: World)
    : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return tileset.asmLane
    }
}

open class EntityAsmPerp(world: World, val isTarget: Boolean = false)
    : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return if (isTarget) tileset.asmCentrePerpTarget else tileset.asmCentrePerp
    }
}

class EntityPistonAsm(world: World) : EntityPiston(world) {
    
    companion object {
        val playerTint: Color = Color(1f, 0.35f, 0.35f, 1f)
        val playerCompressedTint: Color = Color(1f, 0.15f, 0.15f, 1f)
    }
    
    sealed class Animation(val piston: EntityPistonAsm) {
        class Neutral(piston: EntityPistonAsm) : Animation(piston)
        class Charged(piston: EntityPistonAsm, val startBeat: Float) : Animation(piston) {
            override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
                super.engineUpdate(engine, beat, seconds)
                
                val duration = 0.25f
                val maxZ = 3f
                val alpha = ((beat - startBeat) / duration).coerceIn(0f, 1f)
                piston.position.z = Interpolation.linear.apply(0f, maxZ, alpha)
                piston.tint?.set(playerTint)?.lerp(playerCompressedTint, alpha)
            }
        }
        class Uncharged(piston: EntityPistonAsm, val startBeat: Float) : Animation(piston) {
            override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
                super.engineUpdate(engine, beat, seconds)
                
                val duration = 0.25f
                val maxZ = 3f
                val alpha = ((beat - startBeat) / duration).coerceIn(0f, 1f)
                piston.position.z = Interpolation.linear.apply(maxZ, 0f, alpha)
                piston.tint?.set(playerCompressedTint)?.lerp(playerTint, alpha)
                
                if (alpha >= 1f) {
                    piston.animation = Neutral(piston)
                }
            }
        }
        class Fire(piston: EntityPistonAsm, val startBeat: Float) : Animation(piston) {
            object FireInterpolation : Interpolation() {
                private fun decay(a: Float): Float {
                    return MathUtils.lerp(2.0.pow(10.0 * -a).toFloat(), 1f - a, 0.25f)
                }

                private fun sine(a: Float): Float {
                    return MathUtils.sin((1f - a) * ((5 - 0.5f) * MathUtils.PI))
                }

                @Suppress("RemoveRedundantQualifierName")
                override fun apply(a: Float): Float {
                    val holdAmt = 0.075f
                    val holdMin = 0.9f
                    val returnAmt = 0.25f
                    val alpha: Float = if (a < holdAmt) {
                        return Interpolation.slowFast.apply(1f, holdMin, a / holdAmt)
                    } else if (a > 1f - returnAmt) {
                        val beforeA = Interpolation.fastSlow.apply(((1f - returnAmt) - holdAmt) / (1f - holdAmt - returnAmt)) * holdMin
                        val before = decay(beforeA) * sine(beforeA)
                        return Interpolation.fastSlow.apply(before, 0f, (a - (1f - returnAmt)) / returnAmt)
                    } else {
                        Interpolation.fastSlow.apply((a - holdAmt) / (1f - holdAmt - returnAmt)) * holdMin
                    }
                    return decay(alpha) * sine(alpha)
                }
            }
            
            override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
                super.engineUpdate(engine, beat, seconds)

                val duration = 1f
                val minZ = -3f
                val alpha = ((beat - startBeat) / duration).coerceIn(0f, 1f)
                piston.position.z = Interpolation.circleOut.apply(minZ, 0f, alpha)
                piston.position.z = Interpolation.circleOut.apply(minZ, 0f, alpha) + ((Interpolation.ElasticOut(2f, 10f, 20, 1f).apply(alpha) - 1) * -1f)
                piston.position.z = FireInterpolation.apply(0f, minZ, alpha)
                
                if (alpha >= 1f) {
                    piston.animation = Neutral(piston)
                    piston.position.z = 0f
                }
                piston.tint?.set(playerTint)
            }
        }
        
        open fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {}
    }
    
    var animation: Animation = Animation.Neutral(this)
    var retractAfterBeats: Float = Float.POSITIVE_INFINITY
    private var extendWiggleAlpha: Float = 0f
    
    init {
        this.type = Type.PISTON_DPAD
    }

    fun fullyExtend(engine: Engine, beat: Float, retractAfterBeats: Float, doWiggle: Boolean) {
        super.fullyExtend(engine, beat)
        this.retractAfterBeats = beat + retractAfterBeats
        if (doWiggle) {
            this.extendWiggleAlpha = 1f
        }
    }
    
    fun chargeUp(beat: Float) {
        animation = Animation.Charged(this, beat)
        retract()
    }
    
    fun uncharge(beat: Float) {
        animation = Animation.Uncharged(this, beat)
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        animation.engineUpdate(engine, beat, seconds)
        
        super.engineUpdate(engine, beat, seconds)
        
        if (beat >= retractAfterBeats) {
            retractAfterBeats = Float.POSITIVE_INFINITY
            retract()
        }
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine,
                              vec: Vector3) {
        if (animation is Animation.Charged) {
            vec.x += MathUtils.random() * MathUtils.randomSign() * 0.025f
            vec.y += MathUtils.random() * MathUtils.randomSign() * 0.025f
        }
        if (extendWiggleAlpha > 0) {
            extendWiggleAlpha = (extendWiggleAlpha - Gdx.graphics.deltaTime / 0.125f).coerceAtLeast(0f)
            vec.y += MathUtils.lerp(-1f, 0f, extendWiggleAlpha % 1f) * 0.25f * extendWiggleAlpha
        }
        
        val tmpColor = ColorStack.getAndPush()
        val tint = this.tint
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            if (tr != null) {
                val allowTint = !(tr == tileset.pistonAExtendedFaceX || tr == tileset.pistonAExtendedFaceZ || tr == tileset.pistonAPartialFaceX || tr == tileset.pistonAPartialFaceZ ||
                        tr == tileset.pistonDpadExtendedFaceX || tr == tileset.pistonDpadExtendedFaceZ || tr == tileset.pistonDpadPartialFaceX || tr == tileset.pistonDpadPartialFaceZ)
                if (tintIsMultiplied) {
                    tmpColor.set(tr.color.getOrCompute())
                    if (tint != null && allowTint) {
                        tmpColor.r *= tint.r
                        tmpColor.g *= tint.g
                        tmpColor.b *= tint.b
                        tmpColor.a *= tint.a
                        // Intentionally don't clamp values.
                    }
                } else {
                    if (tint != null && allowTint) tmpColor.set(tint)
                }
                drawTintedRegion(batch, vec, tileset, tr, tmpColor)
            }
        }
        ColorStack.pop()
    }

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return when (type) {
            Type.PLATFORM -> tileset.platform
            Type.PISTON_A -> when (pistonState) {
                PistonState.FULLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonAExtendedFaceX
                    1 -> tileset.pistonAExtendedFaceZ
                    2 -> tileset.asmPistonAExtended
                    else -> null
                }
                PistonState.PARTIALLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonAPartialFaceX
                    1 -> tileset.pistonAPartialFaceZ
                    2 -> tileset.asmPistonAPartial
                    else -> null
                }
                PistonState.RETRACTED -> tileset.asmPistonA
            }
            Type.PISTON_DPAD -> when (pistonState) { // DPAD indicates that it's a non-player bouncer.
                PistonState.FULLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonAExtendedFaceX
                    1 -> tileset.pistonAExtendedFaceZ
                    2 -> tileset.pistonAExtended
                    else -> null
                }
                PistonState.PARTIALLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonAPartialFaceX
                    1 -> tileset.pistonAPartialFaceZ
                    2 -> tileset.pistonAPartial
                    else -> null
                }
                PistonState.RETRACTED -> tileset.pistonARetracted
            }
        }
    }

}

class EntityRodAsm(world: World, deployBeat: Float) : EntityRod(world, deployBeat) {

    data class BounceAsm(val startBeat: Float, val duration: Float,
                         val peakHeight: Float,
                         val startX: Float, val startY: Float, val endX: Float, val endY: Float,
                         val previousBounce: BounceAsm?) {

        fun getXFromBeat(beat: Float): Float {
            if (previousBounce != null && beat < startBeat) {
                return previousBounce.getXFromBeat(beat)
            }
            val alpha = ((beat - startBeat) / (duration)).coerceIn(0f, 1f)
            return MathUtils.lerp(startX, endX, alpha)
        }
        
        fun getYFromBeat(beat: Float): Float {
            if (previousBounce != null && beat < startBeat) {
                return previousBounce.getYFromBeat(beat)
            }
            val alpha = ((beat - startBeat) / (duration)).coerceIn(0f, 1f)
            return if (alpha <= 0.5f) {
                MathUtils.lerp(startY, peakHeight, WaveUtils.getBounceWave(alpha))
            } else {
                MathUtils.lerp(endY, peakHeight, WaveUtils.getBounceWave(alpha))
            }
        }
    }
    
    class NextExpected(val inputBeat: Float, val isFire: Boolean) {
//        class NextBounce(inputBeat: Float, val toIndex: Int) : NextExpected(inputBeat)
//        class Fire(inputBeat: Float) : NextExpected(inputBeat)

        /**
         * Filled when an input is hit
         */
        var hitInput: InputResult? = null
            private set

        /**
         * If not null, this bounce will be put next when hitInput is filled with a non-miss.
         */
        var conditionalBounce: BounceAsm? = null
            private set

        fun addInput(rod: EntityRodAsm, input: InputResult) {
            if (input.inputScore == InputScore.MISS || !MathUtils.isEqual(input.perfectBeat, inputBeat, 0.01f)) return
            hitInput = input
            
            if (isFire) {
                rod.kill()
            } else {
                val cond = this.conditionalBounce
                if (cond != null) {
                    rod.bounce = cond
                }
            }
        }
        
        fun addConditionalBounce(rod: EntityRodAsm, bounce: BounceAsm) {
            val hit = this.hitInput
            val early = rod.earlyInputResult
            if (hit != null && MathUtils.isEqual(hit.perfectBeat, bounce.startBeat, 0.01f)) {
                rod.bounce = bounce
            } else if (early != null && MathUtils.isEqual(early.perfectBeat, bounce.startBeat, 0.01f)) {
                conditionalBounce = bounce
                addInput(rod, early)
                rod.earlyInputResult = null
            } else {
                conditionalBounce = bounce
            }
        }
    }

    override val isInAir: Boolean = true
    
    val expectedInputs: List<NextExpected> = mutableListOf()
    var earlyInputResult: InputResult? = null
        private set

    var killAtBeat: Float = Float.POSITIVE_INFINITY
    var bounce: BounceAsm? = null

    /**
     * True when an input has missed
     */
    var failed: Boolean = false
        private set
    var failFallVeloY: Float = 0f
    
    var disableInputs: Boolean = false

    val acceptingInputs: Boolean
        get() = !isKilled && !failed && !disableInputs

    /**
     * If there is an expected input that [input] matches, then it is added to the expected input ([input] is late).
     * Otherwise, it is queued up in [earlyInputResult], overwriting anything that was previously there.
     */
    fun addInputResult(engine: Engine, input: InputResult) {
        val matchingExpected = expectedInputs.lastOrNull {
            MathUtils.isEqual(it.inputBeat, input.perfectBeat, 0.01f) && it.hitInput == null
        }
        
        if (matchingExpected != null) {
            matchingExpected.addInput(this, input)
            val piston = world.asmPlayerPiston
            if (matchingExpected.isFire/* && piston.animation is EntityPistonAsm.Animation.Charged*/) {
                piston.animation = EntityPistonAsm.Animation.Fire(piston, matchingExpected.inputBeat)
                piston.retract()
                engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_shoot"), SoundInterface.SFXType.PLAYER_INPUT)
                engine.addEvent(EventAsmAssemble(engine, matchingExpected.inputBeat))
            } else {
                engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_middle_right"), SoundInterface.SFXType.PLAYER_INPUT)
            }
        } else {
            earlyInputResult = input
        }
    }

    /**
     * Adds an expected input. Consumes earlyInputResult, and if it matches the expected input, immediately
     * calls [NextExpected.addInput] with it.
     */
    fun addExpectedInput(expected: NextExpected) {
        expectedInputs as MutableList
        val earlyInput = earlyInputResult
        
        if (earlyInput != null) {
            if (MathUtils.isEqual(earlyInput.perfectBeat, expected.inputBeat, 0.01f)) {
                expected.addInput(this, earlyInput)
            }
        }
        
        expectedInputs += expected
        
        this.earlyInputResult = null
    }
    
    fun getPistonPosition(engine: Engine, index: Int): Vector3 {
        val vec = Vector3(0f, 0f, 0f)
        val pistons = engine.world.asmPistons

        if (index < 0) {
            vec.set(pistons[0].position)
            vec.x -= 4f
        } else if (index >= pistons.size) {
            vec.set(pistons[pistons.size - 1].position)
            vec.x += 4f
        } else {
            vec.set(pistons[index].position)
        }

        return vec
    }
    
    override fun collisionCheck(engine: Engine, beat: Float, seconds: Float, deltaSec: Float) {
        super.collisionCheck(engine, beat, seconds, deltaSec)

        val currentBounce = this.bounce
        if (currentBounce != null) {
            val endBeat = currentBounce.startBeat + currentBounce.duration
            this.position.x = currentBounce.getXFromBeat(beat) + ((1f / 32f) * 6)
            if (!failed) this.position.y = currentBounce.getYFromBeat(beat)

            if (beat >= endBeat) {
                this.bounce = null
            }
        }


        val expected = expectedInputs.lastOrNull()
        
        // Auto-inputs
        if (engine.autoInputs && expected != null && expected.hitInput == null && beat > expected.inputBeat) {
            this.addInputResult(engine, InputResult(expected.inputBeat, InputType.A, 0f, 0f, 0))
            val inputter = engine.inputter
            inputter.inputFeedbackFlashes[inputter.getInputFeedbackIndex(InputScore.ACE, false)] = seconds

            val asmPlayerPiston = world.asmPlayerPiston
            if (asmPlayerPiston.animation is EntityPistonAsm.Animation.Neutral) {
                asmPlayerPiston.fullyExtend(engine, expected.inputBeat, 1f, true)
            }
        }

        // Check expected inputs. If missed, make the rod fall off/fall down depending on if input was fire or normal bounce
        if (!failed && expected != null && (expected.hitInput == null || expected.hitInput?.inputScore == InputScore.MISS)) {
            val expectedInputSec = engine.tempos.beatsToSeconds(expected.inputBeat)
            if (seconds > expectedInputSec + InputThresholds.MAX_OFFSET_SEC) {
                failed = true
                killAtBeat = expected.inputBeat + 2f
                engine.inputter.missed()
                if (expected.isFire) {
                    failFallVeloY = GRAVITY
                } else {
                    bounce = BounceAsm(beat, 1f, this.position.y - 0.01f, this.position.x, this.position.y,
                            this.position.x + MathUtils.random(1.25f, 2f) * MathUtils.randomSign(), this.position.y - 7f, null)
                    failFallVeloY = 6f
                    engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_collide"), SoundInterface.SFXType.NORMAL) {
                        it.gain = 0.5f
                    }
                }

                if (engine.areStatisticsEnabled) {
                    GlobalStats.rodsDroppedAssemble.increment()
                }
            }
        }

        if (failed) {
            failFallVeloY += -64f * deltaSec
            this.position.y += failFallVeloY * deltaSec
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        if (!isKilled && beat > killAtBeat) {
            kill()
        }
    }

    override fun onRemovedFromWorld(engine: Engine) {
        super.onRemovedFromWorld(engine)
    }
}

class EntityAsmWidgetHalf(world: World, val goingRight: Boolean,
                          val combineBeat: Float, val startBeatsBeforeCombine: Float,
                          val beatsPerUnit: Float = 1f)
    : SpriteEntity(world), TemporaryEntity {
    
    companion object {
        private val asmWidgetRollTestBccadIDs: Map<String, Int> = listOf("17", "16", "15", "23", "14", "24", "13", "22", "21", "20", "19", "18").withIndex().associate { it.value to it.index }
        private val asmWidgetRollTestStepL: Animation = Animation(listOf("13" to 1, "22" to 1, "19" to 1, "18" to 2, "17" to 2, "16" to 3, "15" to 3, "23" to 4, "14" to 13).map {
            (id, delay) -> Step(id, delay)
        })
        private val asmWidgetRollTestStepR: Animation = Animation(listOf("13" to 1, "14" to 1, "15" to 1, "16" to 2, "17" to 2, "18" to 3, "19" to 3, "20" to 4, "21" to 13).map {
            (id, delay) -> Step(id, delay)
        })
    }

    private val combineX: Float = 12f - 1 /* -1 due to positioning offset to fix floor clipping */

    private var lastBeat: Float = 0f
    private val animation: Animation = if (goingRight) asmWidgetRollTestStepL else asmWidgetRollTestStepR
    private val animationPlayer: AnimationPlayer = animation.createPlayer().apply { 
        this.speedMultiplier.set(1 / 3f)
    }
    private var animationReset: Boolean = false
    
    init {
        this.position.y = 0f
        this.position.z = if (goingRight) -6.5f else -6f
        this.position.x = getXFromBeat(1000f)
        
        this.position.y += 1f
        this.position.z += 1f
        if (!goingRight) {
            this.position.y += 0.5f
            this.position.z += 0.5f
        }
    }

    fun getXFromBeat(beatsBeforeCombine: Float): Float {
        val movementSign = if (goingRight) 1 else -1
        val offset = floor(beatsBeforeCombine / beatsPerUnit) + 0.25f

        var x = combineX - (offset * movementSign)
        val beatPiece = 1f - (((beatsBeforeCombine / beatsPerUnit) + 1000f) % 1)
        val moveTime = (17f / 30) * beatsPerUnit
        if (beatPiece < moveTime) {
            x += (Interpolation.linear.apply((beatPiece / moveTime)) - 1f) * movementSign
            if (!animationReset) {
                animationReset = true
                animationPlayer.reset()
            }
        } else {
            animationReset = false
        }
        
        if (!goingRight) {
            x -= 0.5f
        }

        return x
    }

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.asmWidgetRollTestFrames[asmWidgetRollTestBccadIDs[animationPlayer.getCurrentStep().id] ?: 5]
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        animationPlayer.update((beat - this.lastBeat) / beatsPerUnit)
        this.lastBeat = beat

        this.position.x = getXFromBeat(-(beat - combineBeat))
        
        if (!isKilled && beat > combineBeat + 12f * beatsPerUnit) {
            kill()
        }
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine,
                              vec: Vector3) {
        
        
        val xOff = -0.5f * 0
        vec.x += xOff
        vec.y += xOff / 2
        super.renderSimple(renderer, batch, tileset, engine, vec)
    }
}

open class EntityAsmWidgetComplete(world: World,
                                   val combineBeat: Float)
    : SpriteEntity(world), TemporaryEntity {

    private val combineX: Float = 12f - 1f
    private val combineY: Float = 0f + 1f
    private val combineZ: Float = -6f + 1f

    init {
        this.position.y = combineY
        this.position.z = combineZ
        this.position.x = combineX
    }


    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.asmWidgetComplete
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        val elapsed = (beat - combineBeat).coerceAtLeast(0f)
        if (elapsed in 0f..1f) {
            // Slide off
            this.position.z = Interpolation.exp5Out.apply(combineZ, combineZ - (2f + 0.2f), elapsed * 0.85f)
        } else {
            val alpha = elapsed - 1
            this.position.x = combineX + 1f
            this.position.z = Interpolation.smoother.apply(Interpolation.exp5Out.apply(combineZ, combineZ - (2f + 0.2f), 0.85f), combineZ - 2.5f, alpha) - 1f
            this.position.y = Interpolation.circleIn.apply(0f, -4f, alpha)
        }

        if (!isKilled && beat > combineBeat + 5f) {
            kill()
        }
    }

}

class EntityAsmWidgetCompleteBlur(world: World,
                              val combineBeat: Float)
    : SpriteEntity(world), TemporaryEntity {

    override val pxOffsetX: Float = 24f / 32f
    override val pxOffsetY: Float = -16f / 32f
    
    private var frameCountdown: Int = 2

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.asmWidgetCompleteBlur
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        super.render(renderer, batch, tileset, engine)
        frameCountdown--
        if (frameCountdown <= 0) {
            kill()
        }
    }
}
