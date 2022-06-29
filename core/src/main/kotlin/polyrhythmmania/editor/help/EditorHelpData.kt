package polyrhythmmania.editor.help

import com.badlogic.gdx.utils.Align
import paintbox.ui.area.Insets
import polyrhythmmania.PRManiaGame


object EditorHelpData {
    
    fun createHelpData(): HelpData {
        return HelpData(mapOf(
                HelpData.ROOT_ID to EditorHelpDocRoot(),
                "glossary" to EditorHelpDocGlossary(),
                "controls" to EditorHelpDocControls(),
                "music_sync" to EditorHelpDocMusicSync(),
                "texture_packs" to EditorHelpDocTexturePacks(),
                "exporting" to EditorHelpDocExporting(),
                "prmproj" to EditorHelpDocPrmproj(),
        ))
    }
    
}

class EditorHelpDocRoot : HelpDocument(
        "editorHelp.root.title",
        listOf(
                LayerTitle("editorHelp.root.title"),
                LayerParagraph("editorHelp.root.pp0", 48f),
                LayerCol3(
                        LayerButton("editorHelp.glossary.title", "glossary", false),
                        LayerButton("editorHelp.controls.title", "controls", false),
                        LayerButtonWithNewIndicator("editorHelp.prmproj.title", "prmproj", false, PRManiaGame.instance.settings.newIndicatorEditorHelpPrmproj),
                ),
                LayerCol3(
                        LayerButton("editorHelp.music_sync.title", "music_sync", false),
                        LayerButtonWithNewIndicator("editorHelp.exporting.title", "exporting", false, PRManiaGame.instance.settings.newIndicatorEditorHelpExporting),
                        LayerButtonWithNewIndicator("editorHelp.texture_packs.title", "texture_packs", false, PRManiaGame.instance.settings.newIndicatorEditorHelpTexpack),
                ),
        )
)

class EditorHelpDocControls : HelpDocument(
        "editorHelp.controls.title",
        listOf(
                LayerParagraph("editorHelp.controls.pp0", 50f),
                LayerParagraph("editorHelp.controls.pp1", 80f),
                LayerParagraph("editorHelp.controls.pp2", 60f),
                LayerParagraph("editorHelp.controls.heading.general", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.general.keys", 540f, renderAlign = Align.topRight, padding = keybindsKeysPadding),
                        LayerParagraph("editorHelp.controls.keybinds.general.desc", 540f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.selectionTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.selectionTool.keys", 220f, renderAlign = Align.topRight, padding = keybindsKeysPadding),
                        LayerParagraph("editorHelp.controls.keybinds.selectionTool.desc", 220f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.tempoChangeTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.tempoChangeTool.keys", 130f, renderAlign = Align.topRight, padding = keybindsKeysPadding),
                        LayerParagraph("editorHelp.controls.keybinds.tempoChangeTool.desc", 130f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.musicVolumeTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.musicVolumeTool.keys", 100f, renderAlign = Align.topRight, padding = keybindsKeysPadding),
                        LayerParagraph("editorHelp.controls.keybinds.musicVolumeTool.desc", 100f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.timeSigTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.timeSigTool.keys", 100f, renderAlign = Align.topRight, padding = keybindsKeysPadding),
                        LayerParagraph("editorHelp.controls.keybinds.timeSigTool.desc", 100f),
                        leftProportion = 0.275f
                ),
        )
) {
    companion object {
        private val keybindsKeysPadding: Insets = Insets(4f, 4f, 8f, 0f)
    }
}

class EditorHelpDocGlossary : HelpDocument(
        "editorHelp.glossary.title",
        listOf(
                LayerParagraph("editorHelp.glossary.pp0", 50f),
                LayerParagraph("editorHelp.glossary.track", 64f, renderAlign = Align.bottomLeft),
                LayerImage("textures/help/glossary/track.png", 288f),
                LayerParagraph("editorHelp.glossary.block", 100f, renderAlign = Align.bottomLeft),
                LayerImage("textures/help/glossary/blocks.png", 288f),
                LayerParagraph("editorHelp.glossary.tempoChange", 64f, renderAlign = Align.bottomLeft),
                LayerImage("textures/help/glossary/tempo_change.png", 92f),
                LayerParagraph("editorHelp.glossary.musicVolume", 64f, renderAlign = Align.bottomLeft),
                LayerImage("textures/help/glossary/music_volume.png", 38f),
                LayerParagraph("editorHelp.glossary.timeSignature", 64f, renderAlign = Align.bottomLeft),
                LayerImage("textures/help/glossary/time_signature.png", 102f),
        )
)


class EditorHelpDocMusicSync : HelpDocument(
        "editorHelp.music_sync.title",
        listOf(
                LayerParagraph("editorHelp.music_sync.pp0", 64f),
                LayerParagraph("editorHelp.music_sync.pp1", 40f),
                LayerImage("textures/help/music_sync/toolbar.png", 150f),
                LayerParagraph("editorHelp.music_sync.pp2", 40f),
                LayerImage("textures/help/music_sync/adjust_music_dialog_no_changes.png", 500f),
                LayerParagraph("editorHelp.music_sync.pp3", 175f),
                LayerImage("textures/help/music_sync/adjust_music_dialog_first_beat.png", 219f),
                LayerParagraph("editorHelp.music_sync.pp4", 125f),
                LayerCol3Asymmetric(LayerVbox(listOf(
                        LayerParagraph("editorHelp.music_sync.pp4b", 70f),
                        LayerParagraph("editorHelp.music_sync.pp5", 250f),
                )), LayerImage("textures/help/music_sync/music_sync_marker.png", 350f), moreLeft = true),
        )
)

class EditorHelpDocTexturePacks : HelpDocument(
        "editorHelp.texture_packs.title",
        listOf(
                LayerParagraph("editorHelp.texture_packs.pp0", 80f),
                LayerImage("textures/help/texture_packs/change_starting_texture_pack.png", 274f),
                LayerParagraph("editorHelp.texture_packs.pp1", 64f),
                LayerImage("textures/help/texture_packs/dialog.png", 500f),
                LayerParagraph("editorHelp.texture_packs.pp2", 500f),
        )
)

class EditorHelpDocExporting : HelpDocument(
        "editorHelp.exporting.title",
        listOf(
                LayerParagraph("editorHelp.exporting.pp0", 65f),
                LayerCol3(
                        null,
                        LayerButtonWithNewIndicator("editorHelp.prmproj.title", "prmproj", false, PRManiaGame.instance.settings.newIndicatorEditorHelpPrmproj),
                        null
                ),
                LayerParagraph("editorHelp.exporting.pp1", 110f, padding = Insets(4f + 10f, 4f, 8f, 8f)),
                LayerParagraph("editorHelp.exporting.pp2", 32f),
                LayerImage("textures/help/exporting/export_button.png", 42f),
                LayerParagraph("editorHelp.exporting.pp3", 160f),
                LayerImage("textures/help/exporting/level_metadata.png", 370f),
                LayerParagraph("editorHelp.exporting.pp4", 32f),
                LayerImage("textures/help/exporting/example.png", 400f),
        )
)

class EditorHelpDocPrmproj : HelpDocument(
        "editorHelp.prmproj.title",
        listOf(
                LayerParagraph("editorHelp.prmproj.pp0", 250f),
                LayerCol3(
                        null,
                        LayerButton("editorHelp.exporting.title", "exporting", false),
                        null
                ),
        )
)
