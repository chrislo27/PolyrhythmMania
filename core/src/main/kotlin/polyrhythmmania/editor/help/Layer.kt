package polyrhythmmania.editor.help

import com.badlogic.gdx.utils.Align
import paintbox.font.TextAlign


/**
 * A [Layer] is a layer of content for a [HelpDocument]
 */
sealed class Layer 

interface LayerFixedHeight
interface LayerSizesToChildren

class LayerTitle(val text: String) : Layer(), LayerFixedHeight

class LayerParagraph(val text: String, val allocatedHeight: Float,
                     val renderAlign: Int = Align.topLeft,
                     val textAlign: TextAlign = TextAlign.fromInt(renderAlign))
    : Layer(), LayerFixedHeight

class LayerVbox(val layers: List<Layer>) : Layer(), LayerSizesToChildren
class LayerCol2(val left: Layer?, val right: Layer?, val leftProportion: Float = 0.5f) : Layer(), LayerSizesToChildren
class LayerCol3(val left: Layer?, val mid: Layer?, val right: Layer?) : Layer(), LayerSizesToChildren
class LayerCol3Asymmetric(val left: Layer?, val right: Layer?, val moreLeft: Boolean = true) : Layer(), LayerSizesToChildren

class LayerButton(val text: String, val link: String, val external: Boolean) : Layer(), LayerFixedHeight
class LayerImage(val texturePath: String, val allocatedHeight: Float) : Layer(), LayerFixedHeight
