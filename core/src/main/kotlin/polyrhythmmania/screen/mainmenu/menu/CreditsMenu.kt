package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.ReadOnlyVar
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.credits.Credits
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.max


class CreditsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    companion object {
        private const val ROW_HEIGHT: Float = 36f
        val HEADING_TEXT_COLOR: Color = Color.valueOf("564F2BFF")
        val NAME_TEXT_COLOR: Color = Color.valueOf("323232FF")
    }
    
    private val scrollPane: ScrollPane

    init {
        this.setSize(0.64f)
        this.titleText.bind { Localization.getVar("mainMenu.credits.title").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)
        this.deleteWhenPopped.set(true)

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
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.credits.goToHomepage").use() }).apply {
                this.bounds.width.set(200f)
                val link = PRMania.GITHUB
                this.tooltipElement.set(createTooltip { Localization.getValue("mainMenu.credits.openToBrowser.tooltip", link) })
                this.setOnAction {
                    Gdx.net.openURI(link)
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.credits.goToLicense").use() }).apply {
                this.bounds.width.set(120f)
                val link = """https://github.com/chrislo27/PolyrhythmMania/blob/master/LICENSE"""
                this.tooltipElement.set(createTooltip { Localization.getValue("mainMenu.credits.openToBrowser.tooltip", link) })
                this.setOnAction {
                    Gdx.net.openURI(link)
                }
            }
        }

        scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
            
            this.vBar.unitIncrement.set(10f)
            this.vBar.blockIncrement.set(40f)
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
            vbox += TextLabel(Localization.getValue("credits.licenseInfo", PRMania.GITHUB), font = font).apply {
                Anchor.TopLeft.configure(this)
                this.bindWidthToParent(adjust = 0f, multiplier = 1f)
                this.bounds.x.set(0f)
                this.bounds.height.set(128f)
                this.padding.set(Insets(12f, 8f, 6f, 6f))
                this.markup.set(this@CreditsMenu.markup)
                this.textColor.set(Color.DARK_GRAY)
                this.renderAlign.set(Align.topLeft)
                this.textAlign.set(TextAlign.LEFT)
                this.doLineWrapping.set(true)
                this.setScaleXY(0.75f)
            }
            vbox += TextLabel(binding = { Localization.getVar("credits.thankYouForPlaying").use() }, font = main.fontMainMenuHeading).apply {
                this.bounds.height.set(64f)
                this.padding.set(Insets(6f, 24f, 6f, 6f))
                this.textColor.set(NAME_TEXT_COLOR)
                this.renderAlign.set(Align.center)
                this.setScaleXY(0.75f)
            }
            vbox += TextLabel(PRMania.HOMEPAGE, font = font).apply {
                this.bounds.height.set(32f)
                this.padding.set(Insets(6f, 6f, 6f, 6f))
                this.markup.set(this@CreditsMenu.markup)
                this.textColor.set(HEADING_TEXT_COLOR)
                this.renderAlign.set(Align.center)
            }
            vbox += Pane().apply { 
                this.bounds.height.set(32f)
            }
        }
        vbox.sizeHeightToChildren(100f)

        scrollPane.setContent(vbox)
    }
    
    fun resetScroll() {
        scrollPane.vBar.setValue(0f)
        scrollPane.hBar.setValue(0f)
    }

    private fun createCreditRow(headingLoc: ReadOnlyVar<String>, names: List<ReadOnlyVar<String>>): Pane {
        fun getRowHeight(name: String): Float = name.count { it == '\n' } * 0.75f + 1
        fun getHeadingRowHeight(name: String): Float = name.count { it == '\n' } * 0.75f + 1f

        return Pane().apply {
            val numNameColumns = 2

            val headingLabel = TextLabel(headingLoc.getOrCompute(), font = font).apply {
                Anchor.TopLeft.configure(this)
                this.bounds.width.set(210f)
                this.bounds.height.bind { ROW_HEIGHT * getHeadingRowHeight(text.use()) }
                this.padding.set(Insets(12f, 2f, 0f, 8f))
                this.textColor.set(HEADING_TEXT_COLOR)
                this.renderAlign.set(Align.topRight)
                this.textAlign.set(TextAlign.RIGHT)
            }
            addChild(headingLabel)
            var currentRowY = 0f
            var currentRowHeight = 0f
            addChild(Pane().apply {
                Anchor.TopRight.configure(this)
                this.bindWidthToParent(adjustBinding = { -headingLabel.bounds.width.use() })

                var currentRow = -1
                names.forEachIndexed { index, str ->
                    val row = index / numNameColumns
                    val col = index % numNameColumns

                    if (row != currentRow) {
                        currentRow = row
                        currentRowY += currentRowHeight
                        currentRowHeight = 0f
                    }

                    val rh = getRowHeight(str.getOrCompute())
                    addChild(TextLabel(str.getOrCompute(), font = font).apply {
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(adjust = 0f, multiplier = 1f / numNameColumns)
                        this.bounds.x.bind { bounds.width.use() * col }
                        this.bounds.height.set(rh * ROW_HEIGHT)
                        this.bounds.y.set(currentRowY)
                        this.padding.set(Insets(12f, 2f, 6f, 2f))
                        this.markup.set(this@CreditsMenu.markup)
                        this.textColor.set(NAME_TEXT_COLOR)
                        this.renderAlign.set(Align.topLeft)
                        this.textAlign.set(TextAlign.LEFT)
                    })
                    currentRowHeight = max(currentRowHeight, rh * ROW_HEIGHT)
                }
            })

            val bottomMargin = ROW_HEIGHT * 0.25f
            this.margin.set(Insets(0f, bottomMargin, 0f, 0f))
            this.bounds.height.bind {
                max((headingLabel.bounds.height.use()), (currentRowY + currentRowHeight)) + bottomMargin
            }
        }
    }

}