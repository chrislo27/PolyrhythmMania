package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.graphics.Texture
import paintbox.packing.Packable
import paintbox.packing.PackedSheet
import paintbox.packing.PackedSheetLoader
import paintbox.registry.AssetRegistry


class TilesetAssetLoader : AssetRegistry.IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        fun linearTexture(): TextureLoader.TextureParameter = TextureLoader.TextureParameter().apply {
            this.magFilter = Texture.TextureFilter.Linear
            this.minFilter = Texture.TextureFilter.Linear
        }
        
        AssetRegistry.loadAsset<Texture>("tileset_missing_tex", "textures/world/missing.png")
        AssetRegistry.loadAsset<Texture>("gba_spritesheet", "textures/world/gba_spritesheet.png")
        AssetRegistry.loadAsset<Texture>("green_grid", "textures/world/green_grid.png", linearTexture())
        
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_ui", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "skill_star",
                "skill_star_grey",
                "perfect",
                "perfect_failed",
                "perfect_hit",
        ).map { Packable(it, "textures/world/ui/$it.png") }, PackedSheet.Config(padding = 1, maxSize = 512, duplicateBorder = false,
                atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))

        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_gba", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "background_back",
                "background_middle",
                "background_fore",
                "cube_border",
                "cube_border_platform",
                "cube_border_z",
                "cube_face_x",
                "cube_face_y",
                "cube_face_z",
                "explosion_0",
                "explosion_1",
                "explosion_2",
                "explosion_3",
                "indicator_a",
                "indicator_dpad",
                "input_feedback_0",
                "input_feedback_1",
                "input_feedback_2",
                "piston_a",
                "piston_a_extended",
                "piston_a_extended_face_x",
                "piston_a_extended_face_z",
                "piston_a_partial",
                "piston_a_partial_face_x",
                "piston_a_partial_face_z",
                "piston_dpad",
                "piston_dpad_extended",
                "piston_dpad_extended_face_x",
                "piston_dpad_extended_face_z",
                "piston_dpad_partial",
                "piston_dpad_partial_face_x",
                "piston_dpad_partial_face_z",
                "platform",
                "platform_with_line",
                "red_line",
                "rods_borders",
                "rods_fill",
                "sign_a",
                "sign_a_shadow",
                "sign_bo",
                "sign_bo_shadow",
                "sign_dpad",
                "sign_dpad_shadow",
                "sign_n",
                "sign_n_shadow",
                "sign_ta",
                "sign_ta_shadow",
                "xyz",
        ).map { Packable(it, "textures/world/gba/parts/$it.png") } + listOf(
                "basket_back",
                "basket_front",
                "basket_front_face_z",
                "basket_rear",
                "hoop_back",
        ).map { Packable(it, "textures/world/dunk/$it.png") },
                PackedSheet.Config(padding = 1,
                        maxSize = 512 /* Found to be the smallest Po2 size without splitting into more texs */,
                        duplicateBorder = false,)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_hd", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "cube_border",
                "cube_border_platform",
                "cube_border_z",
                "cube_face_x",
                "cube_face_y",
                "cube_face_z",
                "explosion_0",
                "explosion_1",
                "explosion_2",
                "explosion_3",
                "indicator_a",
                "indicator_dpad",
                "input_feedback_0",
                "input_feedback_1",
                "input_feedback_2",
                "piston_a",
                "piston_a_extended",
                "piston_a_extended_face_x",
                "piston_a_extended_face_z",
                "piston_a_partial",
                "piston_a_partial_face_x",
                "piston_a_partial_face_z",
                "piston_dpad",
                "piston_dpad_extended",
                "piston_dpad_extended_face_x",
                "piston_dpad_extended_face_z",
                "piston_dpad_partial",
                "piston_dpad_partial_face_x",
                "piston_dpad_partial_face_z",
                "platform",
                "platform_with_line",
                "red_line",
                "rods_borders",
                "rods_fill",
                "sign_a",
                "sign_a_shadow",
                "sign_bo",
                "sign_bo_shadow",
                "sign_dpad",
                "sign_dpad_shadow",
                "sign_n",
                "sign_n_shadow",
                "sign_ta",
                "sign_ta_shadow",
        ).map { Packable(it, "textures/world/hd/parts/$it.tga") },
                PackedSheet.Config(padding = 2, maxSize = 2048, duplicateBorder = false, atlasMipMaps = true,
                        atlasMinFilter = Texture.TextureFilter.MipMapLinearLinear, atlasMagFilter = Texture.TextureFilter.Linear)))
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}