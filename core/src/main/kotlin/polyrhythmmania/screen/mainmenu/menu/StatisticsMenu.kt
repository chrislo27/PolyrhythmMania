package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.ui.PRManiaSkins


class StatisticsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.statistics.title").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(300f)
            this.spacing.set(0f)
            this.margin.set(Insets(0f, 0f, 0f, 16f))
        }


        vbox.temporarilyDisableLayouts {
            val height = 32f
            val evenColor = Color().grey(0.35f)
            val oddColor = Color().grey(0.45f)
            GlobalStats.statMap.values.mapIndexed { index, stat -> 
                Pane().also { pane ->
                    pane.bounds.height.set(height)
                    val textColor = if (index % 2 == 0) evenColor else oddColor
                    pane += TextLabel(binding = { Localization.getVar(stat.getLocalizationID()).use() }, font = font).apply { 
                        Anchor.TopLeft.configure(this)
                        this.markup.set(this@StatisticsMenu.markup)
                        this.bindWidthToParent(adjust = -6f, multiplier = 0.75f)
                        this.textAlign.set(TextAlign.LEFT)
                        this.textColor.set(textColor)
                        this.setScaleXY(0.85f)
                        this.margin.set(Insets(0f, 0f, 4f, 0f))
                    }
                    pane += TextLabel(binding = { stat.formatter.format(stat.value).use() }, font = font).apply { 
                        Anchor.TopRight.configure(this)
                        this.markup.set(this@StatisticsMenu.markup)
                        this.bindWidthToParent(adjust = -6f, multiplier = 0.25f)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(textColor)
                    }
                }
            }.forEach { 
                vbox += it
            }
        }

        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }
    }

}