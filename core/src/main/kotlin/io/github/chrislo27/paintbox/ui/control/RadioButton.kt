package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.binding.invert
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.ImageRenderingMode
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import java.util.*
import kotlin.math.min


open class RadioButton(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<RadioButton>(), Toggle {

    companion object {
        const val SKIN_ID: String = "RadioButton"

        init {
            DefaultSkins.register(SKIN_ID, SkinFactory { element: RadioButton ->
                RadioButtonSkin(element)
            })
        }
    }

    val textLabel: TextLabel = TextLabel(text, font)
    val imageNode: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

    /**
     * If true, the radio button will act like a toggle and can be unselected. If false, it will only be able to be checked.
     */
    val actAsToggle: Var<Boolean> = Var(false)
    val checkedState: Var<Boolean> = Var(false)
    override val selectedState: Var<Boolean> = checkedState
    override val toggleGroup: Var<ToggleGroup?> = Var(null)

    init {
        val height = Var.bind {
            contentZone.height.use()
        }
        textLabel.bounds.x.bind { height.use() }
        textLabel.bindWidthToParent { -height.use() }
        textLabel.margin.set(Insets(2f))
        imageNode.bounds.x.set(0f)
        imageNode.bounds.width.bind { height.use() }
        imageNode.textureRegion.bind {
            val state = checkedState.use()
            getTextureRegionForType(state)
        }
        imageNode.tint.set(Color(0f, 0f, 0f, 1f))
        imageNode.margin.set(Insets(2f))

        this.addChild(textLabel)
        this.addChild(imageNode)
    }

    init {
        setOnAction {
            if (actAsToggle.getOrCompute()) {
                checkedState.invert()
            } else {
                checkedState.set(true)
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
        return RadioButton.SKIN_ID
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