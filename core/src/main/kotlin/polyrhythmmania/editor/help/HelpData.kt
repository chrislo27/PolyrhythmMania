package polyrhythmmania.editor.help

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import java.util.*


/**
 * In-editor help data.
 *
 * Help documents are a page of text, buttons (that link to other documents or websites), and images.
 * Traversing through documents adds them to a stack and can be traversed backwards and forwards.
 *
 */
class HelpData(val documents: Map<String, HelpDocument>) {
    
    companion object {
        const val ROOT_ID: String = "root"
    }

    private val docStack: Deque<HelpDocument> = ArrayDeque()
    private val forwardStack: Deque<HelpDocument> = ArrayDeque()
    val currentDocument: Var<HelpDocument?> = Var(null)
    val hasBack: ReadOnlyVar<Boolean> = Var(false)
    val hasForward: ReadOnlyVar<Boolean> = Var(false)
    
    private fun resetBackForwardVars() {
        (hasBack as Var).set(docStack.size > 1)
        (hasForward as Var).set(forwardStack.isNotEmpty())
    }
    
    fun goToDoc(document: HelpDocument) {
        if (currentDocument.getOrCompute() == document) return
        forwardStack.clear()
        docStack.push(document)
        currentDocument.set(document)
        resetBackForwardVars()
    }
    
    fun goToRoot() {
        goToDoc(documents.getValue(ROOT_ID))
    }
    
    fun resetToRoot() {
        resetStackTo(documents.getValue(ROOT_ID))
    }
    
    fun resetStackTo(document: HelpDocument) {
        docStack.clear()
        forwardStack.clear()
        docStack.push(document)
        currentDocument.set(document)
        resetBackForwardVars()
    }

    fun backUp() {
        if (docStack.size >= 2) {
            val popped = docStack.pop()
            forwardStack.push(popped)
            val next = docStack.firstOrNull()
            currentDocument.set(next)
            resetBackForwardVars()
        }
    }
    
    fun forward() {
        if (forwardStack.isNotEmpty()) {
            val popped = forwardStack.pop()
            docStack.push(popped)
            currentDocument.set(popped)
            resetBackForwardVars()
        }
    }
}
