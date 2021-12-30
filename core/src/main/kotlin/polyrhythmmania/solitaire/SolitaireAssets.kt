package polyrhythmmania.solitaire

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Sound
import paintbox.registry.AssetRegistryInstance
import paintbox.registry.IAssetLoader


object SolitaireAssets : AssetRegistryInstance()

class SolitaireAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        SolitaireAssets.loadAsset<Sound>("sfx_base_note", "sounds/solitaire/base_note.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_base_note_delayed", "sounds/solitaire/base_note_delayed.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_flick", "sounds/solitaire/flick.ogg")
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}
