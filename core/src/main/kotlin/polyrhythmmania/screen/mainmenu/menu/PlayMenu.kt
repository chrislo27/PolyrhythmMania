package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.Tooltip
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.screen.PlayScreen
import polyrhythmmania.sidemodes.DunkMode
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.ui.PRManiaSkins


class PlayMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.play.title").use() }
        this.contentPane.bounds.height.set(300f)

        val scrollPane = ScrollPane().apply {
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
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }


        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)


        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }

        vbox.temporarilyDisableLayouts {
            vbox += createLongButton { Localization.getVar("mainMenu.play.playSavedLevel").use() }.apply {
                this.setOnAction {
                    val loadMenu = LoadSavedLevelMenu(menuCol)
                    menuCol.addMenu(loadMenu)
                    menuCol.pushNextMenu(loadMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.play.practice").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.practiceMenu)
                }
            }

            fun createSidemode(name: String, tooltipVar: ReadOnlyVar<String> = Localization.getVar("${name}.tooltip"),
                               factory: (PRManiaGame, InputKeymapKeyboard) -> SideMode): UIElement {
                return createLongButton { Localization.getVar(name).use() }.apply {
                    this.tooltipElement.set(Tooltip(binding = { tooltipVar.use() }, font = this@PlayMenu.font))
                    this.setOnAction {
                        menuCol.playMenuSound("sfx_menu_enter_game")
                        mainMenu.transitionAway {
                            val main = mainMenu.main
                            Gdx.app.postRunnable {
                                val practice: SideMode = factory.invoke(main, main.settings.inputKeymapKeyboard.getOrCompute().copy())
                                val playScreen = PlayScreen(main, practice.container, Challenges.NO_CHANGES, showResults = false)
                                main.screen = TransitionScreen(main, main.screen, playScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                                    this.onEntryEnd = {
                                        practice.prepare()
                                        playScreen.resetAndStartOver(false, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Remember to update DataSettingsMenu to reset high scores
            vbox += createSidemode("mainMenu.play.dunk", Localization.getVar("mainMenu.play.dunk.tooltip", Var { listOf(main.settings.endlessDunkHighScore.use()) })) { main, _ ->
                DunkMode(main, EndlessModeScore(main.settings.endlessDunkHighScore))
            }
//            vbox += createLongButton { Localization.getVar("mainMenu.play.toss").use() }.apply {
//                
//            }
//            vbox += createLongButton { Localization.getVar("mainMenu.play.dash").use() }.apply {
//                
//            }
            vbox += createLongButton { "...Other modes (possibly) coming soon!" }.apply {
                this.disabled.set(true)
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
