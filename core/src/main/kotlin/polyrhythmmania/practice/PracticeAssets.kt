package polyrhythmmania.practice

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StreamUtils
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader


/**
 * Holds some common practice mode assets like the music.
 */
object PracticeAssets : Disposable {
    
    val practiceTheme: BeadsMusic by lazy { GdxAudioReader.newMusic(Gdx.files.internal("music/practice.ogg"), null) }

    override fun dispose() {
        if ((this::practiceTheme.getDelegate() as Lazy<*>).isInitialized()) {
            StreamUtils.closeQuietly(practiceTheme.musicSample)
        }
    }
}
