package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.transition.FadeIn
import io.github.chrislo27.paintbox.transition.TransitionScreen
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.editor.EditorScreen

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

    open class ButtonSkin(element: Button) : io.github.chrislo27.paintbox.ui.control.ButtonSkin(element) {
        init {
            val grey = Color().grey(90f / 255f, 1f)
            this.defaultTextColor.set(grey)
            this.disabledTextColor.set(Color().grey(30f / 255f))
            this.hoveredTextColor.set(Color().grey(1f))
            this.pressedTextColor.set(Color(0.4f, 1f, 1f, 1f))
            this.pressedAndHoveredTextColor.set(Color(0.5f, 1f, 1f, 1f))
            this.defaultBgColor.set(Color(1f, 1f, 1f, 0f))
            this.hoveredBgColor.set(grey.cpy().apply { a *= 0.8f })
            this.disabledBgColor.bind { defaultBgColor.use() }
            this.pressedBgColor.bind { hoveredBgColor.use() }
            this.pressedAndHoveredBgColor.bind { hoveredBgColor.use() }
            this.roundedRadius.set(0)
        }
    }

    init {
        this.setSize(WIDTH_SMALL)
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
        }
        vbox.temporarilyDisableLayouts {
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.play").use() }).apply {
                this.disabled.set(true)
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.edit").use() }).apply { 
                this.setOnAction { 
                    mainMenu.transitionAway { 
                        val main = mainMenu.main
                        val editorScreen = EditorScreen(main)
                        main.screen = TransitionScreen(main, main.screen, editorScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f)))
                    }
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.settings").use() }).apply {
                this.setOnAction {
                    menuCol.pushNewMenu(menuCol.settingsMenu)
                }
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.credits").use() }).apply { 
                this.disabled.set(true)
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.quit").use() }).apply {
                this.setOnAction {
                    menuCol.pushNewMenu(menuCol.quitMenu)
                }
            }
        }
        this.bounds.height.set(buttonHeight * vbox.children.size)

        this += vbox
    }
}