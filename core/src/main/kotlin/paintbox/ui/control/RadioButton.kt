package paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.PaintboxGame
import paintbox.binding.*
import paintbox.font.PaintboxFont
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.ui.area.Insets
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory


open class RadioButton(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<RadioButton>(), Toggle {

    companion object {
        const val RADIO_BUTTON_SKIN_ID: String = "RadioButton"

        private val DEFAULT_ACTION: () -> Unit = { }

        init {
            DefaultSkins.register(RADIO_BUTTON_SKIN_ID, SkinFactory { element: RadioButton ->
                RadioButtonSkin(element)
            })
        }
    }

    val color: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val textLabel: TextLabel = TextLabel(text, font)
    val imageNode: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

    /**
     * If true, the radio button will act like a toggle and can be unselected. If false, it will only be able to be checked.
     */
    val actAsToggle: BooleanVar = BooleanVar(false)
    val checkedState: BooleanVar = BooleanVar(false)
    override val selectedState: BooleanVar = checkedState
    override val toggleGroup: Var<ToggleGroup?> = Var(null)

    var onSelected: () -> Unit = DEFAULT_ACTION
    var onUnselected: () -> Unit = DEFAULT_ACTION

    init {
        val height: ReadOnlyFloatVar = FloatVar {
            contentZone.height.useF()
        }
        textLabel.bounds.x.bind { height.useF() }
        textLabel.bindWidthToParent { -height.useF() }
        textLabel.margin.set(Insets(2f))
        imageNode.bounds.x.set(0f)
        imageNode.bounds.width.bind { height.useF() }
        imageNode.textureRegion.bind {
            val state = checkedState.useB()
            getTextureRegionForType(state)
        }
        imageNode.margin.set(Insets(2f))

        textLabel.textColor.bind { color.use() }
        imageNode.tint.bind { color.use() }

        this.addChild(textLabel)
        this.addChild(imageNode)
    }

    init {
        setOnAction {
            if (actAsToggle.get()) {
                checkedState.invert()
            } else {
                checkedState.set(true)
            }
        }
        checkedState.addListener {
            if (it.getOrCompute()) {
                onSelected.invoke()
            } else {
                onUnselected.invoke()
            }
        }
    }

    constructor(binding: Var.Context.() -> String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : this("", font) {
        textLabel.text.bind(binding)
    }

    open fun getTextureRegionForType(state: Boolean): TextureRegion {
        val spritesheet = PaintboxGame.paintboxSpritesheet
        return if (!state) spritesheet.radioButtonEmpty else spritesheet.radioButtonFilled
    }

    override fun getDefaultSkinID(): String {
        return RadioButton.RADIO_BUTTON_SKIN_ID
    }
}


open class RadioButtonSkin(element: RadioButton) : Skin<RadioButton>(element) {

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

}