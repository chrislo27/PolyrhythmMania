package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import io.github.chrislo27.paintbox.registry.AssetRegistry


class InitalAssetLoader : AssetRegistry.IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        AssetRegistry.loadAsset<Texture>("tileset_gba", "textures/gba_spritesheet.png")
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}