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
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import java.util.*
import kotlin.math.min


open class CheckBox(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<CheckBox>() {
    
    companion object {
        const val SKIN_ID: String = "CheckBox"
        
        init {
            DefaultSkins.register(SKIN_ID, SkinFactory { element: CheckBox ->
                CheckBoxSkin(element)
            })
        }
    }
    
    enum class CheckType {
        CHECKMARK, X,
    }
    
    val textLabel: TextLabel = TextLabel(text, font)
    val imageNode: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

    var checkType: Var<CheckType> = Var(CheckType.CHECKMARK)
    var checkedState: Var<Boolean> = Var(false)
    
    init {
        val height = Var.bind {
            contentZone.height.use()
        }
        textLabel.bounds.x.bind { height.use() }
        textLabel.bindWidthToParent { height.use() }
        imageNode.bounds.x.set(0f)
        imageNode.bounds.width.bind { height.use() }
        imageNode.textureRegion.bind { 
            val type = checkType.use()
            val state = checkedState.use()
            getTextureRegionForType(type, state)
        }
    }
    
    init {
        setOnAction { 
            checkedState.invert()
        }
    }

    constructor(binding: Var.Context.() -> String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : this("", font) {
        textLabel.text.bind(binding)
    }
    
    open fun getTextureRegionForType(type: CheckType, state: Boolean): TextureRegion {
        val spritesheet = PaintboxGame.paintboxSpritesheet
        return if (!state) spritesheet.checkboxEmpty else when (type) {
            CheckType.CHECKMARK -> spritesheet.checkboxCheck
            CheckType.X -> spritesheet.checkboxX
        }
    }

    override fun getDefaultSkinID(): String {
        return CheckBox.SKIN_ID
    }
}


open class CheckBoxSkin(element: CheckBox) : Skin<CheckBox>(element) {

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }
    
}