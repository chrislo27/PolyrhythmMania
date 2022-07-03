package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.ui.skin.Skinnable
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.screen.mainmenu.menu.InputSettingsMenu.PendingKeyboardBinding
import polyrhythmmania.ui.PRManiaSkins


class InputSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings

    val pendingKeyboardBinding: Var<PendingKeyboardBinding?> = mainMenu.pendingKeyboardBinding
    val keyboardSettings: KeyboardInputMenu = this.KeyboardInputMenu(menuCol)
    val feedbackSettings: InputFeedbackMenu = this.InputFeedbackMenu(menuCol)

    init {
        this.setSize(MMMenu.WIDTH_EXTRA_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.inputSettings.title").use() }
        this.contentPane.bounds.height.set(250f)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(vbox)
        contentPane.addChild(hbox)

        fun separator(): UIElement {
            return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                this.bounds.height.set(10f)
                this.margin.set(Insets(4f, 4f, 0f, 0f))
            }
        }
        vbox.temporarilyDisableLayouts {
            vbox += createLongButton { Localization.getVar("mainMenu.inputSettings.keyboard").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(keyboardSettings)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.inputSettings.feedback").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(feedbackSettings)
                }
            }
            vbox += separator()
            vbox += createLongButton { Localization.getVar("mainMenu.audioSettings.goToCalibration").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.calibrationSettingsMenu)
                }
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }

        menuCol.addMenu(keyboardSettings)
        menuCol.addMenu(feedbackSettings)
    }

    fun interface PendingKeyboardBinding {
        enum class Status {
            GOOD, CANCELLED
        }

        fun onInput(status: Status, key: Int)
        
        fun sendCancellation() {
            onInput(Status.CANCELLED, Input.Keys.ESCAPE)
        }
    }

    inner class KeyboardInputMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

        val inputKeymap: Var<InputKeymapKeyboard> = settings.inputKeymapKeyboard

        init {
            this.setSize(MMMenu.WIDTH_MID)
            this.titleText.bind { Localization.getVar("mainMenu.inputSettings.keyboard.title").use() }
            this.contentPane.bounds.height.set(300f)

            val scrollPane = ScrollPane().apply {
                Anchor.TopLeft.configure(this)
                this.bindHeightToParent(-40f)

                this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

                val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
                this.vBar.skinID.set(scrollBarSkinID)
                this.hBar.skinID.set(scrollBarSkinID)
            }
            val bottomBar = Pane().apply {
                Anchor.BottomLeft.configure(this)
                this.bounds.height.set(40f)
            }
            val hboxButtons = HBox().apply {
                this.spacing.set(8f)
                this.padding.set(Insets(4f, 0f, 2f, 2f))
                this.visible.bind {
                    pendingKeyboardBinding.use() == null
                }
            }
            contentPane.addChild(scrollPane)
            contentPane.addChild(bottomBar)
            bottomBar.addChild(hboxButtons)
            val cancelPrompt = TextLabel(Localization.getVar("mainMenu.inputSettings.keyboard.cancelPrompt"),
                    font = main.fontMainMenuItalic).apply {
                this.renderAlign.set(Align.center)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.visible.bind { 
                    pendingKeyboardBinding.use() != null
                }
            }
            bottomBar.addChild(cancelPrompt)

            val vbox = VBox().apply {
                Anchor.TopLeft.configure(this)
                this.bounds.height.set(300f)
                this.spacing.set(0f)
            }

            vbox.temporarilyDisableLayouts {
                vbox += createKeyboardInput({ InputKeymapKeyboard.TEXT_BUTTON_A }, { code ->
                    inputKeymap.set(inputKeymap.getOrCompute().copy(buttonA = code))
                }, { inputKeymap.getOrCompute().buttonA })
                vbox += createKeyboardInput({ InputKeymapKeyboard.TEXT_BUTTON_DPAD_UP }, { code ->
                    inputKeymap.set(inputKeymap.getOrCompute().copy(buttonDpadUp = code))
                }, { inputKeymap.getOrCompute().buttonDpadUp })
                vbox += createKeyboardInput({ InputKeymapKeyboard.TEXT_BUTTON_DPAD_DOWN }, { code ->
                    inputKeymap.set(inputKeymap.getOrCompute().copy(buttonDpadDown = code))
                }, { inputKeymap.getOrCompute().buttonDpadDown })
                vbox += createKeyboardInput({ InputKeymapKeyboard.TEXT_BUTTON_DPAD_LEFT }, { code ->
                    inputKeymap.set(inputKeymap.getOrCompute().copy(buttonDpadLeft = code))
                }, { inputKeymap.getOrCompute().buttonDpadLeft })
                vbox += createKeyboardInput({ InputKeymapKeyboard.TEXT_BUTTON_DPAD_RIGHT }, { code ->
                    inputKeymap.set(inputKeymap.getOrCompute().copy(buttonDpadRight = code))
                }, { inputKeymap.getOrCompute().buttonDpadRight })
                vbox += createKeyboardInput({ Localization.getVar("mainMenu.inputSettings.keyboard.keybindPause").use() }, { code ->
                    inputKeymap.set(inputKeymap.getOrCompute().copy(pause = code))
                }, { inputKeymap.getOrCompute().pause })
            }
            vbox.sizeHeightToChildren(100f)
            scrollPane.setContent(vbox)

            hboxButtons.temporarilyDisableLayouts {
                hboxButtons += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                    this.bounds.width.set(100f)
                    this.setOnAction {
                        menuCol.popLastMenu()
                    }
                    this.disabled.bind { pendingKeyboardBinding.use() != null }
                }
                hboxButtons += createSmallButton(binding = { Localization.getVar("mainMenu.inputSettings.resetToDefault").use() }).apply {
                    this.bounds.width.set(280f)
                    this.setOnAction {
                        inputKeymap.set(InputKeymapKeyboard())
                    }
                    this.disabled.bind { pendingKeyboardBinding.use() != null }
                }
            }
        }

        private fun createKeyboardInput(labelText: Var.Context.() -> String, setter: (code: Int) -> Unit, getter: () -> Int): SettingsOptionPane {
            val inwardArrows = BooleanVar(false)
            val skinFactory: SkinFactory<Button, ButtonSkin, Button> = SkinFactory { button: Button ->
                object : ButtonSkin(button) {
                    init {
                        this.roundedRadius.set(0)
                        
                        val goodTextColor = Color(0f, 0f, 0f, 1f)
                        val badTextColor = Color(1f, 0f, 0f, 1f)
                        val textColor = Var.bind {
                            if (!inwardArrows.use() && settings.inputKeymapKeyboard.use().numOccurrencesOfThisKey(getter()) > 1) {
                                badTextColor
                            } else {
                                goodTextColor
                            }
                        }
                        this.defaultTextColor.bind { textColor.use() }
                        this.hoveredTextColor.bind { textColor.use() }
                        this.pressedTextColor.bind { textColor.use() }
                        this.pressedAndHoveredTextColor.bind { textColor.use() }
                    }
                }
            }
            
            return createSettingsOption(labelText, font = main.fontMainMenuRodin, percentageContent = 0.65f).apply settingsOptionPane@{
                pendingKeyboardBinding.addListener {
                    if (it.getOrCompute() == null) inwardArrows.set(false)
                }
                val pane = Pane().also { pane ->
                    pane += TextLabel(binding = { if (inwardArrows.use()) ">" else "<" }, font = main.fontMainMenuMain).apply { 
                        this.bindWidthToParent(multiplier = 0.175f)
                        Anchor.TopLeft.configure(this)
                        this.renderAlign.set(Align.center)
                        this.textColor.bind { this@settingsOptionPane.textColorVar.use() }
                        this.visible.bind { inwardArrows.use() }
                        this.doXCompression.set(false)
                    }
                    pane += TextLabel(binding = { if (inwardArrows.use()) "<" else ">" }, font = main.fontMainMenuMain).apply { 
                        this.bindWidthToParent(multiplier = 0.175f)
                        Anchor.TopRight.configure(this)
                        this.renderAlign.set(Align.center)
                        this.textColor.bind { this@settingsOptionPane.textColorVar.use() }
                        this.visible.bind { inwardArrows.use() }
                        this.doXCompression.set(false)
                    }
                    pane += Button(binding = {
                        settings.inputKeymapKeyboard.use()
                        if (inwardArrows.use()) {
                            "..."
                        } else Input.Keys.toString(getter())
                    }, font = main.fontMainMenuRodin).apply {
                        this.bindWidthToParent(multiplier = 0.65f)
                        Anchor.Centre.configure(this)
                        @Suppress("UNCHECKED_CAST")
                        this.skinFactory.set(skinFactory as SkinFactory<Button, Skin<Button>, Skinnable<Button>>)
                        this.setOnAction { 
                            val currentPendingKBBinding = pendingKeyboardBinding.getOrCompute()
                            if (currentPendingKBBinding != null) {
                                currentPendingKBBinding.sendCancellation()
                                pendingKeyboardBinding.set(null)
                            } else {
                                inwardArrows.set(true)
                                pendingKeyboardBinding.set(PendingKeyboardBinding { status, keycode ->
                                    if (status == PendingKeyboardBinding.Status.GOOD) {
                                        setter.invoke(keycode)
                                    }
                                })
                            }
                        }
                    }
                }
                this.content.addChild(pane)
                Anchor.Centre.configure(pane)
            }
        }
    }

    inner class InputFeedbackMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
        
        init {
            this.setSize(MMMenu.WIDTH_MID)
            this.titleText.bind { Localization.getVar("mainMenu.inputSettings.feedback").use() }
            this.contentPane.bounds.height.set(300f)

            val scrollPane = ScrollPane().apply {
                Anchor.TopLeft.configure(this)
                this.bindHeightToParent(-40f)

                this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

                val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
                this.vBar.skinID.set(scrollBarSkinID)
                this.hBar.skinID.set(scrollBarSkinID)
            }
            val hbox = HBox().apply {
                Anchor.BottomLeft.configure(this)
                this.spacing.set(8f)
                this.padding.set(Insets(4f, 0f, 2f, 2f))
                this.bounds.height.set(40f)
            }
            contentPane.addChild(scrollPane)
            contentPane.addChild(hbox)

            val vbox = VBox().apply {
                Anchor.TopLeft.configure(this)
                this.bounds.height.set(300f)
                this.spacing.set(0f)
            }

            vbox.temporarilyDisableLayouts {
                val (feedbackPane, feedbackCheck) = createCheckboxOption({ Localization.getVar("mainMenu.inputSettings.feedback.bar").use() })
                feedbackCheck.checkedState.set(settings.showInputFeedbackBar.getOrCompute())
                feedbackCheck.onCheckChanged = { newState ->
                    settings.showInputFeedbackBar.set(newState)
                }
                vbox += feedbackPane
                val (skillStarPane, skillStarCheck) = createCheckboxOption({ Localization.getVar("mainMenu.inputSettings.feedback.skillStar").use() })
                skillStarCheck.checkedState.set(settings.showSkillStar.getOrCompute())
                skillStarCheck.onCheckChanged = { newState ->
                    settings.showSkillStar.set(newState)
                }
                vbox += skillStarPane
            }
            vbox.sizeHeightToChildren(100f)
            scrollPane.setContent(vbox)

            hbox.temporarilyDisableLayouts {
                hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                    this.bounds.width.set(100f)
                    this.setOnAction {
                        menuCol.popLastMenu()
                    }
                    this.disabled.bind { pendingKeyboardBinding.use() != null }
                }
            }
        }
    }
}