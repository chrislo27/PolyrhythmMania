package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import io.github.chrislo27.paintbox.registry.AssetRegistry
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsMusicLoader
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.BeadsSoundLoader


class InitialAssetLoader : AssetRegistry.IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        manager.setLoader(BeadsSound::class.java, BeadsSoundLoader(InternalFileHandleResolver()))
        manager.setLoader(BeadsMusic::class.java, BeadsMusicLoader(InternalFileHandleResolver()))
        
        fun linearTexture(): TextureLoader.TextureParameter = TextureLoader.TextureParameter().apply {
            this.magFilter = Texture.TextureFilter.Linear
            this.minFilter = Texture.TextureFilter.Linear
        }
        
        AssetRegistry.loadAsset<Texture>("tileset_gba", "textures/gba_spritesheet.png")
        
        AssetRegistry.loadAsset<Texture>("ui_icon_tool_selection", "textures/ui/icon/tool/selection.png")
        AssetRegistry.loadAsset<Texture>("ui_icon_tool_tempo_change", "textures/ui/icon/tool/tempo_change.png")
        AssetRegistry.loadAsset<Texture>("ui_triangle_equilateral", "textures/ui/triangle_equilateral.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_right", "textures/ui/triangle_right.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_equilateral_bordered", "textures/ui/triangle_equilateral_bordered.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_right_bordered", "textures/ui/triangle_right_bordered.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_icon_buttons_editor", "textures/ui/icon/buttons_editor.png")
        AssetRegistry.loadAsset<Texture>("ui_icon_block_flat", "textures/ui/icon/block_flat.png")
        listOf("applause", "despawn", "explosion", "input_a", "input_d", "land", "retract", "side_collision",
                "spawn_a", "spawn_d",).forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_$it", "sounds/${it}.wav")
        }
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}