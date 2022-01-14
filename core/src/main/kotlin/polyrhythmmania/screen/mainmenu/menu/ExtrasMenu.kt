package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
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
import polyrhythmmania.Settings
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.screen.play.ResultsBehaviour
import polyrhythmmania.sidemodes.AssembleMode
import polyrhythmmania.sidemodes.DunkMode
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.solitaire.SolitaireMenu
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.ui.PRManiaSkins


class ExtrasMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = main.settings
    val anyNewIndicators: ReadOnlyBooleanVar = BooleanVar {
        settings.newIndicatorExtrasAssemble.value.use() || settings.newIndicatorExtrasSolitaire.value.use()
    }

    init {
        this.setSize(MMMenu.WIDTH_SMALL_MID)
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
            this.margin.set(Insets(0f, 0f, 0f, 4f))
        }

        vbox.temporarilyDisableLayouts {
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 12f, 12f))
                }
            }
            
            // Remember to update DataSettingsMenu to reset high scores
            vbox += createSidemodeLongButton(AssetRegistry.get<PackedSheet>("achievements_icon")["dunk"],
                    "mainMenu.play.dunk", Localization.getVar("mainMenu.play.dunk.tooltip",
                    Var { listOf(use(main.settings.endlessDunkHighScore)) })) { main, _ ->
                DiscordCore.updateActivity(DefaultPresences.playingDunk())
                mainMenu.backgroundType = BgType.DUNK
                GlobalStats.timesPlayedDunk.increment()
                DunkMode(main, EndlessModeScore(settings.endlessDunkHighScore))
            }
            vbox += createSidemodeLongButton(AssetRegistry.get<PackedSheet>("achievements_icon")["assemble"],
                    "mainMenu.play.assemble", Localization.getVar("mainMenu.play.assemble.tooltip",
                    Var { listOf(use(main.settings.sidemodeAssembleHighScore)) }),
                    resultsBehaviour = ResultsBehaviour.ShowResults(null, null), // Note: high score is handled by EndlessModeScore
                    newIndicator = main.settings.newIndicatorExtrasAssemble) { main, _ ->
                DiscordCore.updateActivity(DefaultPresences.playingAssemble())
                mainMenu.backgroundType = BgType.ASSEMBLE
                GlobalStats.timesPlayedAssemble.increment()
                AssembleMode(main, EndlessModeScore(settings.sidemodeAssembleHighScore))
            }
            vbox += createLongButtonWithNewIndicator(settings.newIndicatorExtrasSolitaire, AssetRegistry.get<PackedSheet>("achievements_icon")["solitaire"]) {
                Localization.getVar("mainMenu.play.solitaire").use() 
            }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.solitaireMenu)
                    
                    val newIndicator = settings.newIndicatorExtrasSolitaire
                    if (newIndicator.value.get()) {
                        newIndicator.value.set(false)
                        main.settings.persist()
                    }
                }
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.solitaire.tooltip")))
            }

            vbox += separator()
            vbox += createLongButtonWithIcon(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_music"]) {
                Localization.getVar("mainMenu.extras.ost").use()
            }.apply {
                val link = """https://www.youtube.com/playlist?list=PLt_3dgnFrUPwcA6SdTfi0RapEBdQV64v_"""
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.extras.ost.tooltip", Var { listOf(link) })))
                this.setOnAction {
                    Gdx.net.openURI(link)
                }
            }
            vbox += createLongButtonWithIcon(TextureRegion(AssetRegistry.get<Texture>("mainmenu_rhre"))) {
                """Rhythm Heaven Remix Editor""" 
            }.apply {
                val rhreGithub = """https://github.com/chrislo27/RhythmHeavenRemixEditor"""
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.extras.rhre.tooltip", Var { listOf(rhreGithub) })))
                this.setOnAction {
                    Gdx.net.openURI(rhreGithub)
                }
            }
            vbox += createLongButtonWithIcon(TextureRegion(AssetRegistry.get<Texture>("mainmenu_brm"))) { 
                """Bouncy Road Mania"""
            }.apply {
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
