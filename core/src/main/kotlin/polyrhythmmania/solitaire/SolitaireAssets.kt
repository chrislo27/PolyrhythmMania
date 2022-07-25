package polyrhythmmania.solitaire

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import paintbox.packing.Packable
import paintbox.packing.PackedSheet
import paintbox.packing.PackedSheetLoader
import paintbox.registry.AssetRegistryInstance
import paintbox.registry.IAssetLoader
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame


object SolitaireAssets : AssetRegistryInstance() {
    
    init {
        Gdx.app.postRunnable { 
            PRManiaGame.instance.addDisposeCall {
                this.disposeQuietly()
            }
        }
    }
}

class SolitaireAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {        
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
                Packable("special", "textures/solitaire/special.png"),
                Packable("suit_a", "textures/solitaire/suit_a.png"),
                Packable("suit_a_small", "textures/solitaire/suit_a_small.png"),
                Packable("suit_launcher", "textures/solitaire/suit_launcher.png"),
                Packable("suit_launcher_small", "textures/solitaire/suit_launcher_small.png"),
                Packable("suit_piston", "textures/solitaire/suit_piston.png"),
                Packable("suit_piston_small", "textures/solitaire/suit_piston_small.png"),
                Packable("widget", "textures/solitaire/widget.png"),
                Packable("zone_outline", "textures/solitaire/zone_outline.png"),
        ), PackedSheet.Config(padding = 1, maxSize = 512, duplicateBorder = false)))
        
        SolitaireAssets.loadAsset("help_0", "textures/solitaire/help_0.png", linearTexture())
        SolitaireAssets.loadAsset("help_1", "textures/solitaire/help_1.png", linearTexture())
        SolitaireAssets.loadAsset("help_2", "textures/solitaire/help_2.png", linearTexture())
        SolitaireAssets.loadAsset("help_3", "textures/solitaire/help_3.png", linearTexture())
        
        SolitaireAssets.loadAsset<Sound>("sfx_note_C3", "sounds/solitaire/note_C3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_D3", "sounds/solitaire/note_D3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_E3", "sounds/solitaire/note_E3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_F3", "sounds/solitaire/note_F3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_G3", "sounds/solitaire/note_G3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_A3", "sounds/solitaire/note_A3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_B3", "sounds/solitaire/note_B3.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_note_C4", "sounds/solitaire/note_C4.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_flick", "sounds/solitaire/flick.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_win", "sounds/solitaire/win.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_card_deal", "sounds/solitaire/card_deal.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_card_pickup", "sounds/solitaire/card_pickup.ogg")
        SolitaireAssets.loadAsset<Sound>("sfx_card_putdown", "sounds/solitaire/card_putdown.ogg")
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}
