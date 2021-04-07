package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import io.github.chrislo27.paintbox.registry.AssetRegistry
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsMusicLoader
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.BeadsSoundLoader


class InitalAssetLoader : AssetRegistry.IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        manager.setLoader(BeadsSound::class.java, BeadsSoundLoader(InternalFileHandleResolver()))
        manager.setLoader(BeadsMusic::class.java, BeadsMusicLoader(InternalFileHandleResolver()))
        
        AssetRegistry.loadAsset<Texture>("tileset_gba", "textures/gba_spritesheet.png")
        
        listOf("applause", "despawn", "explosion", "input_a", "input_d", "land", "retract", "side_collision",
                "spawn_a", "spawn_d",).forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_$it", "sounds/${it}.wav")
        }
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}