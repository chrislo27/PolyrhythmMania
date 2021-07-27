package polyrhythmmania.sidemodes

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StreamUtils
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader

/**
 * Holds some common side mode assets like the practice music.
 */
object SidemodeAssets : Disposable {
    
    val practiceTheme: BeadsMusic by lazy { GdxAudioReader.newMusic(Gdx.files.internal("music/practice.ogg"), null) }
    val polyrhythmTheme: BeadsMusic by lazy { GdxAudioReader.newMusic(Gdx.files.internal("music/Polyrhythm.ogg"), null) }

    override fun dispose() {
        if ((this::practiceTheme.getDelegate() as Lazy<*>).isInitialized()) {
            StreamUtils.closeQuietly(practiceTheme.musicSample)
        }
        if ((this::polyrhythmTheme.getDelegate() as Lazy<*>).isInitialized()) {
            StreamUtils.closeQuietly(polyrhythmTheme.musicSample)
        }
    }
}