package paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.PaintboxGame
import paintbox.binding.*
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.ui.area.Insets
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory


open class CheckBox(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<CheckBox>(), Toggle {
    
    companion object {
        const val CHECKBOX_SKIN_ID: String = "CheckBox"

        private val DEFAULT_ACTION: () -> Unit = { }
        private val DEFAULT_CHECK_ACTION: (Boolean) -> Unit = { }
        
        init {
            DefaultSkins.register(CHECKBOX_SKIN_ID, SkinFactory { element: CheckBox ->
                CheckBoxSkin(element)
            })
        }
    }
    
    enum class CheckType {
        CHECKMARK, X,
    }
    
    enum class BoxAlign {
        LEFT, RIGHT;
    }
    
    val color: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val textLabel: TextLabel = TextLabel(text, font)
    val imageNode: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

    val checkType: Var<CheckType> = Var(CheckType.CHECKMARK)
    val checkedState: BooleanVar = BooleanVar(false)
    val boxAlignment: Var<BoxAlign> = Var(BoxAlign.LEFT)
    override val selectedState: BooleanVar = checkedState
    override val toggleGroup: Var<ToggleGroup?> = Var(null)

    var onSelected: () -> Unit = DEFAULT_ACTION
    var onUnselected: () -> Unit = DEFAULT_ACTION
    var onCheckChanged: (newState: Boolean) -> Unit = DEFAULT_CHECK_ACTION
    
    init {
        val height: ReadOnlyFloatVar = FloatVar {
            contentZone.height.use()
        }
        textLabel.bounds.x.bind { if (boxAlignment.use() == BoxAlign.LEFT) height.use() else 0f }
        textLabel.bindWidthToParent { -height.use() }
        textLabel.margin.set(Insets(2f))
        imageNode.bounds.x.bind { if (boxAlignment.use() == BoxAlign.LEFT) 0f else ((imageNode.parent.use()?.bounds?.width?.use()
                ?: 0f) - height.use()) }
        imageNode.bounds.width.bind { height.use() }
        imageNode.textureRegion.bind { 
            val type = checkType.use()
            val state = checkedState.use()
            getTextureRegionForType(type, state)
        }
        imageNode.margin.set(Insets(2f))
        
        textLabel.renderAlign.bind { if (boxAlignment.use() == BoxAlign.LEFT) com.badlogic.gdx.utils.Align.left else com.badlogic.gdx.utils.Align.right }
        textLabel.textAlign.bind { if (boxAlignment.use() == BoxAlign.LEFT) TextAlign.LEFT else TextAlign.RIGHT }
        
        textLabel.textColor.bind { color.use() }
        imageNode.tint.bind { color.use() }
        
        this.addChild(textLabel)
        this.addChild(imageNode)
    }
    
    init {
        setOnAction { 
            checkedState.invert()
        }
        checkedState.addListener {
            if (it.getOrCompute()) {
                onSelected.invoke()
                onCheckChanged.invoke(true)
            } else {
                onUnselected.invoke()
                onCheckChanged.invoke(false)
            }
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
        return CheckBox.CHECKBOX_SKIN_ID
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