package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.screen.PlayScreen
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.sidemodes.EndlessModeScore
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.endlessmode.EndlessHighScore
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.ui.PRManiaSkins
import java.util.*


class EndlessModeMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

//    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.play.endless").use() }
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
            val seedText: Var<String> = Var("")
            val disableRegen = BooleanVar(false)
            val daredevilMode = BooleanVar(false)
            val playButtonText: ReadOnlyVar<String> = Var {
                if (seedText.use().isEmpty()) Localization.getVar("mainMenu.play.endless.play.random").use() else Localization.getVar("mainMenu.play.endless.play").use()
            }
            vbox += createLongButton { playButtonText.use() }.apply {
                this.setOnAction {
                    menuCol.playMenuSound("sfx_menu_enter_game")
                    mainMenu.transitionAway {
                        val main = mainMenu.main
                        Gdx.app.postRunnable {
                            val seed: Long = try {
                                seedText.getOrCompute().toUInt(16).toLong()
                            } catch (e: Exception) {
                                Random().nextInt().toUInt().toLong()
                            }
                            val seedUInt = seed.toUInt()
                            val endlessHighScore = main.settings.endlessHighScore
                            val scoreVar = Var(endlessHighScore.getOrCompute().score)
                            scoreVar.addListener {
                                main.settings.endlessHighScore.set(EndlessHighScore(seedUInt, it.getOrCompute()))
                            }
                            val sidemode: SideMode = EndlessPolyrhythm(main,
                                    EndlessModeScore(scoreVar, showHighScore = true),
                                    seed, dailyChallenge = null,
                                    disableLifeRegen = disableRegen.get(),
                                    maxLives = if (daredevilMode.get()) 1 else -1)
                            val playScreen = PlayScreen(main, sidemode, sidemode.container,
                                    challenges = Challenges.NO_CHANGES, showResults = false,
                                    inputCalibration = main.settings.inputCalibration.getOrCompute(),
                                    levelScoreAttemptConsumer = null, previousHighScore = -1)
                            main.screen = TransitionScreen(main, main.screen, playScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                                this.onEntryEnd = {
                                    sidemode.prepare()
                                    playScreen.resetAndStartOver(false, false)
                                    DiscordCore.updateActivity(DefaultPresences.playingEndlessMode())
                                    mainMenu.backgroundType = BgType.ENDLESS
                                }
                            }
                        }
                    }
                }
            }
            
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 12f, 12f))
                }
            }
            
            vbox += separator()
            
            vbox += HBox().apply { 
                this.spacing.set(8f)
                this.bounds.height.set(48f)
                this.margin.set(Insets(4f, 4f, 0f, 0f))
                this += TextLabel(binding = {Localization.getVar("mainMenu.play.endless.settings.seed").use()}, font).apply { 
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(80f)
                }
                val textField = TextField(font = font).apply {
                    this.characterLimit.set(8)
                    this.inputFilter.set { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' }
                    this.textColor.set(Color(1f, 1f, 1f, 1f))
                    this.setOnRightClick {
                        requestFocus()
                        text.set("")
                    }
                    this.text.addListener { t ->
                        if (hasFocus.get()) {
                            val upper = t.getOrCompute().uppercase()
                            seedText.set(upper)
                            this.text.set(upper)
                        }
                    }
                }
                this += RectElement(Color.BLACK).apply { 
                    this.bounds.width.set(150f)
                    this.border.set(Insets(2f))
                    this.borderStyle.set(SolidBorder(Color.WHITE))
                    this.padding.set(Insets(4f))
                    this += textField
                }
                
                this += createSmallButton { Localization.getVar("mainMenu.play.endless.settings.clear").use() }.apply { 
                    this.bounds.width.set(80f)
                    this.setOnAction {
                        textField.requestUnfocus()
                        seedText.set("")
                        textField.text.set("")
                    }
                }
                this += createSmallButton { Localization.getVar("mainMenu.play.endless.settings.random").use() }.apply { 
                    this.bounds.width.set(120f)
                    this.setOnAction { 
                        textField.requestUnfocus()
                        val randomSeed = Random().nextInt().toUInt()
                        val randomSeedStr = randomSeed.toString(16).padStart(8, '0').uppercase()
                        seedText.set(randomSeedStr)
                        textField.text.set(randomSeedStr)
                    }
                }
                this += createSmallButton { "" }.apply copyButton@{
                    this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_copy"))).also { img ->
                        img.tint.bind { 
                            when (this@copyButton.pressedState.use()) {
                                PressedState.NONE, PressedState.HOVERED -> Color.WHITE
                                PressedState.PRESSED, PressedState.PRESSED_AND_HOVERED -> Color.LIGHT_GRAY
                            }
                        }
                    }
                    this.bindWidthToSelfHeight()
                    this.setOnAction { 
                        val seed = seedText.getOrCompute()
                        Gdx.app.clipboard?.contents = seed
                    }
                }
            }
            
            vbox += separator()
            
            val (disableRegenPane, disableRegenCheck) = createCheckboxOption({ Localization.getVar("mainMenu.play.endless.settings.disableRegen").use() })
            val (daredevilPane, daredevilCheck) = createCheckboxOption({ Localization.getVar("mainMenu.play.endless.settings.daredevil").use() })
            
            disableRegenCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.endless.settings.disableRegen.tooltip")))
            daredevilCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.endless.settings.daredevil.tooltip")))
            
            disableRegenCheck.disabled.bind { 
                daredevilMode.useB()
            }
            
            disableRegenCheck.onCheckChanged = { newState ->
                disableRegen.set(newState)
            }
            daredevilCheck.onCheckChanged = { newState ->
                daredevilMode.set(newState)
                if (newState) {
                    disableRegenCheck.checkedState.set(false)
                }
            }
            
            vbox += disableRegenPane
            vbox += daredevilPane
            
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
