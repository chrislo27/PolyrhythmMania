package polyrhythmmania.editor.pane.dialog

import io.github.chrislo27.paintbox.ui.Pane
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane


open class DialogPane(val editorPane: EditorPane) : Pane() {
    
    val editor: Editor = editorPane.editor
    
}