package polyrhythmmania.editor.music

import paintbox.binding.FloatVar
import paintbox.binding.Var
import polyrhythmmania.container.ExternalResource
import polyrhythmmania.editor.Editor
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.FullMusicSample
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
    val loopParams: Var<LoopParams> = Var(LoopParams.NO_LOOP_FORWARDS)
    val firstBeatSec: FloatVar = FloatVar(0f)
    val rate: FloatVar = FloatVar(1f)
    
    fun setMusic(beadsMusic: BeadsMusic, compressedMusicRes: ExternalResource) {
        if (this.beadsMusic != null) {
            removeMusic()
        }
        loopParams.set(LoopParams.NO_LOOP_FORWARDS)
        firstBeatSec.set(0f)
        this.beadsMusic = beadsMusic
        editor.container.setCompressedMusic(compressedMusicRes)
        rate.set(1f)
    }
    
    fun removeMusic() {
        val m = this.beadsMusic
        if (m != null) {
            m.musicSample.close()
            (m.musicSample as? FullMusicSample)?.pcmDataFile?.deleteIfExists()
            this.beadsMusic = null
            editor.container.setCompressedMusic(null)
        }
    }
}