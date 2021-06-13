package polyrhythmmania.editor.help


object EditorHelpData {
    
    fun createHelpData(): HelpData {
        return HelpData(mapOf(
                HelpData.ROOT_ID to EditorHelpDocRoot(),
                "music_sync" to EditorHelpDocMusicSync(),
        ))
    }
    
}

class EditorHelpDocRoot : HelpDocument(
        "editor.dialog.help.title",
        listOf(
                LayerTitle("editorHelp.root.title"),
                LayerParagraph("editorHelp.root.pp0", 64f),
                LayerCol3(LayerButton("editorHelp.music_sync.title", "music_sync", false), null, null)
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
