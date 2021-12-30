package polyrhythmmania.solitaire

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.binding.ReadOnlyVar
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.set
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.screen.mainmenu.menu.MMMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType


class SolitaireHelpMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(MMMenu.WIDTH_LARGE, adjust = 16f)
        this.titleText.bind { Localization.getVar("solitaire.instructions").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)

        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(hbox)

        val pane = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)
//            this.padding.set(Insets(24f))
        }
        contentPane.addChild(pane)
        
        val grey = Color().grey(0.35f)
        pane += RectElement(grey).apply {
            Anchor.Centre.configure(this)
            this.bounds.height.set(2f)
        }
        pane += RectElement(grey).apply {
            Anchor.Centre.configure(this)
            this.bounds.width.set(2f)
        }
        
        fun createCornerPane(text: ReadOnlyVar<String>): Pane {
            return Pane().apply { 
                this.bindWidthToParent(multiplier = 0.5f, adjust = -8f)
                this.bindHeightToParent(multiplier = 0.5f, adjust = -6f)

                this += TextLabel(binding = { text.use() }).apply {
                    Anchor.BottomLeft.configure(this)
                    this.bindHeightToParent(multiplier = 0.35f)
                    this.doLineWrapping.set(true)
                    this.markup.set(this@SolitaireHelpMenu.markup)
                    this.textColor.set(LongButtonSkin.TEXT_COLOR)
                    this.setScaleXY(0.75f)
                    this.renderAlign.set(Align.center)
                    this.padding.set(Insets(2f, 2f, 4f, 4f))
                }
            }
        }
        
        pane += createCornerPane(Localization.getVar("solitaire.instructions.1")).apply { 
            Anchor.TopLeft.configure(this)
        }
        pane += createCornerPane(Localization.getVar("solitaire.instructions.2")).apply { 
            Anchor.TopRight.configure(this)
        }
        pane += createCornerPane(Localization.getVar("solitaire.instructions.3")).apply { 
            Anchor.BottomLeft.configure(this)
        }
        pane += createCornerPane(Localization.getVar("solitaire.instructions.4")).apply { 
            Anchor.BottomRight.configure(this)
        }
                
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu(instant = true, playSound = true)
                }
            }
        }
    }
    

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)
        
        GlobalStats.updateModePlayTime(PlayTimeType.SOLITAIRE)
    }
}