package polyrhythmmania.editor.help




/**
 * A [Layer] is a layer of content for a [HelpDocument]
 */
sealed class Layer 

class LayerTitle(val text: String) : Layer()

class LayerParagraph(val text: String) : Layer()
