package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.Markup
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaGame
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
        const val WIDTH_EXTRA_SMALL: Float = 0.3f
        const val WIDTH_SMALL: Float = 0.4f
        const val WIDTH_MID: Float = 0.5f
        const val WIDTH_MEDIUM: Float = 0.6f
        const val WIDTH_LARGE: Float = 0.75f
        const val WIDTH_FULL: Float = 1f
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

    protected val main: PRManiaGame get() = mainMenu.main
    protected val font: PaintboxFont = main.fontMainMenuMain
    protected val markup: Markup = Markup(mapOf("prmania_icons" to main.fontIcons, "rodin" to main.fontMainMenuRodin),
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
        titleLabel = TextLabel(binding = { titleText.use() }, font = main.fontMainMenuHeading).apply {
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
        this.padding.set(Insets(4f, 4f, 12f, 12f))
        this.bounds.height.set(40f)
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
    
    protected fun createSettingsOption(labelText: Var.Context.() -> String): SettingsOptionPane {
        return SettingsOptionPane(labelText, this.font).apply {
            this.bounds.height.set(36f)
        }
    }

    open class SettingsOptionPane(labelText: Var.Context.() -> String, font: PaintboxFont) : Pane() {

        val isHoveredOver: ReadOnlyVar<Boolean> = Var(false)
        val textColorVar: ReadOnlyVar<Color> = Var.bind {
            if (isHoveredOver.use()) UppermostMenu.ButtonSkin.HOVERED_TEXT else UppermostMenu.ButtonSkin.TEXT_COLOR
        }
        val bgColorVar: ReadOnlyVar<Color> = Var.bind {
            if (isHoveredOver.use()) UppermostMenu.ButtonSkin.HOVERED_BG else UppermostMenu.ButtonSkin.BG_COLOR
        }

        val label: TextLabel
        val content: Pane

        init {
            val rect = RectElement(binding = { bgColorVar.use() }).apply {
                this.padding.set(Insets(4f))
            }
            addChild(rect)
            
            label = TextLabel(labelText, font).apply {
                Anchor.TopLeft.configure(this)
                this.bindWidthToParent(adjust = 0f, multiplier = 0.5f)
                this.textColor.bind { textColorVar.use() }
                this.renderAlign.set(Align.left)
                this.textAlign.set(TextAlign.RIGHT)
            }
            rect.addChild(label)
            
            content = Pane().apply {
                Anchor.TopRight.configure(this)
                this.bindWidthToParent(adjust = 0f, multiplier = 0.5f)
            }
            rect.addChild(content)

            addInputEventListener { event ->
                when (event) {
                    is MouseEntered -> {
                        (isHoveredOver as Var).set(true)
                    }
                    is MouseExited -> {
                        (isHoveredOver as Var).set(false)
                    }
                }
                false
            }
        }
    }
}