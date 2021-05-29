package polyrhythmmania.editor.pane.dialog

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.BasicDialog


open class EditorDialog(val editorPane: EditorPane) : BasicDialog(editorPane.main) {

    val editor: Editor = editorPane.editor

}