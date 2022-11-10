package polyrhythmmania.init

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import paintbox.lazysound.LazySound
import paintbox.packing.Packable
import paintbox.packing.PackedSheet
import paintbox.packing.PackedSheetLoader
import paintbox.registry.AssetRegistry
import paintbox.registry.IAssetLoader
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsMusicLoader
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.BeadsSoundLoader
import polyrhythmmania.world.tileset.TransitionCurve


class InitialAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        manager.setLoader(BeadsSound::class.java, BeadsSoundLoader(InternalFileHandleResolver()))
        manager.setLoader(BeadsMusic::class.java, BeadsMusicLoader(InternalFileHandleResolver()))
        
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_tool", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("selection", "textures/ui/icon/tool/selection.png"),
                Packable("tempo_change", "textures/ui/icon/tool/tempo_change.png"),
                Packable("music_volume", "textures/ui/icon/tool/music_volume.png"),
                Packable("time_signature", "textures/ui/icon/tool/time_signature.png"),
        ), PackedSheet.Config(padding = 1, maxSize = 128, duplicateBorder = false)))
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
                Packable("toolbar_playtest_shut", "textures/ui/icon/toolbar/playtest_shut.png"),
                Packable("toolbar_playtest_open", "textures/ui/icon/toolbar/playtest_open.png"),
                Packable("toolbar_tileset_palette", "textures/ui/icon/toolbar/tileset_palette.png"),
                Packable("toolbar_results", "textures/ui/icon/toolbar/results.png"),
                Packable("toolbar_world_settings", "textures/ui/icon/toolbar/world_settings.png"),
                Packable("toolbar_texture_pack", "textures/ui/icon/toolbar/texture_pack.png"),
                Packable("menubar_new", "textures/ui/icon/menubar/new.png"),
                Packable("menubar_open", "textures/ui/icon/menubar/open.png"),
                Packable("menubar_open32", "textures/ui/icon/menubar/open32.png"),
                Packable("menubar_save", "textures/ui/icon/menubar/save.png"),
                Packable("menubar_exit", "textures/ui/icon/menubar/exit.png"),
                Packable("menubar_undo", "textures/ui/icon/menubar/undo.png"),
                Packable("menubar_undo_white", "textures/ui/icon/menubar/undo_white.png"),
                Packable("menubar_export", "textures/ui/icon/menubar/export.png"),
                Packable("menubar_export_base", "textures/ui/icon/menubar/export_base.png"),
                Packable("menubar_import", "textures/ui/icon/menubar/import.png"),
                Packable("menubar_trash", "textures/ui/icon/menubar/trash.png"),
                Packable("menubar_export_as_level", "textures/ui/icon/menubar/export_as_level.png"),
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
                Packable("allowed_tracks", "textures/ui/icon/allowed_tracks.png"),
                Packable("filter", "textures/ui/icon/filter.png"),
                Packable("refresh", "textures/ui/icon/refresh.png"),
                Packable("controls_help", "textures/ui/icon/controls_help.png"),
        ), PackedSheet.Config(padding = 1, maxSize = 256, duplicateBorder = false)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_editor_linear", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("x", "textures/ui/x.png"),
        ), PackedSheet.Config(padding = 1, maxSize = 1024, duplicateBorder = false, 
                atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_editor_help", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("arrow_right", "textures/ui/icon/help/arrow_right.png"),
                Packable("home", "textures/ui/icon/help/home.png"),
        ), PackedSheet.Config(padding = 1, maxSize = 512, duplicateBorder = false,
                atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAssetNoFile<PackedSheet>("ui_icon_editor_curves",
                PackedSheetLoader.PackedSheetLoaderParam(TransitionCurve.VALUES.map {
                    Packable(it.imageID, "textures/curves/${it.imageID}.png")
                }, PackedSheet.Config(padding = 0, maxSize = 1024, duplicateBorder = false,
                        atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAsset<Texture>("ui_triangle_equilateral", "textures/ui/triangle_equilateral.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_right", "textures/ui/triangle_right.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_equilateral_bordered", "textures/ui/triangle_equilateral_bordered.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_triangle_right_bordered", "textures/ui/triangle_right_bordered.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_colour_picker_arrow", "textures/ui/colour_picker_arrow.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_colour_picker_copy", "textures/ui/copy.png")
        AssetRegistry.loadAsset<Texture>("ui_colour_picker_paste", "textures/ui/paste.png")
        AssetRegistry.loadAsset<Texture>("ui_icon_block_flat", "textures/ui/icon/block_flat.png")
        AssetRegistry.loadAsset<Texture>("ui_rounded_textbox", "textures/ui/textbox.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("ui_robot_textbox", "textures/ui/textbox_robot.png")
        AssetRegistry.loadAsset<Texture>("results_score_bar", "textures/results/score_bar.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("results_score_bar_border", "textures/results/score_bar_border.png", linearTexture())
        AssetRegistry.loadAssetNoFile<PackedSheet>("results_ranking", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("try_again", "textures/results/ranking/try_again.png"),
                Packable("ok", "textures/results/ranking/ok.png"),
                Packable("superb", "textures/results/ranking/superb.png"),
        ), PackedSheet.Config(padding = 4, maxSize = 512, duplicateBorder = false,
                atlasMinFilter = Texture.TextureFilter.Linear, atlasMagFilter = Texture.TextureFilter.Linear)))
        AssetRegistry.loadAsset<Texture>("github_mark", "textures/mainmenu/github_mark.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("support_donate", "textures/mainmenu/support.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("mainmenu_rhre", "textures/mainmenu/rhre.png")
        AssetRegistry.loadAsset<Texture>("mainmenu_brm", "textures/mainmenu/brm.png")
        AssetRegistry.loadAsset<Texture>("logo_2lines_en", "textures/logo/logo_2lines_en.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("logo_2lines_ja", "textures/logo/logo_2lines_ja.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("logo_pome", "textures/logo/logo_pome.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("pause_rod", "textures/pause/rod.png")
        AssetRegistry.loadAsset<Texture>("pause_square", "textures/pause/bg_square.png")
        AssetRegistry.loadAsset<Texture>("dunk_background", "textures/world/dunk/basketball_bg.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("hud_vignette", "textures/ui/hud/vignette.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("hud_song_card", "textures/ui/hud/song_card.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("country_flags", "textures/flag_icon/flags32.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("library_default_banner", "textures/mainmenu/default_banner.png", linearTexture())
        AssetRegistry.loadAsset<Texture>("achievements_completed_mark", "textures/achievements/completed.png", linearTexture())
        (0..3).forEach { AssetRegistry.loadAsset<Texture>("endless_mode_help_$it", "textures/ui/endless_help/help_$it.png", linearTexture()) }
        AssetRegistry.loadAsset("solitaire_tableau", "textures/solitaire/tableau.png", linearTexture())
        AssetRegistry.loadAssetNoFile<PackedSheet>("achievements_icon", PackedSheetLoader.PackedSheetLoaderParam(listOf(
                Packable("compact_mode", "textures/achievements/compact_mode.png"),
                Packable("assemble", "textures/achievements/assemble.png"),
                Packable("assemble_gold", "textures/achievements/assemble_gold.png"),
                Packable("daily", "textures/achievements/daily.png"),
                Packable("daily_calendar", "textures/achievements/daily_calendar.png"),
                Packable("daily_gold", "textures/achievements/daily_gold.png"),
                Packable("dunk", "textures/achievements/dunk.png"),
                Packable("dunk_gold", "textures/achievements/dunk_gold.png"),
                Packable("editor", "textures/achievements/editor.png"),
                Packable("editor_gold", "textures/achievements/editor_gold.png"),
                Packable("endless", "textures/achievements/endless.png"),
                Packable("endless_daredevil", "textures/achievements/endless_daredevil.png"),
                Packable("endless_gold", "textures/achievements/endless_gold.png"),
                Packable("endless_no_regen", "textures/achievements/endless_no_regen.png"),
                Packable("explosion", "textures/achievements/explosion.png"),
                Packable("locked", "textures/achievements/locked.png"),
                Packable("maniac", "textures/achievements/maniac.png"),
                Packable("new_grad", "textures/achievements/new_grad.png"),
                Packable("new_grad_gold", "textures/achievements/new_grad_gold.png"),
                Packable("solitaire", "textures/achievements/solitaire.png"),
                Packable("solitaire_gold", "textures/achievements/solitaire_gold.png"),
                Packable("trophy1", "textures/achievements/trophy1.png"),
                Packable("trophy2", "textures/achievements/trophy2.png"),
        ), PackedSheet.Config(padding = 2, maxSize = 512, duplicateBorder = false,
                atlasMinFilter = Texture.TextureFilter.Nearest, atlasMagFilter = Texture.TextureFilter.Nearest)))
        
        listOf("applause", "despawn", "explosion", "input_a", "input_d", "land", "retract", "side_collision",
                "spawn_a", "spawn_d", "cowbell",).forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_$it", "sounds/${it}.wav")
        }
        listOf("perfect_fail", "monster_goal_ace",).forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_$it", "sounds/${it}.ogg")
        }
        listOf("moretimes_1", "moretimes_2",).forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_practice_$it", "sounds/practice/${it}.ogg")
        }
        listOf("dunk_callout", "basket_swoosh", "ok1", "ok2").forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_dunk_$it", "sounds/dunk/${it}.ogg")
        }
        AssetRegistry.loadAsset<BeadsSound>("sfx_skill_star", "sounds/skill_star.ogg")
        listOf("enter", "exit", "robot_off", "robot_on").forEach {
            AssetRegistry.loadAsset<Sound>("sfx_pause_$it", "sounds/pause/${it}.ogg")
        }
        listOf("blip", "select", "deselect", "enter_game").forEach {
            AssetRegistry.loadAsset<Sound>("sfx_menu_$it", "sounds/menu/${it}.ogg")
        }
        listOf("text_advance_1", "text_advance_2").forEach {
            AssetRegistry.loadAsset<Sound>("sfx_$it", "sounds/${it}.ogg")
        }
        listOf("fail_music_hi", "fail_music_nohi").forEach {
            AssetRegistry.loadAsset<LazySound>("sfx_$it", "sounds/${it}.ogg")
        }
        listOf("prepare", "compress", "shoot", "left", "middle_left", "middle_right", "right", "collide").forEach {
            AssetRegistry.loadAsset<BeadsSound>("sfx_asm_$it", "sounds/assemble/${it}.ogg")
        }
        AssetRegistry.loadAsset<LazySound>("sfx_reset_achievements", "sounds/menu/reset_achievements.ogg")
        AssetRegistry.loadAsset<Sound>("sfx_challenge_complete", "sounds/menu/challenge_complete.ogg")
        AssetRegistry.loadAsset<Sound>("sfx_silence", "sounds/silence.wav") // DEBUG
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
        AssetRegistry.bindAsset("loading_icon_rod", "textures/loading/rod_loading.png").let { (key, filename) ->
            assets[key] = Texture(filename)
        }
    }
}