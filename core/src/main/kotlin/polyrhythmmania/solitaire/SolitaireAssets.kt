package polyrhythmmania.solitaire

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import paintbox.packing.Packable
import paintbox.packing.PackedSheet
import paintbox.packing.PackedSheetLoader
import paintbox.registry.AssetRegistryInstance
import paintbox.registry.IAssetLoader


object SolitaireAssets : AssetRegistryInstance()

class SolitaireAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        fun linearTexture(): TextureLoader.TextureParameter = TextureLoader.TextureParameter().apply {
            this.magFilter = Texture.TextureFilter.Linear
            this.minFilter = Texture.TextureFilter.Linear
        }
        
        SolitaireAssets.loadAssetNoFile<PackedSheet>("cards", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("1", "textures/solitaire/1.png"),
                Packable("2", "textures/solitaire/2.png"),
                Packable("3", "textures/solitaire/3.png"),
                Packable("4", "textures/solitaire/4.png"),
                Packable("5", "textures/solitaire/5.png"),
                Packable("6", "textures/solitaire/6.png"),
                Packable("7", "textures/solitaire/7.png"),
                Packable("card_back", "textures/solitaire/card_back.png"),
                Packable("card_front", "textures/solitaire/card_front.png"),
                Packable("rod", "textures/solitaire/rod.png"),
                Packable("suit_a", "textures/solitaire/suit_a.png"),
                Packable("suit_a_small", "textures/solitaire/suit_a_small.png"),
                Packable("suit_launcher", "textures/solitaire/suit_launcher.png"),
                Packable("suit_launcher_small", "textures/solitaire/suit_launcher_small.png"),
                Packable("suit_piston", "textures/solitaire/suit_piston.png"),
                Packable("suit_piston_small", "textures/solitaire/suit_piston_small.png"),
                Packable("widget", "textures/solitaire/widget.png"),
        ), PackedSheet.Config(padding = 1, maxSize = 512, duplicateBorder = false)))
        
        SolitaireAssets.loadAsset<Sound>("sfx_base_note", "sounds/solitaire/base_note.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_base_note_delayed", "sounds/solitaire/base_note_delayed.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_flick", "sounds/solitaire/flick.ogg")
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}
