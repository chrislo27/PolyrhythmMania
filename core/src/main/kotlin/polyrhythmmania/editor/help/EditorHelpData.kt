package polyrhythmmania.editor.help

import com.badlogic.gdx.utils.Align


object EditorHelpData {
    
    fun createHelpData(): HelpData {
        return HelpData(mapOf(
                HelpData.ROOT_ID to EditorHelpDocRoot(),
                "controls" to EditorHelpDocControls(),
                "music_sync" to EditorHelpDocMusicSync(),
        ))
    }
    
}

class EditorHelpDocRoot : HelpDocument(
        "editor.dialog.help.title",
        listOf(
                LayerTitle("editorHelp.root.title"),
                LayerParagraph("editorHelp.root.pp0", 64f),
                LayerCol3(LayerButton("editorHelp.controls.title", "controls", false), LayerButton("editorHelp.music_sync.title", "music_sync", false), null)
        )
)

class EditorHelpDocControls : HelpDocument(
        "editorHelp.controls.title",
        listOf(
                LayerParagraph("editorHelp.controls.pp0", 50f),
                LayerParagraph("editorHelp.controls.pp1", 80f),
                LayerParagraph("editorHelp.controls.heading.general", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.general.keys", 300f, renderAlign = Align.topRight),
                        LayerParagraph("editorHelp.controls.keybinds.general.desc", 300f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.selectionTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.selectionTool.keys", 220f, renderAlign = Align.topRight),
                        LayerParagraph("editorHelp.controls.keybinds.selectionTool.desc", 220f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.tempoChangeTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.tempoChangeTool.keys", 120f, renderAlign = Align.topRight),
                        LayerParagraph("editorHelp.controls.keybinds.tempoChangeTool.desc", 120f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.musicVolumeTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.musicVolumeTool.keys", 100f, renderAlign = Align.topRight),
                        LayerParagraph("editorHelp.controls.keybinds.musicVolumeTool.desc", 100f),
                        leftProportion = 0.275f
                ),
                LayerParagraph("editorHelp.controls.heading.timeSigTool", 20f),
                LayerCol2(
                        LayerParagraph("editorHelp.controls.keybinds.timeSigTool.keys", 100f, renderAlign = Align.topRight),
                        LayerParagraph("editorHelp.controls.keybinds.timeSigTool.desc", 100f),
                        leftProportion = 0.275f
                ),
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
