package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxGame
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import paintbox.util.gdxutils.grey
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
            (this@MMMenu.parent.use()?.let { p -> p.contentZone.width.useF() } ?: 0f) * percentage + adjust
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

    class LongButtonSkin(element: Button) : UppermostMenu.ButtonSkin(element) {
        init {
            this.disabledTextColor.set(Color().grey(150f / 255f))
        }
    }

    class SmallButtonSkin(element: Button) : paintbox.ui.control.ButtonSkin(element) {
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
    protected val markup: Markup = Markup(mapOf(
            "prmania_icons" to main.fontIcons,
            "rodin" to main.fontMainMenuRodin,
            "thin" to main.fontMainMenuThin
    ), TextRun(font, ""), Markup.FontStyles("bold", "italic", "bolditalic"))
    protected val titleHeight: Float = 64f
    protected val grey: Color = Color().grey(0.8f, 1f)
    protected val blipSoundListener: (MouseEntered?) -> Unit = {
        menuCol.playBlipSound()
    }

    protected val titleText: Var<String> = Var("")
    protected val titleLabel: TextLabel
    protected val contentPane: UIElement = RectElement(grey).apply {
        this.bounds.height.set(titleHeight * 2) // Default.
        this.padding.set(Insets(16f))
    }

    init {
        titleLabel = TextLabel(binding = { titleText.use() }, font = main.fontMainMenuHeading).apply {
            this.bounds.height.set(titleHeight)
            this.bgPadding.set(Insets(16f, 16f, 16f, 32f))
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
        vbox.bounds.height.bind { contentPane.bounds.height.useF() + titleLabel.bounds.height.useF() }

        this.bounds.height.bind { vbox.bounds.height.useF() }
        this.setSize(MMMenu.WIDTH_MEDIUM) // Default size
    }

    protected fun createLongButton(binding: Var.Context.() -> String): Button = Button(binding, font = font).apply {
        this.skinID.set(BUTTON_LONG_SKIN_ID)
        this.padding.set(Insets(4f, 4f, 12f, 12f))
        this.bounds.height.set(40f)
        this.textAlign.set(TextAlign.LEFT)
        this.renderAlign.set(Align.left)
        this.setOnHoverStart(blipSoundListener)
    }

    protected fun createSmallButton(binding: Var.Context.() -> String): Button = Button(binding, font = font).apply {
        this.skinID.set(BUTTON_SMALL_SKIN_ID)
        this.padding.set(Insets(2f))
        this.textAlign.set(TextAlign.CENTRE)
        this.renderAlign.set(Align.center)
        this.setScaleXY(0.75f)
//        this.setOnHoverStart(blipSoundListener)
    }
    
    protected fun createSettingsOption(labelText: Var.Context.() -> String, font: PaintboxFont = this.font,
                                       percentageContent: Float = 0.5f): SettingsOptionPane {
        return SettingsOptionPane(labelText, font, percentageContent).apply {
            this.bounds.height.set(36f)
            this.addInputEventListener {
                if (it is MouseEntered) {
                    blipSoundListener(null)
                }
                false
            }
        }
    }
    
    protected fun createSliderPane(slider: Slider, labelText: Var.Context.() -> String): SettingsOptionPane {
        return createSettingsOption(labelText).apply {
            this.content.addChild(slider)
            Anchor.CentreRight.configure(slider)
        }
    }
    
    protected fun createCheckboxOption(labelText: Var.Context.() -> String, font: PaintboxFont = this.font,
                                     percentageContent: Float = 0.5f): Pair<SettingsOptionPane, CheckBox> {
        val settingsOptionPane = createSettingsOption(labelText, font, percentageContent)
        val checkBox = CheckBox("").apply { 
            Anchor.TopRight.configure(this)
            this.boxAlignment.set(CheckBox.BoxAlign.RIGHT)
            this.imageNode.tint.bind { settingsOptionPane.textColorVar.use() }
            this.textLabel.textColor.bind { settingsOptionPane.textColorVar.use() }
        }
        settingsOptionPane.content += checkBox
        
        return settingsOptionPane to checkBox
    }

    protected fun <T> createCycleOption(items: List<T>, firstItem: T,
                                        labelText: Var.Context.() -> String, font: PaintboxFont = this.font,
                                       percentageContent: Float = 0.5f): Pair<SettingsOptionPane, CycleControl<T>> {
        val settingsOptionPane = createSettingsOption(labelText, font, percentageContent)
        val cycle = CycleControl<T>(settingsOptionPane, items, firstItem)
        settingsOptionPane.content += cycle

        return settingsOptionPane to cycle
    }

    open class SettingsOptionPane(labelText: Var.Context.() -> String, val font: PaintboxFont,
                                  percentageContent: Float = 0.5f)
        : Pane(), HasPressedState by HasPressedState.DefaultImpl() {

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
                this.bindWidthToParent(adjust = 0f, multiplier = 1f - percentageContent)
                this.textColor.bind { textColorVar.use() }
                this.renderAlign.set(Align.left)
                this.textAlign.set(TextAlign.RIGHT)
            }
            rect.addChild(label)
            
            content = Pane().apply {
                Anchor.TopRight.configure(this)
                this.bindWidthToParent(adjust = 0f, multiplier = percentageContent)
            }
            rect.addChild(content)
            
            @Suppress("LeakingThis")
            HasPressedState.DefaultImpl.addDefaultPressedStateInputListener(this)
        }
    }
    
    class CycleControl<T>(settingsOptionPane: SettingsOptionPane, val list: List<T>, firstItem: T)
        : Pane(), HasPressedState by HasPressedState.DefaultImpl() {
        
        val left: Button
        val right: Button
        val label: TextLabel
        
        val currentItem: Var<T> = Var(firstItem)
        
        init {
            left = Button("").apply { 
                Anchor.TopLeft.configure(this)
                this.bindWidthToSelfHeight()
                this.skinID.set(BUTTON_LONG_SKIN_ID)
                addChild(ImageNode(TextureRegion(PaintboxGame.paintboxSpritesheet.upArrow)).apply { 
                    this.rotation.set(90f)
                    this.padding.set(Insets(4f))
                    this.tint.bind { settingsOptionPane.textColorVar.use() }
                })
                this.setOnAction { 
                    val index = list.indexOf(currentItem.getOrCompute())
                    val nextIndex = (index - 1 + list.size) % list.size
                    currentItem.set(list[nextIndex])
                }
            }
            right = Button("").apply { 
                Anchor.TopRight.configure(this)
                this.bindWidthToSelfHeight()
                this.skinID.set(BUTTON_LONG_SKIN_ID)
                addChild(ImageNode(TextureRegion(PaintboxGame.paintboxSpritesheet.upArrow)).apply {
                    this.rotation.set(270f)
                    this.padding.set(Insets(4f))
                    this.tint.bind { settingsOptionPane.textColorVar.use() }
                })
                this.setOnAction {
                    val index = list.indexOf(currentItem.getOrCompute())
                    val nextIndex = (index + 1) % list.size
                    currentItem.set(list[nextIndex])
                }
            }
            label = TextLabel(binding = {currentItem.use().toString()}, font = settingsOptionPane.font).apply {
                Anchor.Centre.configure(this)
                this.bindWidthToParent { -(bounds.height.useF() * 2) }
                this.textColor.bind { settingsOptionPane.textColorVar.use() }
                this.textAlign.set(TextAlign.CENTRE)
                this.renderAlign.set(Align.center)
            }
            
            addChild(left)
            addChild(right)
            addChild(label)
            
            @Suppress("LeakingThis")
            HasPressedState.DefaultImpl.addDefaultPressedStateInputListener(this)
        }
    }
}