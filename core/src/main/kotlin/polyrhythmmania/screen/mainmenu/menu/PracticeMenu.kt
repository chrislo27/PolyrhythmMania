package polyrhythmmania.screen.mainmenu.menu

import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.practice.Polyrhythm1Practice
import polyrhythmmania.sidemodes.practice.Polyrhythm2Practice
import polyrhythmmania.sidemodes.practice.Practice
import polyrhythmmania.sidemodes.practice.PracticeBasic


class PracticeMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.practice.title").use() }
        this.contentPane.bounds.height.set(280f)

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

        vbox.temporarilyDisableLayouts {
            fun createPractice(name: String, factory: (PRManiaGame, InputKeymapKeyboard) -> Practice): UIElement {
                return createSidemodeLongButton(name, Localization.getVar("${name}.tooltip"), Challenges.NO_CHANGES, false, factory = factory)
            }
            
            vbox += createPractice("mainMenu.practice.basic") { main, keymap ->
                PracticeBasic(main, keymap)
            }
            vbox += createSidemodeLongButton("mainMenu.practice.polyrhythm1",
                    Localization.getVar("mainMenu.practice.polyrhythm1.tooltip"),
                    Challenges.NO_CHANGES, true) { main, _ ->
                Polyrhythm1Practice(main)
            }
            vbox += createSidemodeLongButton("mainMenu.practice.polyrhythm2",
                    Localization.getVar("mainMenu.practice.polyrhythm2.tooltip"),
                    Challenges.NO_CHANGES, true) { main, _ ->
                Polyrhythm2Practice(main)
            }
        }

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
