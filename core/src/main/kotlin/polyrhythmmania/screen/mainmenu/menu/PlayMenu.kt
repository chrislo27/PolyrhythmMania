package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.eclipsesource.json.Json
import paintbox.Paintbox
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
import paintbox.util.Version
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.discordrpc.DefaultPresences
import polyrhythmmania.discordrpc.DiscordHelper
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.screen.PlayScreen
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.sidemodes.DunkMode
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeScore
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeUtils
import polyrhythmmania.ui.PRManiaSkins
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread


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
                    menuCol.pushNextMenu(menuCol.endlessMenu)
                }
                val highScoreSubstitutionVar = Localization.getVar("mainMenu.play.endless.tooltip.highScore", Var {
                    val endlessScore = main.settings.endlessHighScore.use()
                    listOf(endlessScore.score, EndlessPolyrhythm.getSeedString(endlessScore.seed))
                })
                val noHighScoreSubstitutionVar = Localization.getVar("mainMenu.play.endless.tooltip.highScore.none")
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.endless.tooltip", Var {
                    val endlessScore = main.settings.endlessHighScore.use()
                    if (endlessScore.score > 0) {
                        listOf(highScoreSubstitutionVar.use())
                    } else {
                        listOf(noHighScoreSubstitutionVar.use())
                    }
                })))
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
                                main.settings.endlessDailyChallenge.set(DailyChallengeScore(date, it.getOrCompute()))
                            }
                            val sidemode: EndlessPolyrhythm = EndlessPolyrhythm(main,
                                    EndlessModeScore(scoreVar, showHighScore = false),
                                    EndlessPolyrhythm.getSeedFromLocalDate(date), date, disableLifeRegen = false)
                            val playScreen = PlayScreen(main, sidemode, sidemode.container, challenges = Challenges.NO_CHANGES, showResults = false)
                            main.settings.endlessDailyChallenge.set(DailyChallengeScore(date, 0))
                            main.settings.persist()
                            main.screen = TransitionScreen(main, main.screen, playScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                                this.onEntryEnd = {
                                    sidemode.prepare()
                                    playScreen.resetAndStartOver(false, false)
                                    DiscordHelper.updatePresence(DefaultPresences.PlayingDailyChallenge(date))
                                    mainMenu.backgroundType = BgType.ENDLESS
                                }
                            }
                            
                            // Get UUID nonce from high score server
                            DailyChallengeUtils.sendNonceRequest(date, sidemode.dailyChallengeUUIDNonce)
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
            vbox += createLongButton { Localization.getVar("mainMenu.play.sideModes").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.sideModesMenu)
                }
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
