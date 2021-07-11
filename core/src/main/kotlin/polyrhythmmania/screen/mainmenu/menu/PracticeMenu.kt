package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.Tooltip
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.sidemodes.practice.Practice
import polyrhythmmania.sidemodes.practice.PracticeBasic
import polyrhythmmania.screen.PlayScreen


class PracticeMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.practice.title").use() }
        this.contentPane.bounds.height.set(250f)

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
                return createLongButton { Localization.getVar("mainMenu.practice.basic").use() }.apply {
                    this.tooltipElement.set(Tooltip(binding = { Localization.getVar("${name}.tooltip").use() }, font = this@PracticeMenu.font))
                    this.setOnAction {
                        menuCol.playMenuSound("sfx_menu_enter_game")
                        mainMenu.transitionAway {
                            val main = mainMenu.main
                            Gdx.app.postRunnable {
                                val practice: Practice = factory.invoke(main, main.settings.inputKeymapKeyboard.getOrCompute().copy())
                                val playScreen = PlayScreen(main, practice.container, Challenges.NO_CHANGES, showResults = false)
                                main.screen = TransitionScreen(main, main.screen, playScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                                    this.onEntryEnd = {
                                        practice.prepare()
//                                    playScreen.prepareGameStart()
                                        playScreen.resetAndStartOver(false, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            vbox += createPractice("mainMenu.practice.basic") { main, keymap ->
                PracticeBasic(main, keymap)
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
