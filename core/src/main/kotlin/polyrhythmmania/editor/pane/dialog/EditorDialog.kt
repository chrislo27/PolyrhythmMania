package polyrhythmmania.editor.pane.dialog

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.BasicDialog


abstract class EditorDialog(val editorPane: EditorPane, mergeTopAndContent: Boolean = false)
    : BasicDialog(editorPane.main, mergeTopAndContent) {

    val editor: Editor = editorPane.editor

    fun attemptClose() {
        if (canCloseDialog()) {
            onCloseDialog()
            editorPane.closeDialog()
            afterDialogClosed()
        }
    }

    /**
     * Returns false if we cannot close the dialog.
     */
    protected abstract fun canCloseDialog(): Boolean

    /**
     * Overridden by subclasses to do close cleanup.
     */
    protected open fun onCloseDialog() {
    }
    
    protected open fun afterDialogClosed() {
    }
    
    open fun canCloseWithEscKey(): Boolean = true
    
}