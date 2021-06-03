package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.Markup
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.transition.FadeIn
import io.github.chrislo27.paintbox.transition.TransitionScreen
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.screen.mainmenu.MainMenuScreen


/**
 * Represents a main menu-menu.
 *
 * There is a list of options with fixed functions. They will either go to another [MMMenu] or require a
 * total screen wipe.
 *
 * When transitioning to another [MMMenu], the tile flip effect will play for just the maximal area that changes.
 *
 */
abstract class MMMenu(val menuCol: MenuCollection) : Pane() {

    companion object {
        const val WIDTH_SMALL: Float = 0.4f
        const val WIDTH_MEDIUM: Float = 0.6f
        const val WIDTH_LARGE: Float = 0.75f
        const val WIDTH_EXTRA_LARGE: Float = 1f
    }

    protected val root: SceneRoot get() = menuCol.sceneRoot
    protected val mainMenu: MainMenuScreen get() = menuCol.mainMenu

    protected fun setSize(percentage: Float, adjust: Float = 0f) {
        bounds.width.bind {
            (this@MMMenu.parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) * percentage + adjust
        }
    }
}

/**
 * The very beginning of the main menu.
 */
class UppermostMenu(menuCol: MenuCollection) : MMMenu(menuCol) {

    companion object {
        val BUTTON_SKIN_ID: String = "MMMenu_UppermostMenu_Button"

        init {
            DefaultSkins.register(BUTTON_SKIN_ID, SkinFactory { element: Button ->
                UppermostMenu.ButtonSkin(element)
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
        this.setSize(MMMenu.WIDTH_SMALL)
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
                this.disabled.set(true)
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.credits").use() }).apply { 
                this.disabled.set(true)
            }
            vbox += createButton(binding = { Localization.getVar("mainMenu.main.quit").use() }).apply {
                this.setOnAction {
                    menuCol.changeActiveMenu(menuCol.quitMenu, false)
                }
            }
        }
        this.bounds.height.set(buttonHeight * vbox.children.size)

        this += vbox
    }
}

/**
 * Standard menu.
 */
open class StandardMenu(menuCol: MenuCollection) : MMMenu(menuCol) {

    companion object {
        val BUTTON_LONG_SKIN_ID: String = "MMMenu_StandardMenu_Button_Long"
        val BUTTON_SMALL_SKIN_ID: String = "MMMenu_StandardMenu_Button_Small"

        init {
            DefaultSkins.register(BUTTON_LONG_SKIN_ID, SkinFactory { element: Button ->
                StandardMenu.LongButtonSkin(element)
            })
            DefaultSkins.register(BUTTON_SMALL_SKIN_ID, SkinFactory { element: Button ->
                StandardMenu.SmallButtonSkin(element)
            })
        }
    }

    class LongButtonSkin(element: Button) : UppermostMenu.ButtonSkin(element)

    class SmallButtonSkin(element: Button) : io.github.chrislo27.paintbox.ui.control.ButtonSkin(element) {
        init {
            this.defaultTextColor.set(Color().grey(0f, 1f))
            this.disabledTextColor.set(Color().grey(100f / 255f))
            this.hoveredTextColor.set(Color().grey(30f / 255f))
            this.pressedTextColor.set(Color(0.2f, 0.5f, 0.5f, 1f))
            this.pressedAndHoveredTextColor.set(Color(0.3f, 0.5f, 0.5f, 1f))

            this.defaultBgColor.set(Color().grey(145f / 255f, 1f))
            this.hoveredBgColor.set(Color().grey(155f / 255f, 1f))
            this.disabledBgColor.set(Color().grey(125f / 255f, 1f))
            this.pressedBgColor.bind { hoveredBgColor.use() }
            this.pressedAndHoveredBgColor.bind { hoveredBgColor.use() }

            this.roundedRadius.set(1)
        }
    }

    protected val font: PaintboxFont = mainMenu.main.fontMainMenuMain
    protected val markup: Markup = Markup(mapOf("prmania_icons" to mainMenu.main.fontIcons),
            TextRun(font, ""), Markup.FontStyles("bold", "italic", "bolditalic"))
    protected val titleHeight: Float = 64f
    protected val grey: Color = Color().grey(0.8f, 1f)

    protected val titleText: Var<String> = Var("")
    protected val titleLabel: TextLabel
    protected val contentPane: UIElement = RectElement(grey).apply {
        this.bounds.height.set(titleHeight * 2) // Default.
        this.padding.set(Insets(16f))
    }

    init {
        titleLabel = TextLabel(binding = { titleText.use() }, font = mainMenu.main.fontMainMenuHeading).apply {
            this.bounds.height.set(titleHeight)
            this.bgPadding.set(16f)
            this.backgroundColor.set(grey)
            this.renderBackground.set(true)
            this.renderAlign.set(Align.bottomLeft)
        }
        val vbox: VBox = VBox().apply {
            this.spacing.set(0f)
            this.align.set(VBox.Align.TOP)
            Anchor.BottomLeft.configure(this)
        }
        vbox.temporarilyDisableLayouts {
            vbox += titleLabel
            vbox += contentPane
        }
        addChild(vbox)
        vbox.bounds.height.bind { contentPane.bounds.height.use() + titleLabel.bounds.height.use() }

        this.bounds.height.bind { vbox.bounds.height.use() }
        this.setSize(MMMenu.WIDTH_MEDIUM) // Default size
    }

    protected fun createLongButton(binding: Var.Context.() -> String): Button = Button(binding, font = font).apply {
        this.skinID.set(BUTTON_LONG_SKIN_ID)
        this.padding.set(Insets(8f, 8f, 16f, 16f))
        this.bounds.height.set(48f)
        this.textAlign.set(TextAlign.LEFT)
        this.renderAlign.set(Align.left)
    }

    protected fun createSmallButton(binding: Var.Context.() -> String): Button = Button(binding, font = font).apply {
        this.skinID.set(BUTTON_SMALL_SKIN_ID)
        this.padding.set(Insets(2f))
        this.textAlign.set(TextAlign.CENTRE)
        this.renderAlign.set(Align.center)
        this.setScaleXY(0.75f)
    }
}