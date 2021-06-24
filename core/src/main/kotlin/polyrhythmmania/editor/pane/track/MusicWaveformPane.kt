package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.ui.Pane
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.pane.EditorPane
import kotlin.math.floor


class MusicWaveformPane(val editorPane: EditorPane) : Pane() {
    
//    data class Segment(var beat: Float, var isLoopPoint: Boolean)
//    
//    object SegmentStack : ResourceStack<Segment>() {
//        override fun newObject(): Segment = Segment(0f, false)
//        override fun resetBeforePushed(obj: Segment) {
//        }
//        override fun resetWhenFreed(obj: Segment?) {
//        }
//    }

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
        val trackView = editor.trackView
        val trackViewBeat = trackView.beat.get()
        val pxPerBeat = trackView.pxPerBeat.get()
        val leftBeat = trackViewBeat
        val rightBeat = trackViewBeat + (w / pxPerBeat)
        val leftSec = engine.tempos.beatsToSeconds(leftBeat, disregardSwing = true)
        val rightSec = engine.tempos.beatsToSeconds(rightBeat, disregardSwing = true)

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
        if (music != null && rightSec - leftSec > 0f && w >= 1f) {
            val tempos = engine.tempos
            val tempoChanges = editor.tempoChanges.getOrCompute()
            for (tc in tempoChanges) {
                val beat = tc.beat
                if (beat !in (leftBeat)..(rightBeat)) continue
                if (beat == 0f) continue
                segmentsInRenderZone.add(tempos.beatsToSeconds(tc.beat, disregardSwing = false))
            }

            val engineMusicData = engine.musicData
            val musicDelaySec = engineMusicData.computeMusicDelaySec()
            val loopParams = engineMusicData.loopParams
            if (loopParams.loopType == SamplePlayer.LoopType.LOOP_FORWARDS) {
                val loopDur = loopParams.endPointMs - loopParams.startPointMs
                if (loopDur > 0) {
                    var ms = loopParams.startPointMs + (musicDelaySec * 1000)
                    ms += ((leftSec * 1000.0 - ms) / loopDur).toInt() * loopDur // Jump ahead to avoid iterations
                    while (ms < (rightSec * 1000)) {
                        val s = (ms / 1000).toFloat()
                        if (s in leftSec..rightSec) {
                            segmentsInRenderZone.add(s)
                            if (printDebugStuff) {
                                println("Loop: point added at sec $s")
                            }
                        }
                        ms += loopDur
                    }
                }
            }
            
            segmentsInRenderZone.sort()
            
            if (printDebugStuff) {
                println("segments: $segmentsInRenderZone")
            }

            var currentSegmentBeat: Float = leftBeat
            var musicOffsetForLooping = -(musicDelaySec)
            
            var currentSegmentSec: Float = leftSec
            var blockPxOffset = 0f // Accumulated offset
            for (segment in 0 until (segmentsInRenderZone.size + 1)) {
                val segmentStartSec: Float = currentSegmentSec
                val segmentEndSec: Float = segmentsInRenderZone.getOrNull(segment) ?: rightSec
                
                val currentTempo: Float = tempos.tempoAtSeconds(segmentStartSec)
                val pxPerSec: Float = pxPerBeat / (60f / currentTempo)
                
                // DEBUG red lines at each segment split
//                val pc = batch.packedColor
//                batch.setColor(1f, 0f, 0f, 1f)
//                batch.fillRect(x + blockPxOffset, y - h, 1f, h)
//                batch.packedColor = pc
                
                // Find the first music seconds.
                val musicSeconds = (engineMusicData.getCorrectMusicPlayerPositionAt(segmentStartSec + 0.001f, delaySec = musicDelaySec) / 1000).toFloat()
                val startCurMusicSec = musicSeconds
                var currentMusicSec = startCurMusicSec
                var currentSubsegmentSec = segmentStartSec
                val originalBlockPxOffset = blockPxOffset
                // Break into chunks, going to the nearest whole seconds if possible
                var subsegmentIndex = 0
                while (currentSubsegmentSec < segmentEndSec) {
//                    val endSubsegmentSec = (floor(currentSubsegmentSec) + 1).coerceAtMost(segmentEndSec)
//                    val durSubsegmentSec = endSubsegmentSec - currentSubsegmentSec
                    val endMusicSec = (floor(currentMusicSec) + 1).coerceAtMost(startCurMusicSec + (segmentEndSec - segmentStartSec))
                    val durMusicSec = endMusicSec - currentMusicSec
                    if (durMusicSec <= 0f) break
                    
                    if (subsegmentIndex == 0 && printDebugStuff) {
                        println("subseg at ${segmentStartSec}:\t currentMusicSec: $currentMusicSec  musicSeconds: ${musicSeconds}  musicSecondsEpsilon: ${(engineMusicData.getCorrectMusicPlayerPositionAt(segmentStartSec + 0.0001f, delaySec = musicDelaySec) / 1000).toFloat()}")
                    }
                    
                    val texReg: TextureRegion? = editor.waveformWindow.getSecondsBlock(floor(currentMusicSec).toInt())
                    if (texReg != null) {
                        val u = texReg.u
                        val u2 = texReg.u2

                        val correctU = MathUtils.lerp(u, u2, currentMusicSec - floor(currentMusicSec))
                        val correctU2 = MathUtils.lerp(u, u2, 1f - ((floor(currentMusicSec) + 1f) - endMusicSec))

                        batch.draw(texReg.texture, x + blockPxOffset,
                                y - h, durMusicSec * pxPerSec, h,
                                correctU, texReg.v,
                                correctU2, texReg.v2)
                    }

                    currentSubsegmentSec += durMusicSec
                    currentMusicSec += durMusicSec
                    blockPxOffset += pxPerSec * durMusicSec
                    subsegmentIndex++
//                    if (subsegmentIndex >= 2) break
                }
                if (printDebugStuff) {
                    println("original: $originalBlockPxOffset  currentBlockPxOffset: $blockPxOffset  expectedNew: ${originalBlockPxOffset + pxPerSec * (segmentEndSec - segmentStartSec)}")
                    println("segment length: ${segmentEndSec - segmentStartSec}  subsegment accumulated: ${currentSubsegmentSec - segmentStartSec}")
                }
                // Force reset blockPxOffset in case of addition issues/floating-point error
                blockPxOffset = originalBlockPxOffset + pxPerSec * (segmentEndSec - segmentStartSec)
                
                
                currentSegmentSec = segmentEndSec
            }
            
        }

        batch.packedColor = lastPackedColor
    }
}