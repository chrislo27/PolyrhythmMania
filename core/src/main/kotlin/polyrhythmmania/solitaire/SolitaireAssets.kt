package polyrhythmmania.solitaire

import com.badlogic.gdx.assets.AssetManager
import paintbox.registry.AssetRegistryInstance
import paintbox.registry.IAssetLoader


object SolitaireAssets : AssetRegistryInstance()

class SolitaireAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}
