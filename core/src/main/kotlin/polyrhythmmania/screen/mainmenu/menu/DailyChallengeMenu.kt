package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.discordrpc.DefaultPresences
import polyrhythmmania.discordrpc.DiscordHelper
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.screen.PlayScreen
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.sidemodes.endlessmode.*
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.util.flags.CountryFlags
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class DailyChallengeMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private var epochSeconds: Long = System.currentTimeMillis() / 1000
    val dailyChallengeDate: Var<LocalDate> = Var(EndlessPolyrhythm.getCurrentDailyChallengeDate())
    
    private var firstShow: Boolean = true
    private val isFetching: Var<Boolean> = Var(false)
    private val leaderboardList: Var<List<DailyLeaderboardScore>?> = Var(null)
    private val scrollPaneContent: Var<Pane> = Var(Pane())

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.dailyChallenge.title").use() }
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
            val dailyChallengeTitle: ReadOnlyVar<String> = Localization.getVar("mainMenu.dailyChallenge.play", Var {
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
                        Localization.getVar("mainMenu.dailyChallenge.play.tooltip.expired", Var {
                            listOf(hiScore)
                        })
                    } else {
                        Localization.getVar("mainMenu.dailyChallenge.play.tooltip.ready")
                    }.use()
                }))
                this.disabled.bind {
                    val (date, _) = main.settings.endlessDailyChallenge.use()
                    date == dailyChallengeDate.use()
                }
            }
            
            
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 0f, 0f))
                }
            }
            
            vbox += separator()
            
            val leaderboardScrollPane = ScrollPane().apply {
                Anchor.TopLeft.configure(this)
                this.bounds.height.set(390f)

                (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

                this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

                val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
                this.vBar.skinID.set(scrollBarSkinID)
                this.hBar.skinID.set(scrollBarSkinID)

                this.vBar.unitIncrement.set(10f)
                this.vBar.blockIncrement.set(40f)
            }
            
            val paneFetching = Pane().apply {
                this.bounds.height.set(200f)
                this += TextLabel(binding = {Localization.getVar("mainMenu.dailyChallenge.leaderboard.fetching").use()}).apply { 
                    this.renderAlign.set(Align.center)
                    this.doLineWrapping.set(true)
                    this.markup.set(this@DailyChallengeMenu.markup)
                }
            }
            val paneNoData = Pane().apply {
                this.bounds.height.set(200f)
                this += TextLabel(binding = {Localization.getVar("mainMenu.dailyChallenge.leaderboard.noData").use()}).apply { 
                    this.renderAlign.set(Align.center)
                    this.doLineWrapping.set(true)
                    this.markup.set(this@DailyChallengeMenu.markup)
                }
            }
            scrollPaneContent.bind { 
                if (isFetching.use()) {
                    paneFetching
                } else {
                    val list = leaderboardList.use()
                    if (list == null || list.isEmpty()) {
                        paneNoData
                    } else {
                        createTable(list)
                    }
                }
            }
            
            scrollPaneContent.addListener {
                leaderboardScrollPane.setContent(it.getOrCompute())
            }
            
            vbox += leaderboardScrollPane
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
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.dailyChallenge.leaderboard.refreshLeaderboard").use() }).apply {
                this.bounds.width.set(250f)
                this.setOnAction {
                    DailyChallengeUtils.getLeaderboard(dailyChallengeDate.getOrCompute(), leaderboardList, isFetching)
                }
                this.disabled.bind { 
                    isFetching.use()
                }
            }
        }
    }
    
    fun prepareShow(): DailyChallengeMenu {
        if (firstShow) {
            firstShow = false
            DailyChallengeUtils.getLeaderboard(dailyChallengeDate.getOrCompute(), leaderboardList, isFetching)
        }
        
        return this
    }
    
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)

        val newEpochSeconds = System.currentTimeMillis() / 1000
        if (newEpochSeconds != epochSeconds) {
            epochSeconds = newEpochSeconds
            val newLocalDate = EndlessPolyrhythm.getCurrentDailyChallengeDate()
            if (newLocalDate != dailyChallengeDate.getOrCompute()) {
                dailyChallengeDate.set(newLocalDate)
            }
        }
        scrollPaneContent.getOrCompute() // Forces refresh.
    }
    
    private fun createTable(list: List<DailyLeaderboardScore>): Pane {
        return VBox().apply {
            fun DailyLeaderboardScore.createPane(place: Int): Pane {
                return Pane().apply {
                    this.bounds.height.set(32f)
                    this += HBox().apply {
                        this.bindWidthToParent(adjust = -100f)
                        this += TextLabel("${place}", font = main.fontMainMenuMain).apply {
                            this.renderAlign.set(Align.right)
                            this.padding.set(Insets(0f, 0f, 0f, 4f))
                            this.bounds.width.set(52f)
                        }
                        val flag = CountryFlags.getFlagByCountryCode(this@createPane.countryCode)
                        this += ImageNode(CountryFlags.getTextureRegionForFlag(flag,
                                AssetRegistry.get<Texture>("country_flags"))).apply {
                            this.bindWidthToSelfHeight()
                        }
                        this += TextLabel((this@createPane.name.takeUnless { it.isBlank() } ?: "..."), font = main.fontMainMenuThin).apply {
                            this.renderAlign.set(Align.left)
                            this.padding.set(Insets(0f, 0f, 4f, 4f))
                            this.bounds.width.set(250f)
                        }
                    }

                    this += TextLabel("${this@createPane.score}", font = main.fontMainMenuMain).apply {
                        Anchor.TopRight.configure(this)
                        this.renderAlign.set(Align.center)
                        this.bounds.width.set(40f)
                    }
                }
            }
            
            this.temporarilyDisableLayouts { 
                var placeNumber = 1
                var placeValue = -1
                val sorted = list.sortedByDescending { it.score }
                sorted.forEachIndexed { i, score ->
                    if (score.score != placeValue) {
                        placeValue = score.score
                        placeNumber = i + 1
                    }
                    this += score.createPane(placeNumber)
                }
            }
        }.apply { 
            sizeHeightToChildren(100f)
        }
    }
    
}
