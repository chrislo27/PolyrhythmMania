package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.ui.PRManiaSkins
import java.time.format.DateTimeFormatter


class PlayMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    private var epochSeconds: Long = System.currentTimeMillis() / 1000

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
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 12f, 12f))
                }
            }
            
            vbox += createLongButtonWithNewIndicator(main.settings.newIndicatorLibrary) { Localization.getVar("mainMenu.play.library").use() }.apply {
                this.setOnAction {
                    main.settings.newIndicatorLibrary.value.set(false)
                    menuCol.pushNextMenu(menuCol.libraryMenu.prepareShow())
                }
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.library.tooltip")))
            }
            vbox += createLongButton { Localization.getVar("mainMenu.play.practice").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.practiceMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.play.playSavedLevel").use() }.apply {
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.playSavedLevel.tooltip")))
                this.setOnAction {
                    val loadMenu = LoadSavedLevelMenu(menuCol, null)
                    menuCol.addMenu(loadMenu)
                    menuCol.pushNextMenu(loadMenu)
                }
            }
            
            vbox += separator()

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
            val dailyChallengeDate = menuCol.dailyChallengeMenu.dailyChallengeDate
            val dailyChallengeTitle: ReadOnlyVar<String> = Localization.getVar("mainMenu.play.endless.daily", Var {
                listOf(dailyChallengeDate.use().format(DateTimeFormatter.ISO_DATE))
            })
            vbox += createLongButton { dailyChallengeTitle.use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.dailyChallengeMenu.prepareShow())
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
}
