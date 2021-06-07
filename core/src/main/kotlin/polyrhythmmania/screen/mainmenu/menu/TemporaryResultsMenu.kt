package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.utils.Align
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.engine.input.InputResult


class TemporaryResultsMenu(menuCol: MenuCollection, val results: Results)
    : StandardMenu(menuCol) {

    data class Results(val expectedInputs: Int, val score: Int, val inputs: List<InputResult>)

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.set("Results:")
        this.contentPane.bounds.height.set(275f)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(vbox)
        contentPane.addChild(hbox)


        val resultsMarkup: Markup = Markup(mapOf(
                "prmania_icons" to main.fontIcons,
                "rodin" to main.fontMainMenuRodin,
                "bold" to font
        ), TextRun(main.fontMainMenuThin, ""), Markup.FontStyles("bold", "italic", "bolditalic"))
        
        val resultsStr = "[b]Inputs:[] ${results.inputs.size} / ${results.expectedInputs}\n" +
                "[b]Score:[] ${results.score}\n" +
                "\n(This is a temporary results screen, a proper one is in the works!)"

        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(resultsStr).apply {
                this.markup.set(resultsMarkup)
                this.bounds.height.set(80f)
                this.padding.set(Insets(4f))
                this.textColor.set(UppermostMenu.ButtonSkin.TEXT_COLOR)
                this.renderAlign.set(Align.topLeft)
                this.textAlign.set(TextAlign.LEFT)
                this.doLineWrapping.set(true)
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { "Back to Main Menu" }).apply {
                this.bounds.width.set(300f)
                this.setOnAction {
                    menuCol.popLastMenu()
                    menuCol.removeMenu(this@TemporaryResultsMenu)
                }
            }
        }
    }
}