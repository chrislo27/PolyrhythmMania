package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import paintbox.packing.Packable
import paintbox.packing.PackedSheet
import paintbox.packing.PackedSheetLoader
import paintbox.registry.AssetRegistry
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
        
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_tool", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("selection", "textures/ui/icon/tool/selection.png"),
                Packable("tempo_change", "textures/ui/icon/tool/tempo_change.png"),
                Packable("music_volume", "textures/ui/icon/tool/music_volume.png"),
                Packable("time_signature", "textures/ui/icon/tool/time_signature.png"),
        ), PackedSheet.Config(padding = 0, maxSize = 128, duplicateBorder = false)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_editor", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("toolbar_pause_color", "textures/ui/icon/toolbar/pause_color.png"),
                Packable("toolbar_play_color", "textures/ui/icon/toolbar/play_color.png"),
                Packable("toolbar_stop_color", "textures/ui/icon/toolbar/stop_color.png"),
                Packable("toolbar_pause_white", "textures/ui/icon/toolbar/pause_white.png"),
                Packable("toolbar_play_white", "textures/ui/icon/toolbar/play_white.png"),
                Packable("toolbar_stop_white", "textures/ui/icon/toolbar/stop_white.png"),
                Packable("toolbar_music", "textures/ui/icon/toolbar/music.png"),
                Packable("toolbar_metronome", "textures/ui/icon/toolbar/metronome.png"),
                Packable("toolbar_metronome_active", "textures/ui/icon/toolbar/metronome_active.png"),
                Packable("toolbar_tapalong", "textures/ui/icon/toolbar/tapalong.png"),
                Packable("toolbar_clapboard_shut", "textures/ui/icon/toolbar/clapboard_shut.png"),
                Packable("toolbar_clapboard_open", "textures/ui/icon/toolbar/clapboard_open.png"),
                Packable("menubar_new", "textures/ui/icon/menubar/new.png"),
                Packable("menubar_open", "textures/ui/icon/menubar/open.png"),
                Packable("menubar_save", "textures/ui/icon/menubar/save.png"),
                Packable("menubar_exit", "textures/ui/icon/menubar/exit.png"),
                Packable("menubar_undo", "textures/ui/icon/menubar/undo.png"),
                Packable("menubar_undo_white", "textures/ui/icon/menubar/undo_white.png"),
                Packable("arrow_long", "textures/ui/icon/arrow/arrow_long.png"),
                Packable("arrow_long_empty", "textures/ui/icon/arrow/arrow_long_empty.png"),
                Packable("arrow_long_semi", "textures/ui/icon/arrow/arrow_long_semi.png"),
                Packable("arrow_head", "textures/ui/icon/arrow/arrow_head.png"),
                Packable("arrow_head_empty", "textures/ui/icon/arrow/arrow_head_empty.png"),
                Packable("arrow_head_semi", "textures/ui/icon/arrow/arrow_head_semi.png"),
                Packable("arrow_short", "textures/ui/icon/arrow/arrow_short.png"),
                Packable("arrow_short_empty", "textures/ui/icon/arrow/arrow_short_empty.png"),
                Packable("arrow_short_semi", "textures/ui/icon/arrow/arrow_short_semi.png"),
                Packable("arrow_instantiator_right", "textures/ui/icon/arrow/instantiator_right_arrow.png"),
                Packable("arrow_pointer_finger", "textures/ui/icon/arrow/pointer_finger.png"),
                Packable("cursor_no_tail", "textures/ui/icon/cursor_no_tail.png"),
                Packable("cursor_thin_tail", "textures/ui/icon/cursor_thin_tail.png"),
                Packable("cursor_wide_tail", "textures/ui/icon/cursor_wide_tail.png"),
                Packable("informational", "textures/ui/icon/informational.png"),
                Packable("settings", "textures/ui/icon/settings.png"),
                Packable("help", "textures/ui/icon/help.png"),
        ), PackedSheet.Config(padding = 0, maxSize = 256, duplicateBorder = false)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_editor_linear", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("x", "textures/ui/x.png"),
        ), PackedSheet.Config(padding = 0, maxSize = 1024, duplicateBorder = false, 
                atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_editor_help", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("arrow_right", "textures/ui/icon/help/arrow_right.png"),
                Packable("home", "textures/ui/icon/help/home.png"),
        ), PackedSheet.Config(padding = 0, maxSize = 512, duplicateBorder = false, 
                atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAsset<Texture>("ui_triangle_equilateral", "textures/ui/triangle_equilateral.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_right", "textures/ui/triangle_right.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_equilateral_bordered", "textures/ui/triangle_equilateral_bordered.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_right_bordered", "textures/ui/triangle_right_bordered.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_icon_block_flat", "textures/ui/icon/block_flat.png")
        AssetRegistry.loadAsset<Texture>("github_mark", "textures/github_mark.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("logo_2lines_en", "textures/logo/logo_2lines_en.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("logo_2lines_ja", "textures/logo/logo_2lines_ja.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("pause_rod", "textures/pause/rod.png")
        AssetRegistry.loadAsset<Texture>("pause_square", "textures/pause/bg_square.png")
        listOf("applause", "despawn", "explosion", "input_a", "input_d", "land", "retract", "side_collision",
                "spawn_a", "spawn_d", "cowbell",).forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_$it", "sounds/${it}.wav")
        }
        listOf("enter", "exit", "robot_off", "robot_on").forEach {
            AssetRegistry.loadAsset<Sound>("sfx_pause_$it", "sounds/pause/${it}.ogg")
        }
        listOf("blip", "select", "deselect", "enter_game").forEach {
            AssetRegistry.loadAsset<Sound>("sfx_menu_$it", "sounds/menu/${it}.ogg")
        }
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}