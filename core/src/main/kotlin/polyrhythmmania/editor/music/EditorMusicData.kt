package polyrhythmmania.editor.music

import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.Var
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import kotlin.io.path.deleteIfExists


class EditorMusicData {
    
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
    var firstBeat: FloatVar = FloatVar(0f)
    
    fun setMusic(beadsMusic: BeadsMusic) {
        if (this.beadsMusic != null) {
            removeMusic()
        }
        // FIXME should this be here?
        loopParams.set(LoopParams.NO_LOOP_FORWARDS)
        firstBeat.set(0f)
        this.beadsMusic = beadsMusic
    }
    
    fun removeMusic() {
        val m = this.beadsMusic
        if (m != null) {
            m.musicSample.close()
            m.musicSample.pcmDataFile.deleteIfExists()
            this.beadsMusic = null
        }
    }
}