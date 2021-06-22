package polyrhythmmania.editor.help

import com.badlogic.gdx.utils.Disposable
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.layout.VBox


/**
 * A [HelpDocument] defines the content to be populated. It contains text, buttons, and images.
 * 
 * Content is separated into vertical layers. Each layer may have items in it with columnal hinting.
 * 
 * 
 */
open class HelpDocument(val title: String, val layers: List<Layer>)

abstract class DocumentRenderer : Disposable {
    
    open fun renderDocument(helpData: HelpData, doc: HelpDocument): UIElement {
        return Pane().apply {
            this.margin.set(Insets(0f, 0f, 0f, 4f))
            val vbox = VBox().also { vbox ->
                vbox.spacing.set(8f)
                vbox.temporarilyDisableLayouts {
                    doc.layers.forEach { layer ->
                        vbox += renderLayer(helpData, layer)
                    }
                }

                vbox.sizeHeightToChildren(10f)
            }
            this += vbox
            this.bounds.height.bind { vbox.bounds.height.useF() }
        }
    }
    
    abstract fun renderLayer(helpData: HelpData, layer: Layer): UIElement
}
