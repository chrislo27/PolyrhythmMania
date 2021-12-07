package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
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
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.sidemodes.AssembleMode
import polyrhythmmania.sidemodes.DunkMode
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.ui.PRManiaSkins


class ExtrasMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(MMMenu.WIDTH_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.play.extras").use() }
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
            
            // Remember to update DataSettingsMenu to reset high scores
            vbox += createSidemodeLongButton("mainMenu.play.dunk", Localization.getVar("mainMenu.play.dunk.tooltip",
                    Var { listOf(use(main.settings.endlessDunkHighScore)) })) { main, _ ->
                DiscordCore.updateActivity(DefaultPresences.playingDunk())
                mainMenu.backgroundType = BgType.DUNK
                DunkMode(main, EndlessModeScore(main.settings.endlessDunkHighScore))
            }
            vbox += createSidemodeLongButton("mainMenu.play.assemble", Localization.getVar("mainMenu.play.assemble.tooltip",
                    Var { listOf(use(main.settings.sidemodeAssembleHighScore)) }), showResults = true,
                    newIndicator = main.settings.newIndicatorExtrasAssemble) { main, _ ->
                DiscordCore.updateActivity(DefaultPresences.playingAssemble())
                mainMenu.backgroundType = BgType.ASSEMBLE
                AssembleMode(main, EndlessModeScore(main.settings.sidemodeAssembleHighScore))
            }
            
//            vbox += createLongButton { """[font=thin]Future Spot for Side Mode #3[]""" }.apply {
//                this.disabled.set(true)
//                this.tooltipElement.set(createTooltip { "This NEW side mode will be developed for a future update if sufficient\ndevelopment costs are recovered — please consider donating\nto help with development costs! Donation link (goes to PayPal):\n[color=prmania_tooltip_keystroke scale=1]https://donate-to-polyrhythmmania.rhre.dev[]" })
//            }

            vbox += separator()
            vbox += createLongButton { Localization.getVar("mainMenu.extras.ost").use() }.apply {
                val link = """https://www.youtube.com/playlist?list=PLt_3dgnFrUPwcA6SdTfi0RapEBdQV64v_"""
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.extras.ost.tooltip", Var { listOf(link) })))
                this.setOnAction {
                    Gdx.net.openURI(link)
                }
            }
            vbox += createLongButtonWithIcon(TextureRegion(AssetRegistry.get<Texture>("mainmenu_rhre"))) { """Rhythm Heaven Remix Editor""" }.apply {
                val rhreGithub = """https://github.com/chrislo27/RhythmHeavenRemixEditor"""
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.extras.rhre.tooltip", Var { listOf(rhreGithub) })))
                this.setOnAction {
                    Gdx.net.openURI(rhreGithub)
                }
            }
            vbox += createLongButtonWithIcon(TextureRegion(AssetRegistry.get<Texture>("mainmenu_brm"))) { """Bouncy Road Mania""" }.apply {
                val brmGithub = """https://github.com/chrislo27/BouncyRoadMania"""
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.extras.brm.tooltip", Var { listOf(brmGithub) })))
                this.setOnAction {
                    Gdx.net.openURI(brmGithub)
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
