package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.layout.VBox
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.screen.mainmenu.bg.BgType

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
        this.setSize(WIDTH_EXTRA_SMALL)
        val vbox = VBox().apply {
            this.spacing.set(0f)
            this.align.set(VBox.Align.BOTTOM)
        }

        val font = mainMenu.main.fontMainMenuMain
        val buttonHeight = 48f
        fun createButton(binding: Var.Context.() -> String): Button = Button(binding, font = font).apply {
            this.skinID.set(BUTTON_SKIN_ID)
            this.padding.set(Insets(8f, 8f, 16f, 16f))
            this.bounds.height.set(buttonHeight)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.setOnHoverStart {
                menuCol.playBlipSound()
            }
        }
        vbox.temporarilyDisableLayouts {
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.play").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.playMenu)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.edit").use() }).apply { 
                this.setOnAction {
                    mainMenu.main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                    mainMenu.transitionAway {
                        val main = mainMenu.main
                        val editorScreen = EditorScreen(main)
                        main.screen = TransitionScreen(main, main.screen, editorScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply { 
                            this.onDestEnd = {
                                Gdx.app.postRunnable {
                                    mainMenu.backgroundType = BgType.NORMAL
                                }
                            }
                        }
                        DiscordCore.updateActivity(DefaultPresences.inEditor())
                    }
                }
            }
            // Remember to update DataSettingsMenu to reset high scores
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.extras").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.sideModesMenu)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.settings").use() }).apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.settingsMenu)
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
        this.bounds.height.set(buttonHeight * vbox.children.size)

        this += vbox
    }
    
}