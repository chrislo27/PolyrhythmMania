package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.ContextBinding
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.layout.VBox
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordRichPresence
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.screen.SimpleLoadingScreen
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.screen.StoryTitleScreen
import polyrhythmmania.storymode.test.TestStoryGimmickDebugScreen
import polyrhythmmania.util.Semitones

/**
 * The very beginning of the main menu.
 */
class UppermostMenu(menuCol: MenuCollection) : MMMenu(menuCol) {

    companion object {
        val BUTTON_SKIN_ID: String = "MMMenu_UppermostMenu_Button"

        init {
            DefaultSkins.register(BUTTON_SKIN_ID, SkinFactory { element: Button ->
                ButtonSkin(element)
            })
        }
    }

    open class ButtonSkin(element: Button) : paintbox.ui.control.ButtonSkin(element) {
        companion object {
            val TEXT_COLOR: Color = Color().grey(90f / 255f, 1f)
            val DISABLED_TEXT: Color = Color().grey(30f / 255f)
            val HOVERED_TEXT: Color = Color().grey(1f)
            val PRESSED_TEXT: Color = Color(0.4f, 1f, 1f, 1f)
            val PRESSED_AND_HOVERED_TEXT: Color = Color(0.5f, 1f, 1f, 1f)

            val BG_COLOR: Color = Color(1f, 1f, 1f, 0f)
            val HOVERED_BG: Color = Color().grey(90f / 255f, 0.8f)
            val DISABLED_BG: Color = BG_COLOR.cpy()
            val PRESSED_BG: Color = HOVERED_BG.cpy()
            val PRESSED_AND_HOVERED_BG: Color = HOVERED_BG.cpy()
        }

        init {
            val grey = TEXT_COLOR
            this.defaultTextColor.set(grey)
            this.disabledTextColor.set(DISABLED_TEXT)
            this.hoveredTextColor.set(HOVERED_TEXT)
            this.pressedTextColor.set(PRESSED_TEXT)
            this.pressedAndHoveredTextColor.set(PRESSED_AND_HOVERED_TEXT)
            this.defaultBgColor.set(BG_COLOR)
            this.hoveredBgColor.set(HOVERED_BG)
            this.disabledBgColor.set(DISABLED_BG)
            this.pressedBgColor.set(PRESSED_BG)
            this.pressedAndHoveredBgColor.set(PRESSED_AND_HOVERED_BG)
            this.roundedRadius.set(0)
        }
    }

    init {
        this.setSize(0.325f)
        val vbox = VBox().apply {
            this.spacing.set(0f)
        }

        val font = mainMenu.main.fontMainMenuMain
        val markupWithFont = Markup.createWithSingleFont(font, lenientMode = true)
        val buttonHeight = 40f
        fun createButton(binding: ContextBinding<String>): Button = Button(binding, font = font).apply {
            this.skinID.set(BUTTON_SKIN_ID)
            this.padding.set(Insets(4f, 4f, 16f, 16f))
            this.bounds.height.set(buttonHeight)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.markup.set(markupWithFont)
            this.setOnHoverStart {
                menuCol.playBlipSound()
            }
        }

        val settings = menuCol.main.settings
        vbox.temporarilyDisableLayouts {
            vbox += createButton(binding = {// TODO remove me, test story mode gimmicks
                "TEST: Story Mode debug screen"
            }).apply {
                this.visible.bind { 
                    PRMania.isDevVersion || (PRMania.isPrereleaseVersion && Paintbox.debugMode.use())
                }
                this.setOnAction {
                    mainMenu.main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"), 1f, Semitones.getALPitch(-2), 0f)

                    val main = mainMenu.main
                    val storySession = StorySession()
                    val doAfterLoad: () -> Unit = {
                        val newScreen = TestStoryGimmickDebugScreen(main, storySession)
                        Gdx.app.postRunnable {
                            newScreen.render(1 / 60f)
                            Gdx.app.postRunnable {
                                main.screen = TransitionScreen(main, main.screen, newScreen,
                                    FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }

                    main.screen = TransitionScreen(main, main.screen, storySession.createEntryLoadingScreen(main, doAfterLoad),
                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
                }
            }
            
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.play").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.playMenu)
                }
            }
            vbox += createButton(binding = {
                (if (settings.newIndicatorStoryMode.value.use())
                    ("[color=#4AFF4A]${Localization.getVar("common.newIndicator").use()}[] ")
                else "") + Localization.getVar("mainMenu.main.storyMode").use()
            }).apply {
                this.setOnAction {
                    mainMenu.main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"), 1f, Semitones.getALPitch(-2), 0f)

                    val main = mainMenu.main
                    val storySession = StorySession()
                    val checkNewIndicator: () -> Unit = {
                        val newIndicator = settings.newIndicatorStoryMode
                        if (newIndicator.value.get()) {
                            newIndicator.value.set(false)
                            settings.persist()
                        }
                    }
                    val doAfterLoad: () -> Unit = {
                        val newScreen = StoryTitleScreen(main, storySession)
                        Gdx.app.postRunnable {
                            checkNewIndicator()
                            newScreen.render(1 / 60f)
                            Gdx.app.postRunnable {
                                main.screen = TransitionScreen(main, main.screen, newScreen,
                                        FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)).apply {
                                    this.onDestEnd = {
                                        val musicHandler = storySession.musicHandler
                                        musicHandler.transitionToStemMix(musicHandler.getTitleStemMix(), 0f, delaySec = 0f)
                                    }
                                }
                            }
                        }
                    }

                    main.screen = TransitionScreen(main, main.screen, storySession.createEntryLoadingScreen(main, doAfterLoad),
                            FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                }
            }

            vbox += createButton(binding = { Localization.getVar("mainMenu.main.edit").use() }).apply {
                this.setOnAction {
                    mainMenu.main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                    mainMenu.transitionAway {
                        DiscordRichPresence.updateActivity(DefaultPresences.inEditor())

                        val main = mainMenu.main
                        main.screen = TransitionScreen(main, main.screen, SimpleLoadingScreen(main), null, FadeToTransparent(0.125f, Color.BLACK)).apply {
                            this.onDestEnd = {
                                Gdx.app.postRunnable {
                                    mainMenu.backgroundType = BgType.NORMAL

                                    val editorScreen = EditorScreen(main)
                                    Gdx.app.postRunnable {
                                        editorScreen.render(1 / 60f)
                                        Gdx.app.postRunnable {
                                            main.screen = TransitionScreen(main, main.screen, editorScreen,
                                                    FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Remember to update DataSettingsMenu to reset high scores
            vbox += createButton(binding = {
                (if (menuCol.extrasMenu.anyNewIndicators.use())
                    (Localization.getVar("common.newIndicator").use() + " ")
                else "") + Localization.getVar("mainMenu.main.extras").use()
            }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.extrasMenu)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.achievementsStats").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.achievementsStatsForkMenu)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.settings").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.settingsMenu)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.updateNotes").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.updateNotesMenu.prepareShow())
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.credits").use() }).apply {
                this.setOnAction {
                    val next = CreditsMenu(menuCol)
                    menuCol.addMenu(next)
                    menuCol.pushNextMenu(next)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.quit").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.quitMenu)
                }
            }
        }
        vbox.sizeHeightToChildren()
        this.bounds.height.bind { vbox.bounds.height.use() }

        this += vbox
    }

}