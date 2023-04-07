package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import paintbox.packing.Packable
import paintbox.packing.PackedSheet
import paintbox.packing.PackedSheetLoader
import paintbox.registry.AssetRegistry
import paintbox.registry.IAssetLoader


class TilesetAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        AssetRegistry.loadAsset<Texture>("tileset_missing_tex", "textures/world/missing.png")
        AssetRegistry.loadAsset<Texture>("gba_spritesheet", "textures/world/gba_spritesheet.png")
        AssetRegistry.loadAsset<Texture>("green_grid", "textures/world/green_grid.png", linearTexture())
        
        AssetRegistry.loadAsset<Texture>("world_light_spotlight", "textures/world/lighting/spotlight.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("world_light_circular_aliased", "textures/world/lighting/circular_aliased.tga", linearTexture())
        AssetRegistry.loadAsset<Texture>("world_light_circular_antialiased", "textures/world/lighting/circular_antialiased.tga", linearTexture())
        
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_ui", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "skill_star",
                "skill_star_grey",
                "perfect",
                "perfect_failed",
                "perfect_hit",
        ).map { Packable(it, "textures/world/ui/$it.png") },
                PackedSheet.Config(
                        padding = 1, maxSize = 512, duplicateBorder = false,
                        atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear
                )))
        AssetRegistry.loadAsset<Texture>("big_circle_ui", "textures/world/ui/big_circle.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("endless_lives_ui", "textures/world/ui/endless_lives.png")
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_ui_lives", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "heart",
                "heart_broken",
                "heart_broken_cross",
                "heart_checkmark",
                "heart_noface",
                "heart_outline",
                "heart_outline_noface",
        ).map { Packable(it, "textures/world/ui/lives/$it.png") },
                PackedSheet.Config(
                        padding = 1, maxSize = 128, duplicateBorder = false,
                        atlasMinFilter = Texture.TextureFilter.Nearest, atlasMagFilter = Texture.TextureFilter.Nearest
                )))
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_ui_boss", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "boss",
                "player",
        ).map { Packable(it, "textures/world/ui/boss/$it.png") },
                PackedSheet.Config(
                        padding = 1, maxSize = 128, duplicateBorder = false,
                        atlasMinFilter = Texture.TextureFilter.Nearest, atlasMagFilter = Texture.TextureFilter.Nearest
                )))

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
                "defective_rods_borders",
                "defective_rods_fill",
        ).map { Packable(it, "textures/world/gba/parts/$it.png") } + listOf(
                "basket_back",
                "basket_front",
                "basket_front_face_z",
                "basket_rear",
                "hoop_back",
                "dunk_star",
        ).map { Packable(it, "textures/world/dunk/${it.substringAfter("dunk_")}.png") } + listOf(
                "asm_lane_border",
                "asm_lane_sides",
                "asm_lane_top",
                "asm_centre_perp",
                "asm_centre_perp_target",
                "asm_cube_face_y",
                "asm_piston_a",
                "asm_piston_a_extended",
                "asm_piston_a_partial",
                "asm_widget_complete",
                "asm_widget_complete_blur",
                "asm_widget_roll",
        ).map { Packable(it, "textures/world/assemble/${it.substringAfter("asm_")}.png") },
                PackedSheet.Config(padding = 1,
                        maxSize = 512 /* Found to be the smallest power of 2 size without splitting into more texs */,
                        duplicateBorder = false,)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_gba_title", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "cube_border_z",
                "cube_face_x",
        ).map { Packable(it, "textures/world/gba/title/$it.png") },
                PackedSheet.Config(padding = 1, maxSize = 128, duplicateBorder = false,)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_hd", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                "background_back",
                "background_middle",
                "background_fore",
        ).map { Packable(it, "textures/world/hd/parts/$it.png") } + listOf(
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
                
                "defective_rods_borders",
                "defective_rods_fill",
        ).map { Packable(it, "textures/world/hd/parts/$it.tga") },
                PackedSheet.Config(padding = 2, maxSize = 2048, duplicateBorder = false, atlasMipMaps = true,
                        atlasMinFilter = Texture.TextureFilter.MipMapLinearLinear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("tileset_arcade", PackedSheetLoader.PackedSheetLoaderParam(listOf(
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
                
                "defective_rods_borders",
                "defective_rods_fill",
        ).map { Packable(it, "textures/world/arcade/parts/$it.tga") },
                PackedSheet.Config(padding = 1, maxSize = 512, duplicateBorder = false, atlasMipMaps = false,
                        atlasMinFilter = Texture.TextureFilter.Nearest, atlasMagFilter = Texture.TextureFilter.Nearest)))
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}