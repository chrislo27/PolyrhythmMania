package polyrhythmmania.editor.help


object EditorHelpData {
    
    fun createHelpData(): HelpData {
        return HelpData(mapOf(
                HelpData.ROOT_ID to EditorRootDoc(),
        ))
    }
    
}

class EditorRootDoc : HelpDocument(
        "editorHelp.root.title",
        listOf(
                LayerTitle("editorHelp.root.title"),
                LayerParagraph("No content yet.", 200f),
        )
)