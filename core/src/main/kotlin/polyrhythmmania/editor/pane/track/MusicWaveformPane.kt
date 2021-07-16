package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.Paintbox
import paintbox.ui.Pane
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.pane.EditorPane
import kotlin.math.floor


class MusicWaveformPane(val editorPane: EditorPane) : Pane() {

    private val segmentsInRenderZone: MutableList<Float> = mutableListOf()
    
    init {
        this.doClipping.set(true)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val renderBounds = this.contentZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val lastPackedColor = batch.packedColor

        val editor = editorPane.editor
        val engine = editor.engine
        val engineMusicData = engine.musicData
        val musicRate: Float = engineMusicData.rate
        val trackView = editor.trackView
        val trackViewBeat = trackView.beat.get()
        val pxPerBeat = trackView.pxPerBeat.get()
        val leftBeat = trackViewBeat
        val rightBeat = trackViewBeat + (w / pxPerBeat)

        val leftRealSec = engine.tempos.beatsToSeconds(leftBeat, disregardSwing = true)
        val rightRealSec = leftRealSec + (engine.tempos.beatsToSeconds(rightBeat, disregardSwing = true) - leftRealSec)
        
        // "Music seconds" (msec) are used since the music may have a different rate
        val leftMsec = leftRealSec * musicRate
        val rightMsec = rightRealSec * musicRate

        val segmentsInRenderZone = this.segmentsInRenderZone
        segmentsInRenderZone.clear()

        /*
        Break into seconds segments, starting at the beginning and splitting on things
        like tempo changes (converted to sec) and loop points.
         */
        
        val printDebugStuff = Gdx.input.isKeyJustPressed(Input.Keys.Y)
        if (printDebugStuff) println()
        
        batch.setColor(1f, 1f, 1f, 1f * apparentOpacity.get())
        val music = editor.musicData.beadsMusic
        if (music != null && rightMsec - leftMsec > 0f && w >= 1f) {
            val tempos = engine.tempos
            val tempoChanges = editor.tempoChanges.getOrCompute()
            for (tc in tempoChanges) {
                val beat = tc.beat
                if (beat !in (leftBeat)..(rightBeat)) continue
                if (beat == 0f) continue
                segmentsInRenderZone.add(tempos.beatsToSeconds(tc.beat, disregardSwing = false) * musicRate)
            }

            val musicDelaySec = engineMusicData.computeMusicDelaySec()
            val loopParams = engineMusicData.loopParams
            if (loopParams.loopType == SamplePlayer.LoopType.LOOP_FORWARDS) {
                val loopDur = loopParams.endPointMs - loopParams.startPointMs
                if (loopDur > 0) {
                    var ms = loopParams.startPointMs + (musicDelaySec * 1000) * musicRate
                    ms += ((leftMsec * 1000.0 - ms) / loopDur).toInt() * loopDur // Jump ahead to avoid iterations
                    while (ms < (rightMsec * 1000)) {
                        val s = (ms / 1000).toFloat()
                        if (s in leftMsec..rightMsec) {
                            segmentsInRenderZone.add(s)
                            if (printDebugStuff) {
                                println("Loop: point added at msec $s")
                            }
                        }
                        val oldMs = ms
                        ms += loopDur
                        if (ms <= oldMs) {
                            break // Prevent an infinite loop due to floating point addition error
                        }
                    }
                }
            }
            
            segmentsInRenderZone.sort()
            
            if (printDebugStuff) {
                println("segments: $segmentsInRenderZone")
                println("current music delay: ${musicDelaySec} sec  leftMsec: ${leftMsec}")
            }
            
            var currentSegmentMsec: Float = leftMsec
            var blockPxOffset = 0f // Accumulated offset
            for (segment in 0 until (segmentsInRenderZone.size + 1)) {
                val segmentStartMsec: Float = currentSegmentMsec
                val segmentEndMsec: Float = segmentsInRenderZone.getOrNull(segment) ?: rightMsec
                
                val currentTempo: Float = tempos.tempoAtSeconds(segmentStartMsec / musicRate)
                val pxPerMsec: Float = pxPerBeat / (60f / currentTempo) / musicRate
                
                // DEBUG red lines at each segment split
                if (Paintbox.debugMode) {
                    val pc = batch.packedColor
                    batch.setColor(1f, 0f, 0f, 1f)
                    batch.fillRect(x + blockPxOffset, y - h, 1f, h)
                    batch.packedColor = pc
                }
                
                // Find the first music seconds.
                val musicSeconds = (engineMusicData.getCorrectMusicPlayerPositionAt((segmentStartMsec / musicRate) + 0.001f, delaySec = musicDelaySec) / 1000).toFloat()
                val startCurMusicSec = musicSeconds
                var currentMusicSec = startCurMusicSec
                var currentSubsegmentMsec = segmentStartMsec
                val originalBlockPxOffset = blockPxOffset
                // Break into chunks, going to the nearest whole seconds if possible
                var subsegmentIndex = 0
                while (currentSubsegmentMsec < segmentEndMsec) {
                    val endMusicMsec = (floor(currentMusicSec) + 1).coerceAtMost(startCurMusicSec + (segmentEndMsec - segmentStartMsec))
                    val durMusicSec = endMusicMsec - currentMusicSec
                    if (durMusicSec <= 0f) break
                    
                    if (subsegmentIndex == 0 && printDebugStuff) {
                        println("segment ${segment} subseg 0 at ${segmentStartMsec} msec up to ${segmentEndMsec} msec:\t currentMusicSec: $currentMusicSec  musicSeconds: ${musicSeconds}")
                    }
                    
                    val texReg: TextureRegion? = editor.waveformWindow.getSecondsBlock(floor(currentMusicSec).toInt())
                    if (texReg != null) {
                        val u = texReg.u
                        val u2 = texReg.u2

                        val correctU = MathUtils.lerp(u, u2, currentMusicSec - floor(currentMusicSec))
                        val correctU2 = MathUtils.lerp(u, u2, 1f - ((floor(currentMusicSec) + 1f) - endMusicMsec))

                        batch.draw(texReg.texture, x + blockPxOffset,
                                y - h, durMusicSec * pxPerMsec, h,
                                correctU, texReg.v,
                                correctU2, texReg.v2)
                    }

                    currentSubsegmentMsec += durMusicSec
                    currentMusicSec += durMusicSec
                    blockPxOffset += pxPerMsec * durMusicSec
                    subsegmentIndex++
//                    if (subsegmentIndex >= 2) break
                }
                if (printDebugStuff) {
                    println("original: $originalBlockPxOffset  currentBlockPxOffset: $blockPxOffset  expectedNew: ${originalBlockPxOffset + pxPerMsec * (segmentEndMsec - segmentStartMsec)}")
                    println("segment length: ${segmentEndMsec - segmentStartMsec} msec  subsegment accumulated: ${currentSubsegmentMsec - segmentStartMsec}")
                }
                // Force reset blockPxOffset in case of addition issues/floating-point error
                blockPxOffset = originalBlockPxOffset + pxPerMsec * (segmentEndMsec - segmentStartMsec)
                
                
                currentSegmentMsec = segmentEndMsec
            }
            
        }

        batch.packedColor = lastPackedColor
    }
}