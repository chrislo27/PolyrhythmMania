package polyrhythmmania.ui

import paintbox.PaintboxGame
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.ui.control.TextField
import polyrhythmmania.util.DecimalFormats
import java.text.DecimalFormat


class DecimalTextField(
    startingValue: Float,
    decimalFormat: DecimalFormat = DecimalFormats["0.0##"],
    font: PaintboxFont = PaintboxGame.gameInstance.debugFont
) : TextField(font) {

    val value: FloatVar = FloatVar(startingValue)
    val decimalFormat: Var<DecimalFormat> = Var(decimalFormat)
    val allowNegatives: BooleanVar = BooleanVar(true)

    init {
        this.text.set(decimalToStr())
        this.inputFilter.bind { 
            val df = this@DecimalTextField.decimalFormat.use()
            val negatives = allowNegatives.use()
            val symbols = df.decimalFormatSymbols
            ;
            { c: Char ->
                (negatives && c == symbols.minusSign) || (c in '0'..'9') || (c == symbols.decimalSeparator)
            }
        }
        this.text.addListener { t ->
            if (hasFocus.get()) {
                try {
                    val newValue = this.decimalFormat.getOrCompute().parse(t.getOrCompute())?.toFloat()
                    if (newValue != null) {
                        value.set(newValue)
                    }
                } catch (ignored: Exception) {
                }
            }
        }
        hasFocus.addListener { f ->
            if (!f.getOrCompute()) {
                this.text.set(decimalToStr())
            }
        }
        this.setOnRightClick {
            requestFocus()
            text.set("")
        }
    }

    private fun decimalToStr(): String {
        return decimalFormat.getOrCompute().format(value.get())
    }
}