package polyrhythmmania.sidemodes

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StreamUtils
import paintbox.registry.AssetRegistry
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.GdxAudioReader

/**
 * Holds some common side mode assets like the practice music.
 */
object SidemodeAssets : Disposable {

    /**
     * 129 BPM. 16 beats long.
     */
    val practiceTheme: BeadsMusic by lazy { GdxAudioReader.newMusic(Gdx.files.internal("music/practice.ogg"), null) }

    /**
     * 129 BPM. 88 beats long. Intro is 8 beats long.
     */
    val polyrhythmTheme: BeadsMusic by lazy { GdxAudioReader.newMusic(Gdx.files.internal("music/Polyrhythm.ogg"), null) }
    
    val assembleTheme: BeadsMusic by lazy { GdxAudioReader.newMusic(Gdx.files.internal("music/assemble.ogg"), null) }
    val assembleSfx: Map<String, BeadsSound> by lazy {
        listOf("prepare", "compress", "shoot", "left", "middle_left", "middle_right", "right").associate {
            "sfx_asm$it" to GdxAudioReader.newSound(Gdx.files.internal("sounds/assemble/${it}.ogg"))
        }
    }

    override fun dispose() {
        if ((this::practiceTheme.getDelegate() as Lazy<*>).isInitialized()) {
            StreamUtils.closeQuietly(practiceTheme.musicSample)
        }
        if ((this::polyrhythmTheme.getDelegate() as Lazy<*>).isInitialized()) {
            StreamUtils.closeQuietly(polyrhythmTheme.musicSample)
        }
        if ((this::assembleTheme.getDelegate() as Lazy<*>).isInitialized()) {
            StreamUtils.closeQuietly(assembleTheme.musicSample)
        }
    }
}