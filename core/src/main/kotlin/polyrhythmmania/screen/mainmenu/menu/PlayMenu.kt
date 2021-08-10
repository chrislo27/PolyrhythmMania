package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.discordrpc.DefaultPresences
import polyrhythmmania.discordrpc.DiscordHelper
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.screen.PlayScreen
import polyrhythmmania.sidemodes.DunkMode
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.ui.PRManiaSkins
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class PlayMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    private var epochSeconds: Long = System.currentTimeMillis() / 1000
    val dailyChallengeDate: Var<LocalDate> = Var(EndlessPolyrhythm.getCurrentDailyChallengeDate())

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

            vbox += createLongButton { Localization.getVar("mainMenu.play.endless").use() }.apply {
                this.setOnAction {
//                    menuCol.pushNextMenu(menuCol.practiceMenu) // TODO
                }
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.endless.tooltip")))
            }
            val dailyChallengeTitle: ReadOnlyVar<String> = Localization.getVar("mainMenu.play.endless.daily", Var {
                listOf(dailyChallengeDate.use().format(DateTimeFormatter.ISO_DATE))
            })
            vbox += createLongButton { dailyChallengeTitle.use() }.apply {
                this.setOnAction {
                    menuCol.playMenuSound("sfx_menu_enter_game")
                    mainMenu.transitionAway {
                        val main = mainMenu.main
                        Gdx.app.postRunnable {
                            val date = dailyChallengeDate.getOrCompute()
                            val scoreVar = Var(0)
                            scoreVar.addListener {
                                main.settings.endlessDailyChallenge.set(date to it.getOrCompute())
                            }
                            val sidemode: SideMode = EndlessPolyrhythm(main, EndlessModeScore(scoreVar), EndlessPolyrhythm.getSeedFromLocalDate(date), date)
                            val playScreen = PlayScreen(main, sidemode, sidemode.container, challenges = Challenges.NO_CHANGES, showResults = false)
                            main.settings.endlessDailyChallenge.set(date to 0)
                            main.settings.persist()
                            main.screen = TransitionScreen(main, main.screen, playScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                                this.onEntryEnd = {
                                    sidemode.prepare()
                                    playScreen.resetAndStartOver(false, false)
                                }
                            }
                        }
                    }
                }
                this.tooltipElement.set(createTooltip(binding = {
                    val (date, hiScore) = main.settings.endlessDailyChallenge.use()
                    if (date == dailyChallengeDate.use()) {
                        Localization.getVar("mainMenu.play.endless.daily.tooltip.expired", Var {
                            listOf(hiScore)
                        })
                    } else {
                        Localization.getVar("mainMenu.play.endless.daily.tooltip.ready")
                    }.use()
                }))
                this.disabled.bind {
                    val (date, _) = main.settings.endlessDailyChallenge.use()
                    date == dailyChallengeDate.use()
                }
            }

            // Remember to update DataSettingsMenu to reset high scores
            vbox += createSidemodeLongButton("mainMenu.play.dunk", Localization.getVar("mainMenu.play.dunk.tooltip",
                    Var { listOf(main.settings.endlessDunkHighScore.use()) })) { main, _ ->
                DiscordHelper.updatePresence(DefaultPresences.PlayingDunk)
                DunkMode(main, EndlessModeScore(main.settings.endlessDunkHighScore))
            }
//            vbox += createLongButton { Localization.getVar("mainMenu.play.toss").use() }.apply {
//                
//            }
//            vbox += createLongButton { Localization.getVar("mainMenu.play.dash").use() }.apply {
//                
//            }

//            vbox += createLongButton { "...Other modes (possibly) coming soon!" }.apply {
//                this.disabled.set(true)
//            }
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

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)

        val newEpochSeconds = System.currentTimeMillis() / 1000
        if (newEpochSeconds != epochSeconds) {
            val newLocalDate = EndlessPolyrhythm.getCurrentDailyChallengeDate()
            if (newLocalDate != dailyChallengeDate.getOrCompute()) {
                dailyChallengeDate.set(newLocalDate)
            }
        }
    }
}
