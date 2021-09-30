package polyrhythmmania.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxGame
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.util.ColorStack
import paintbox.util.Vector2Stack
import paintbox.util.gdxutils.drawQuad
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sign


open class ColourPicker(val hasAlpha: Boolean, font: PaintboxFont = PaintboxGame.gameInstance.debugFont, scale: Float = 1f)
    : Control<ColourPicker>() {
    companion object {
        const val COLOUR_PICKER_SKIN_ID: String = "ColourPicker"
        
        init {
            DefaultSkins.register(COLOUR_PICKER_SKIN_ID, SkinFactory { element: ColourPicker ->
                ColourPickerSkin(element)
            })
        }
    }

    data class HSVA(val hue: Var<Int> = Var(0), val saturation: Var<Int> = Var(0), val value: Var<Int> = Var(100), val alpha: Var<Int> = Var(255))

    private val hsv: HSVA = HSVA()
    val currentColor: ReadOnlyVar<Color> = Var.sideEffecting(Color(1f, 1f, 1f, 1f)) { c ->
        c.fromHsv(hsv.hue.use() % 360f, (hsv.saturation.use() / 100f).coerceIn(0f, 1f), (hsv.value.use() / 100f).coerceIn(0f, 1f))
        c.a = (hsv.alpha.use() / 255f).coerceIn(0f, 1f)
        c
    }
    
    val textColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    
    private val hueArrow: MovingArrow = MovingArrow(0, 360, hsv.hue)
    private val satArrow: MovingArrow = MovingArrow(0, 100, hsv.saturation)
    private val valueArrow: MovingArrow = MovingArrow(0, 100, hsv.value)
    private val alphaArrow: MovingArrow = MovingArrow(0, 255, hsv.alpha)
    private val rgbTextField: TextField
    
    init {
        val rows = if (hasAlpha) 5 else 4
        val rowHeightMultiplier = 1f / rows
        
        this.bounds.height.set(160f)
        
        val vbox = VBox().apply { 
            this.spacing.set(0f)      
        }

        fun createTransparencyNode(): ImageNode {
            return ImageNode(null, ImageRenderingMode.FULL).also { im ->
                im.textureRegion.sideEffecting(TextureRegion(PRManiaGame.instance.colourPickerTransparencyGrid)) { tr ->
                    tr?.setRegion(0, 0, im.bounds.width.useF().toInt(), im.bounds.height.useF().toInt())
                    tr
                }
            }
        }
        
        fun createPropertyPane(varr: Var<Int>, label: String, arrowBgPane: UIElement, movingArrow: MovingArrow): Pane {
            return Pane().apply {
                this.bindHeightToParent(multiplier = rowHeightMultiplier)
                this += TextLabel(label, font).apply {
                    Anchor.CentreLeft.configure(this)
                    this.bounds.width.set(32f)
                    this.renderAlign.set(Align.right)
                    this.setScaleXY(scale)
                    this.textColor.bind { this@ColourPicker.textColor.use() }
                }
                val parentCtr = Pane().apply {
                    this.bounds.x.set(32f)
                    this.bindWidthToParent(adjust = -32f)
                    this.margin.set(Insets(2f, 2f, 0f, 0f))
                }
                this += parentCtr
                parentCtr += Pane().apply {
                    this.bindWidthToParent(multiplier = 0.75f)
                    this += arrowBgPane
                    this.margin.set(Insets(2f, 2f, 4f, 4f))
                }
                arrowBgPane += movingArrow
                parentCtr += Pane().apply {
                    Anchor.CentreRight.configure(this)
                    this.bindWidthToParent(multiplier = 0.25f)
                    this.margin.set(Insets(2f, 2f, 4f, 4f))
                    this += RectElement(Color(0f, 0f, 0f, 0.9f)).apply {
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this.padding.set(Insets(1f))
                        this += TextField(font = font).apply {
                            this.text.set("${varr.getOrCompute()}")
                            this.inputFilter.set({ c -> c in '0'..'9' })
                            this.textColor.bind { this@ColourPicker.textColor.use() }
                            varr.addListener { v ->
                                this.text.set("${v.getOrCompute()}")
                            }
                            this.text.addListener { t ->
                                if (hasFocus.get()) {
                                    val newValue = t.getOrCompute().toIntOrNull()
                                    if (newValue != null) {
                                        varr.set(newValue.coerceIn(movingArrow.minValue, movingArrow.maxValue))
                                    }
                                }
                            }
                            hasFocus.addListener { f ->
                                if (!f.getOrCompute()) {
                                    this.text.set("${varr.getOrCompute()}")
                                }
                            }
                        }
                    }
                }
            }
        }
        val huePane = createPropertyPane(hsv.hue, "H: ",
                ImageNode(TextureRegion(PRManiaGame.instance.colourPickerHueBar), ImageRenderingMode.FULL),
                hueArrow)
        val satPane = createPropertyPane(hsv.saturation, "S: ",
                Gradient().also { grad ->
                    grad.leftColor.sideEffecting { c -> 
                        c.fromHsv(hsv.hue.use().toFloat(), 0f, hsv.value.use() / 100f)
                    }
                    grad.rightColor.sideEffecting { c ->
                        c.fromHsv(hsv.hue.use().toFloat(), 1f, hsv.value.use() / 100f)
                    }
                }, satArrow)
        val valuePane = createPropertyPane(hsv.value, "V: ",
                Gradient().also { grad ->
                    grad.leftColor.sideEffecting { c -> 
                        c.fromHsv(hsv.hue.use().toFloat(), hsv.saturation.use() / 100f, 0f)
                    }
                    grad.rightColor.sideEffecting { c ->
                        c.fromHsv(hsv.hue.use().toFloat(), hsv.saturation.use() / 100f, 1f)
                    }
                }, valueArrow)
        val alphaPane = createPropertyPane(hsv.alpha, "A: ",
                createTransparencyNode().also { im ->
                    im += Gradient().also { grad ->
                        grad.leftColor.sideEffecting { c ->
                            c.set(currentColor.use())
                            c.a = 0f
                            c
                        }
                        grad.rightColor.sideEffecting { c ->
                            c.set(currentColor.use())
                            c.a = 1f
                            c
                        }
                    }
                }, alphaArrow)
        val detailPane = Pane().apply {
            this.bindHeightToParent(multiplier = rowHeightMultiplier)
            this += TextLabel("#", font).apply {
                Anchor.CentreLeft.configure(this)
                this.bounds.width.set(32f)
                this.renderAlign.set(Align.right)
                this.setScaleXY(scale)
                this.textColor.bind { this@ColourPicker.textColor.use() }
            }
            val parentCtr = Pane().apply {
                this.bounds.x.set(32f)
                this.bindWidthToParent(adjust = -32f)
                this.margin.set(Insets(2f, 2f, 0f, 0f))
            }
            this += parentCtr
            parentCtr += Pane().apply {
                Anchor.CentreLeft.configure(this)
                this.bindWidthToParent(multiplier = 0.75f)
                this.margin.set(Insets(2f, 2f, 4f, 4f))
                
                val charLimit = if (hasAlpha) 8 else 6
                
                fun Color.toStr(): String = this.toString().uppercase(Locale.ROOT).take(charLimit)

                val textField: TextField
                this += RectElement(Color(0f, 0f, 0f, 0.9f)).apply {
                    this.bindWidthToParent(adjustBinding = { (bounds.height.useF() + 4f) * 2f * -1 })
                    this.border.set(Insets(1f))
                    this.borderStyle.set(SolidBorder(Color.WHITE))
                    this.padding.set(Insets(1f))
                    textField = TextField(font = font).apply {
                        this.characterLimit.set(charLimit)
                        this.text.set(currentColor.getOrCompute().toStr())
                        this.inputFilter.set({ c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' })
                        this.textColor.bind { this@ColourPicker.textColor.use() }
                        currentColor.addListener { v ->
                            if (!hasFocus.get()) {
                                this.text.set(v.getOrCompute().toStr())
                            }
                        }
                        this.text.addListener { t ->
                            if (hasFocus.get()) {
                                try {
                                    val newValue = Color.valueOf(t.getOrCompute())
                                    this@ColourPicker.setColor(newValue, false)
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                        hasFocus.addListener { f ->
                            if (!f.getOrCompute()) { // on focus lost
//                                this.text.set(currentColor.getOrCompute().toStr())
                                try {
                                    val newValue = Color.valueOf(this.text.getOrCompute())
                                    this@ColourPicker.setColor(newValue, false)
                                } catch (ignored: Exception) {}
                            }
                        }
                        this.setOnRightClick { 
                            requestFocus()
                            text.set("")
                        }
                    }
                    this += textField
                }
                rgbTextField = textField
                this += Button("").apply {
                    Anchor.CentreRight.configure(this, offsetX = { -(bounds.width.useF() + 4f) })
                    this.bindWidthToSelfHeight()
                    this.padding.set(Insets(6f))
                    this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_copy")))
                    this.setOnAction {
                        Gdx.app.clipboard.contents = currentColor.getOrCompute().toStr()
                    }
                    this.tooltipElement.set(Tooltip(binding = { Localization.getVar("ui.colourPicker.copyHex").use() }, font = font))
                }
                this += Button("").apply { 
                    Anchor.CentreRight.configure(this)
                    this.bindWidthToSelfHeight()
                    this.padding.set(Insets(6f))
                    this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_paste")))
                    this.setOnAction {
                        textField.text.set("")
                        textField.requestFocus()
                        textField.attemptPaste()
                        textField.requestUnfocus()
                    }
                    this.tooltipElement.set(Tooltip(binding = { Localization.getVar("ui.colourPicker.pasteHex").use() }, font = font))
                }
            }
            parentCtr += Pane().apply {
                Anchor.CentreRight.configure(this)
                this.bindWidthToParent(multiplier = 0.25f)
                this.margin.set(Insets(2f, 2f, 4f, 4f))
                this += UIElement().apply {
                    Anchor.CentreRight.configure(this)
                    this.border.set(Insets(1f))
                    this.borderStyle.set(SolidBorder(Color.WHITE))
                    this += createTransparencyNode().apply {
                        this += RectElement(binding = { currentColor.use() })
                    }
                }
            }
        }
        
        vbox.temporarilyDisableLayouts { 
            vbox += huePane
            vbox += satPane
            vbox += valuePane
            if (hasAlpha) {
                vbox += alphaPane
            }
            vbox += detailPane
        }
        
        addChild(vbox)
    }
    
    fun setColor(newValue: Color, setRgbTextField: Boolean) {
        val h = newValue.toHsv(FloatArray(3))
        hsv.hue.set(h[0].roundToInt().coerceIn(0, 360))
        hsv.saturation.set((h[1] * 100).roundToInt().coerceIn(0, 100))
        hsv.value.set((h[2] * 100).roundToInt().coerceIn(0, 100))
        if (hasAlpha) {
            hsv.alpha.set((newValue.a * 255).roundToInt().coerceIn(0, 255))
        }
        if (setRgbTextField) {
            rgbTextField.text.set(newValue.toString().uppercase(Locale.ROOT).take(if (hasAlpha) 8 else 6))
        }
    }

    override fun getDefaultSkinID(): String = COLOUR_PICKER_SKIN_ID
    
    private class Gradient : UIElement() {
        val leftColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
        val rightColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
        
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.paddingZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor

            val opacity: Float = this.apparentOpacity.get()
            val tmpColor: Color = ColorStack.getAndPush()
            val tmpColor2: Color = ColorStack.getAndPush()
            tmpColor.set(leftColor.getOrCompute())
            tmpColor.a *= opacity
            tmpColor2.set(rightColor.getOrCompute())
            tmpColor2.a *= opacity
            batch.color = tmpColor
            batch.drawQuad(x, y - h, tmpColor, x + w, y - h, tmpColor2,
                    x + w, y, tmpColor2, x, y, tmpColor)

            ColorStack.pop()
            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }
    }
    
    private class MovingArrow(val minValue: Int, val maxValue: Int, valueVar: Var<Int>? = null)
        : UIElement(), HasTooltip by HasTooltip.DefaultImpl(), HasPressedState by HasPressedState.DefaultImpl() {
        
        val value: Var<Int> = valueVar ?: Var(minValue)
        
        init {
            HasPressedState.DefaultImpl.addDefaultPressedStateInputListener(this)
            addInputEventListener { event ->
                if (event is MouseInputEvent) {
                    if (event is TouchDragged || (event is ClickPressed && event.button == Input.Buttons.LEFT)) {
                        val lastMouseRelative = Vector2Stack.getAndPush()
                        val thisPos = this.getPosRelativeToRoot(lastMouseRelative)
                        lastMouseRelative.x = event.x - thisPos.x
                        lastMouseRelative.y = event.y - thisPos.y

                        val percentage = (lastMouseRelative.x / bounds.width.get()).coerceIn(0f, 1f)
                        value.set(MathUtils.lerp(minValue.toFloat(), maxValue.toFloat(), percentage).toInt().coerceIn(minValue, maxValue))

                        Vector2Stack.pop()

                        true
                    } else false
                } else if (event is Scrolled) {
                    value.set((value.getOrCompute() - event.amountY.sign.toInt()).coerceIn(minValue, maxValue))
                    true
                } else false
            }
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.paddingZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor

            val percentage = (value.getOrCompute() - minValue) / (maxValue - minValue).toFloat()
            val opacity: Float = this.apparentOpacity.get()
            val tmpColor: Color = ColorStack.getAndPush().set(1f, 1f, 1f, 1f)
            tmpColor.a *= opacity
            batch.color = tmpColor
            val tex = AssetRegistry.get<Texture>("ui_colour_picker_arrow")
            val size = h / 2
            batch.draw(tex, x - size / 2 + (w * percentage), y - h, size, size)

            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }
    }
}

class ColourPickerSkin(element: ColourPicker) : Skin<ColourPicker>(element) {
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
    }
}