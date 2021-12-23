package polyrhythmmania.achievements.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaGame
import polyrhythmmania.ui.TextboxPane


class Toast : UIElement() {
    
    val imageIcon: ImageIcon = ImageIcon(null, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
    val titleLabel: TextLabel
    val nameLabel: TextLabel
    
    init {
        this.bounds.height.set(80f)
        this.bindWidthToSelfHeight(multiplier = 4.5f)
        
        val outermostBorderColor = Color(0f, 0f, 0f, 1f)
        val middleBorderColor = Color().grey(85f / 255f)
        val innermostColor = Color().grey(33f / 255f)
        
        val innermostRect = RectElement(innermostColor).also { rect ->
            rect.padding.set(Insets(8f))
            rect.border.set(Insets(3f))
            rect.borderStyle.set(SolidBorder(middleBorderColor))
        }
        
        // Outermost border
        this += RectElement(outermostBorderColor).apply { 
            this.border.set(Insets(6f))
            this.borderStyle.set(SolidBorder().also { border ->
                border.color.bind { this@apply.color.use() }
                border.roundedCorners.set(true)
            })
            
            // Middle border
            this += RectElement(innermostColor).also { rect ->
                rect.border.set(Insets(3f))
                rect.borderStyle.set(SolidBorder(middleBorderColor).apply {
                    this.roundedCorners.set(true)
                })
                rect += innermostRect
            }
        }
        
        innermostRect += imageIcon.apply {
            Anchor.TopLeft.configure(this)    
            this.bindWidthToSelfHeight()
        }
        val main = PRManiaGame.instance
        titleLabel = TextLabel("Test Title Label", font = main.fontMainMenuMain).apply { 
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(multiplier = 0.5f, adjust = -2f)
            this.setScaleXY(0.8f)
            this.renderAlign.set(Align.left)
            this.padding.set(Insets.ZERO)
            this.textColor.set(Color.GOLD)
        }
        nameLabel = TextLabel("Test Name Label", font = main.fontMainMenuMain).apply { 
            Anchor.BottomLeft.configure(this)
            this.bindHeightToParent(multiplier = 0.5f, adjust = -2f)
            this.setScaleXY(0.8f)
            this.renderAlign.set(Align.left)
            this.padding.set(Insets.ZERO)
            this.textColor.set(Color.LIGHT_GRAY)
        }
        
        val pane = Pane().apply {
            Anchor.TopRight.configure(this)    
            this.bindWidthToParent(adjustBinding = {
                -((parent.use()?.contentZone?.height?.use() ?: 0f) + 8f)
            })
            this += titleLabel
            this += nameLabel
        }
        innermostRect += pane
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
        1 + 1
    }
}