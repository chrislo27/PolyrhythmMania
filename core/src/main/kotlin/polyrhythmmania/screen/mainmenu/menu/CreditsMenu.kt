package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.ScrollBar
import io.github.chrislo27.paintbox.ui.control.ScrollPane
import io.github.chrislo27.paintbox.ui.control.ScrollPaneSkin
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.layout.HBox
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRoundedRect
import io.github.chrislo27.paintbox.util.sumByFloat
import polyrhythmmania.Localization
import polyrhythmmania.credits.Credits
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.ceil


class CreditsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    companion object {
        private const val ROW_HEIGHT: Float = 40f
        private val HEADING_TEXT_COLOR: Color = Color.valueOf("564F2BFF")
        private val NAME_TEXT_COLOR: Color = Color.valueOf("323232FF")
    }

    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.credits.title").use() }
        this.contentPane.bounds.height.set(300f)

        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(hbox)
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }

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
        contentPane.addChild(scrollPane)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(300f)
            this.spacing.set(0f)
        }

        vbox.temporarilyDisableLayouts {
            Credits.credits.forEach { (header, names) ->
                vbox += createCreditRow(header, names)
            }
            vbox += TextLabel(binding = {Localization.getVar("credits.licenseInfo").use()}, font = font).apply {
                Anchor.TopLeft.configure(this)
                this.bindWidthToParent(adjust = 0f, multiplier = 1f)
                this.bounds.x.set(0f)
                this.bounds.height.set(ROW_HEIGHT * 2)
                this.padding.set(Insets(8f, 8f, 6f, 6f))
                this.markup.set(this@CreditsMenu.markup)
                this.textColor.set(NAME_TEXT_COLOR)
                this.renderAlign.set(Align.topLeft)
                this.textAlign.set(TextAlign.LEFT)
                this.doLineWrapping.set(true)
                val scale = 0.75f
                val textBlock = Var {
                    val label = this@apply
                    TextRun(label.font.use(), label.text.use(), Color.WHITE,
                            scale, scale).toTextBlock().also { textBlock ->
                        if (label.doLineWrapping.use()) {
                            textBlock.lineWrapping = label.contentZone.width.use()
                        }
                    }
                }
                this.internalTextBlock.bind { textBlock.use() }
            }

            vbox.bounds.height.set(vbox.children.sumByFloat { it.bounds.height.getOrCompute() })
        }

        scrollPane.setContent(vbox)
    }

    private fun createCreditRow(headingLoc: String, names: List<String>): Pane {
        return Pane().apply {
            val nameColumns = 2
            val rowCount = ceil(names.size / nameColumns.toFloat()).toInt()
            this.bounds.height.set(rowCount * ROW_HEIGHT)

            val headingLabel = TextLabel(binding = { Localization.getVar(headingLoc).use() }, font = font).apply {
                Anchor.TopLeft.configure(this)
                this.bounds.width.set(220f)
                this.bounds.height.set(ROW_HEIGHT)
                this.padding.set(Insets(4f, 4f, 0f, 8f))
                this.textColor.set(HEADING_TEXT_COLOR)
                this.renderAlign.set(Align.right)
                this.textAlign.set(TextAlign.RIGHT)
            }
            addChild(headingLabel)
            addChild(Pane().apply {
                Anchor.TopRight.configure(this)
                this.bindWidthToParent(adjustBinding = { -headingLabel.bounds.width.use() })
                names.forEachIndexed { index, str ->
                    val row = index / nameColumns
                    val col = index % nameColumns

                    addChild(TextLabel(str, font = font).apply {
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(adjust = 0f, multiplier = 1f / nameColumns)
                        this.bounds.x.bind { bounds.width.use() * col }
                        this.bounds.height.set(ROW_HEIGHT)
                        this.bounds.y.set(ROW_HEIGHT * row)
                        this.padding.set(Insets(4f, 4f, 6f, 0f))
                        this.markup.set(this@CreditsMenu.markup)
                        this.textColor.set(NAME_TEXT_COLOR)
                        this.renderAlign.set(Align.left)
                        this.textAlign.set(TextAlign.LEFT)
                    })
                }
            })
        }
    }
    
}