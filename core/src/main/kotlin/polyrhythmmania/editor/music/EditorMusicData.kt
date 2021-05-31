package polyrhythmmania.editor.music

import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.Var
import polyrhythmmania.container.ExternalResource
import polyrhythmmania.editor.Editor
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import kotlin.io.path.deleteIfExists


class EditorMusicData(val editor: Editor) {
    
    var beadsMusic: BeadsMusic? = null
        private set(value) {
            if (field !== value) {
                field = value
                this.waveform = if (value == null) null else Waveform(value.musicSample)
            }
        }
    var waveform: Waveform? = null
        private set // Set when beadsMusic is set
    var loopParams: Var<LoopParams> = Var(LoopParams.NO_LOOP_FORWARDS)
    var firstBeatSec: FloatVar = FloatVar(0f)
    
    fun setMusic(beadsMusic: BeadsMusic, compressedMusicRes: ExternalResource) {
        if (this.beadsMusic != null) {
            removeMusic()
        }
        loopParams.set(LoopParams.NO_LOOP_FORWARDS)
        firstBeatSec.set(0f)
        this.beadsMusic = beadsMusic
        editor.container.setCompressedMusic(compressedMusicRes)
    }
    
    fun removeMusic() {
        val m = this.beadsMusic
        if (m != null) {
            m.musicSample.close()
            m.musicSample.pcmDataFile.deleteIfExists()
            this.beadsMusic = null
            editor.container.setCompressedMusic(null)
        }
    }
}