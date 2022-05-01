package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordRichPresence
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.screen.play.ResultsBehaviour
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.practice.*
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.ui.PRManiaSkins


class PracticeMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.practice.title").use() }
        this.contentPane.bounds.height.set(280f)

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
            this.padding.set(Insets(4f, 0f, 2f, 2f))
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
            fun createPractice(name: String, factory: (PRManiaGame, InputKeymapKeyboard) -> AbstractPracticeTutorial): UIElement {
                return createSidemodeLongButton(null, name, Localization.getVar("${name}.tooltip"), Challenges.NO_CHANGES) { game, keymap ->
                    DiscordRichPresence.updateActivity(DefaultPresences.playingPractice())
                    mainMenu.backgroundType = BgType.PRACTICE_NORMAL
                    GlobalStats.timesPlayedTutorial.increment()
                    factory.invoke(game, keymap)
                }
            }

            fun createSidemode(name: String, tooltipVar: ReadOnlyVar<String> = Localization.getVar("${name}.tooltip"),
                               challenges: Challenges = Challenges.NO_CHANGES,
                               resultsBehaviour: ResultsBehaviour,
                               factory: (PRManiaGame, InputKeymapKeyboard) -> GameMode): UIElement {
                return createSidemodeLongButton(null, name, tooltipVar, challenges, resultsBehaviour) { game, keymap ->
                    DiscordRichPresence.updateActivity(DefaultPresences.playingPractice())
                    mainMenu.backgroundType = BgType.PRACTICE_NORMAL
                    factory.invoke(game, keymap)
                }
            }

            vbox += createPractice("mainMenu.practice.tutorial1") { main, keymap ->
                PracticeTutorial1(main, keymap)
            }
            vbox += createSidemode("mainMenu.practice.polyrhythm1",
                    Localization.getVar("mainMenu.practice.polyrhythm1.tooltip"),
                    Challenges.NO_CHANGES, ResultsBehaviour.ShowResults(null, null)) { main, _ ->
                GlobalStats.timesPlayedPracticePolyrhythm1.increment()
                Polyrhythm1Practice(main)
            }
            vbox += createPractice("mainMenu.practice.tutorial2") { main, keymap ->
                PracticeTutorial2(main, keymap)
            }
            vbox += createSidemode("mainMenu.practice.polyrhythm2",
                    Localization.getVar("mainMenu.practice.polyrhythm2.tooltip"),
                    Challenges.NO_CHANGES, ResultsBehaviour.ShowResults(null, null)) { main, _ ->
                GlobalStats.timesPlayedPracticePolyrhythm2.increment()
                Polyrhythm2Practice(main)
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
