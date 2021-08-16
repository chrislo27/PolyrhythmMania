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


class PlaySideModesMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {


    init {
        this.setSize(MMMenu.WIDTH_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.play.sideModes").use() }
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
}
