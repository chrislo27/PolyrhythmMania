package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import paintbox.ui.Pane
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
        val trackView = editor.trackView
        val trackViewBeat = trackView.beat.get()
        val pxPerBeat = trackView.pxPerBeat.get()
        val leftBeat = trackViewBeat
        val rightBeat = trackViewBeat + (w / pxPerBeat)
        val leftSec = engine.tempos.beatsToSeconds(leftBeat)
        val rightSec = engine.tempos.beatsToSeconds(rightBeat)

        val segmentsInRenderZone = this.segmentsInRenderZone
        segmentsInRenderZone.clear()
        
        val printDebugStuff = Gdx.input.isKeyJustPressed(Input.Keys.Y)
        
        batch.setColor(1f, 1f, 1f, 1f * apparentOpacity.get())
        val music = editor.musicData.beadsMusic
        if (music != null && rightSec - leftSec > 0f && w >= 1f) {
            val tempos = engine.tempos
            val tempoChanges = editor.tempoChanges.getOrCompute()
            for (tc in tempoChanges) {
                val beat = tc.beat
                if (beat !in (leftBeat)..(rightBeat)) continue
                if (beat == 0f) continue
                segmentsInRenderZone.add(tc.beat)
            }
            
            segmentsInRenderZone.sort()
            
            /*
            1. Break into beat segments, starting at the beginning and splitting on things like tempo changes.
            2. For each beat segment, render the appropriate seconds of music waveform.
             */

            val musicDelaySec = engine.musicData.computeMusicDelaySec()
            var currentSegmentBeat: Float = leftBeat
            var blockPxOffset = 0f // Accumulated offset
            for (segment in 0 until (segmentsInRenderZone.size + 1)) {
                val segmentStart: Float = currentSegmentBeat
                val segmentEnd: Float = segmentsInRenderZone.getOrNull(segment) ?: rightBeat
                val segmentStartSec: Float = tempos.beatsToSeconds(segmentStart, disregardSwing = true)
                val segmentEndSec: Float = tempos.beatsToSeconds(segmentEnd, disregardSwing = true)
                
                val currentTempo: Float = tempos.tempoAtBeat(segmentStart)
                val pxPerSec: Float = pxPerBeat / (60f / currentTempo)
                if (printDebugStuff) {
                    println("Segment: $segment  start/end: $segmentStart / $segmentEnd   start/end sec: $segmentStartSec / $segmentEndSec   tempo: $currentTempo   pxPerSec: $pxPerSec")
                }
                
                // For each beat segment, render the music in up-to-one-second chunks.
                // The offsets for the chunks are pre-offset by the music delay seconds
                // TODO support loop points.
                
                val segmentMusicStartSec = segmentStartSec - musicDelaySec
                val segmentMusicEndSec = segmentEndSec - musicDelaySec
                
                var currentSec = segmentMusicStartSec
                while (currentSec < segmentMusicEndSec) {
                    val currentSecFloor = floor(currentSec)
                    val currentPlusOne = floor(currentSec + 1f) // Up to nearest whole beat
                    val endSec = currentPlusOne.coerceAtMost(segmentMusicEndSec)
                    
                    val musicSec = currentSec
                    val musicSecFloor = floor(musicSec)
                    
                    val dur = endSec - currentSec
                    
                    val texReg: TextureRegion? = editor.waveformWindow.getSecondsBlock(musicSecFloor.toInt())
                    if (texReg != null) {
                        val u = texReg.u
                        val u2 = texReg.u2
                        
                        val correctU = MathUtils.lerp(u, u2, currentSec - currentSecFloor)
                        val correctU2 = MathUtils.lerp(u, u2, 1f - (currentPlusOne - endSec))
                        
                        batch.draw(texReg.texture, x + blockPxOffset,
                                y - h, dur * pxPerSec, h,
                                correctU, texReg.v,
                                correctU2, texReg.v2)
                    }

                    blockPxOffset += dur * pxPerSec
                    currentSec = endSec // Go up to nearest whole sec.
                }
                
                currentSegmentBeat = segmentEnd
            }
            
        }

        batch.packedColor = lastPackedColor
    }
}