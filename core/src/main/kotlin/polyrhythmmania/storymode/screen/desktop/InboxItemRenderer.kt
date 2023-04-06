package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.binding.toConstVar
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.GdxRunnableTransition
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.contract.Requester
import polyrhythmmania.storymode.inbox.IContractDoc
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import kotlin.math.sqrt


class InboxItemRenderer(val main: PRManiaGame, val scenario: DesktopScenario) {
    
    private class Paper(val root: ImageNode, val paperPane: Pane, val envelopePane: Pane)
    
    
    private var desktopUI: DesktopUI? = null

    val monoMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoMono, main.fontRobotoMonoBold, main.fontRobotoMonoItalic, main.fontRobotoMonoBoldItalic)
    val slabMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoSlab, main.fontRobotoSlabBold, null, null)
    val robotoRegularMarkup: Markup = Markup.createWithBoldItalic(main.fontRoboto, main.fontRobotoBold, main.fontRobotoItalic, main.fontRobotoBoldItalic)
    val robotoBoldMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoBold, main.fontRobotoBold, main.fontRobotoBoldItalic, main.fontRobotoItalic)
    val robotoItalicMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoItalic, main.fontRobotoBoldItalic, main.fontRobotoItalic, main.fontRobotoBold)
    val robotoCondensedMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoCondensed, main.fontRobotoCondensedBold, main.fontRobotoCondensedItalic, main.fontRobotoCondensedBoldItalic)
    val robotoMonoMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoMono, main.fontRobotoMonoBold, main.fontRobotoMonoItalic, main.fontRobotoMonoBoldItalic)
    val openSansMarkup: Markup = Markup.createWithBoldItalic(main.fontOpenSans, main.fontOpenSansBold, main.fontOpenSansItalic, main.fontOpenSansBoldItalic)

    
    constructor(desktopUI: DesktopUI) : this(desktopUI.main, desktopUI.scenario) {
        this.desktopUI = desktopUI
    }


    private fun createPaperTemplate(textureID: ReadOnlyVar<String>): Paper {
        val root = ImageNode(binding = { TextureRegion(StoryAssets.get<Texture>(textureID.use())) }, ImageRenderingMode.FULL).apply {
            this.bounds.width.set(112f * UI_SCALE)
            this.bounds.height.set(150f * UI_SCALE)
        }
        val paperPane = Pane().apply {// Paper part
            this.bounds.height.set(102f * UI_SCALE)
            this.margin.set(Insets((2f + 4f) * UI_SCALE, 0f * UI_SCALE, (4f + 4f) * UI_SCALE, (4f + 4f) * UI_SCALE))
        }
        root += paperPane
        val envelopePane = Pane().apply {// Envelope part
            this.margin.set(Insets(0f * UI_SCALE, 6f * UI_SCALE))
            this.bounds.height.set(48f * UI_SCALE)
            this.bounds.y.set(102f * UI_SCALE)
        }
        root += envelopePane

        return Paper(root, paperPane, envelopePane)
    }

    private fun createPaperTemplate(textureID: String = "desk_contract_full"): Paper =
            createPaperTemplate(textureID.toConstVar())
    
    fun createInboxItemUI(item: InboxItem): UIElement {
        return when (item) {
            is InboxItem.Memo -> {
                val paper = createPaperTemplate("desk_contract_paper")
                paper.paperPane += VBox().apply {
                    this.spacing.set(1f * UI_SCALE)
                    this.temporarilyDisableLayouts {
                        this += TextLabel(StoryL10N.getVar("inboxItem.memo.heading"), font = main.fontMainMenuHeading).apply {
                            this.bounds.height.set(9f * UI_SCALE)
                            this.textColor.set(Color.BLACK)
                            this.renderAlign.set(Align.topLeft)
                            this.padding.set(Insets(0f, 2f * UI_SCALE, 0f, 2f * UI_SCALE))
                        }
                        val fields: List<Pair<String, ReadOnlyVar<String>>> = listOfNotNull(
                                if (item.hasToField) ("to" to item.to) else null,
                                "from" to item.from,
                                "subject" to item.subject
                        )
                        this += ColumnarPane(fields.size, true).apply {
                            this.bounds.height.set((7f * UI_SCALE) * fields.size)

                            fun addField(index: Int, fieldName: String, valueField: String, valueMarkup: Markup? = null) {
                                this[index] += Pane().apply {
                                    this.margin.set(Insets(0.5f * UI_SCALE, 0f))
                                    this += TextLabel(StoryL10N.getVar("inboxItem.memo.${fieldName}"), font = main.fontRobotoBold).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.left)
                                        this.padding.set(Insets(2f, 2f, 0f, 10f))
                                        this.bounds.width.set(22.5f * UI_SCALE)
                                    }
                                    this += TextLabel(valueField, font = main.fontRoboto).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.left)
                                        this.padding.set(Insets(2f, 2f, 4f, 0f))
                                        this.bounds.x.set(90f)
                                        this.bindWidthToParent(adjust = -(22.5f * UI_SCALE))
                                        this.markup.set(valueMarkup ?: robotoRegularMarkup)
//                                        if (valueMarkup != null) {
//                                            this.markup.set(valueMarkup)
//                                        }
                                    }
                                }
                            }

                            fields.forEachIndexed { i, (key, value) ->
                                addField(i, key, value.getOrCompute())
                            }
                        }
                        this += RectElement(Color.BLACK).apply {
                            this.bounds.height.set(2f)
                        }

                        this += TextLabel(item.desc.getOrCompute()).apply {
                            this.markup.set(openSansMarkup)
                            this.textColor.set(Color.BLACK)
                            this.renderAlign.set(Align.topLeft)
                            this.padding.set(Insets(3f * UI_SCALE, 0f, 0f, 0f))
                            this.bounds.height.set(100f * UI_SCALE)
                            this.doLineWrapping.set(true)
                        }
                    }
                }

                paper.root
            }
            is InboxItem.InfoMaterial -> {
                val paper = createPaperTemplate("desk_contract_paper")
                paper.paperPane += VBox().apply {
                    this.spacing.set(1f * UI_SCALE)
                    this.temporarilyDisableLayouts {
                        this += TextLabel(StoryL10N.getVar("inboxItem.infoMaterial.heading"), font = main.fontMainMenuHeading).apply {
                            this.bounds.height.set(9f * UI_SCALE)
                            this.textColor.set(Color.BLACK)
                            this.renderAlign.set(Align.top)
                            this.padding.set(Insets(0f, 2f * UI_SCALE, 0f, 0f))
                        }
                        val fields: List<Pair<String, ReadOnlyVar<String>>> = listOfNotNull(
                                "topic" to item.topic,
                                "audience" to item.audience,
                        )
                        this += VBox().apply {
                            val rowHeight = 7f * UI_SCALE
                            this.bounds.height.set(rowHeight * fields.size)
                            this.temporarilyDisableLayouts {
                                fields.forEachIndexed { i, (key, value) ->
                                    this += Pane().apply {
                                        this.bounds.height.set(rowHeight)
                                        this.margin.set(Insets(0.5f * UI_SCALE, 0f))
                                        this += TextLabel({
                                            "[b]${StoryL10N.getVar("inboxItem.infoMaterial.${key}").use()}[] ${value.use()}"
                                        }, font = main.fontRobotoBold).apply {
                                            this.markup.set(slabMarkup)
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.center)
                                            this.padding.set(Insets(2f, 2f, 0f, 10f))
                                        }
                                    }
                                }
                            }
                        }
                        this += RectElement(Color.BLACK).apply {
                            this.bounds.height.set(2f)
                        }

                        this += TextLabel(item.desc.getOrCompute()).apply {
                            this.markup.set(robotoCondensedMarkup)
                            this.textColor.set(Color.BLACK)
                            this.renderAlign.set(Align.topLeft)
                            this.padding.set(Insets(3f * UI_SCALE, 0f, 0f, 0f))
                            this.bounds.height.set(100f * UI_SCALE)
                            this.doLineWrapping.set(true)
                        }
                    }
                }

                paper.root
            }
            is InboxItem.ContractDoc, is InboxItem.RobotTest, is InboxItem.PlaceholderContract -> {
                item as IContractDoc
                val subtype: IContractDoc.ContractSubtype = item.subtype
                val paper = createPaperTemplate(when (subtype) {
                    IContractDoc.ContractSubtype.NORMAL -> "desk_contract_full"
                    IContractDoc.ContractSubtype.TRAINING -> "desk_contract_paper"
                    IContractDoc.ContractSubtype.ROBOT_TEST -> "desk_contract_paper"
                })

                val headingText: ReadOnlyVar<String> = when (item) {
                    is InboxItem.ContractDoc -> item.headingText
                    is InboxItem.PlaceholderContract -> item.headingText
                    is InboxItem.RobotTest -> item.headingText
                    else -> "<missing heading text>".toConstVar()
                }

                paper.paperPane += VBox().apply {
                    this.spacing.set(1f * UI_SCALE)
                    this.temporarilyDisableLayouts {
                        val useLongCompanyName = item.hasLongCompanyName
                        this += Pane().apply {
                            this.bounds.height.set(13f * UI_SCALE)
                            this.margin.set(Insets(0f, 2.5f * UI_SCALE, 0f, 0f))

                            this += TextLabel(headingText, font = main.fontMainMenuHeading).apply {
                                this.bindWidthToParent(multiplier = 0.5f, adjust = -2f * UI_SCALE)
                                this.padding.set(Insets(0f, 0f, 0f, 1f * UI_SCALE))
                                this.textColor.set(Color.BLACK)
                                if (useLongCompanyName) {
                                    this.renderAlign.set(Align.topLeft)
                                    this.setScaleXY(0.6f)
                                } else {
                                    this.renderAlign.set(Align.left)
                                }
                            }
                            this += Pane().apply {
                                Anchor.TopRight.configure(this)
                                this.bindWidthToParent(multiplier = 0.5f, adjust = -2f * UI_SCALE)

                                this += TextLabel(item.name, font = main.fontRobotoMonoBold).apply {
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topRight)
                                }
                                if (!useLongCompanyName) { // Right-aligned company name
                                    this += TextLabel(item.requester.localizedName, font = main.fontRobotoCondensedItalic).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.bottomRight)
                                    }
                                }
                            }

                            if (useLongCompanyName) {
                                // Centred long company name
                                this += TextLabel(item.requester.localizedName, font = main.fontRobotoCondensedItalic).apply {
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.bottom)
                                }
                            }
                        }
                        this += RectElement(Color.BLACK).apply {
                            this.bounds.height.set(2f)
                        }
                        this += TextLabel(item.tagline.getOrCompute(), font = main.fontLexend).apply {
                            this.bounds.height.set(10f * UI_SCALE)
                            this.textColor.set(Color.BLACK)
                            this.renderAlign.set(Align.center)
                            this.padding.set(Insets(1f * UI_SCALE, 1f * UI_SCALE, 1f * UI_SCALE, 0f))
                        }
                        this += RectElement(Color.BLACK).apply {
                            this.bounds.height.set(2f)
                        }

                        this += TextLabel(item.desc.getOrCompute()).apply {
                            this.markup.set(openSansMarkup)
                            this.textColor.set(Color.BLACK)
                            this.renderAlign.set(Align.topLeft)
                            this.padding.set(Insets(3f * UI_SCALE, 1f * UI_SCALE, 0f, 0f))
                            this.bounds.height.set(400f)
                            this.doLineWrapping.set(true)
                            this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
                            
                            when (item.requester) {
                                Requester.ALIENS -> {
                                    this.markup.set(robotoMonoMarkup)
                                    this.renderAlign.set(Align.center)
                                    this.textAlign.set(TextAlign.LEFT)
                                }
                                Requester.GAME_DEV -> {
                                    this.renderAlign.set(Align.center)
                                    this.textAlign.set(TextAlign.CENTRE)
                                }
                                Requester.POLYBUILD_ROBOT_TEST -> {
                                    this.markup.set(robotoMonoMarkup)
                                }
                            }
                        }
                    }
                }

                paper.root
            }
            is InboxItem.EmploymentContract -> {
                val itemStateVar = scenario.inboxState.itemStateVar(item.id)
                val paper = createPaperTemplate(Var.bind { 
                    if (itemStateVar.use()?.completion == InboxItemCompletion.COMPLETED) "desk_contract_employment_signed" else "desk_contract_employment_blank"
                })
                paper.paperPane += VBox().apply {
                    this.spacing.set(1f * UI_SCALE)
                    this.temporarilyDisableLayouts { 
                        this += RectElement(Color.WHITE).apply {
                            this.bounds.height.set(12f * UI_SCALE)
                            this.padding.set(Insets(0f, 2f * UI_SCALE, 0f, 0f))

                            this += TextLabel(StoryL10N.getVar("inboxItem.employmentContract.heading"), font = main.fontMainMenuHeading).apply {
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.bottom)
                                this.setScaleXY(0.8f)
                            }
                        }
                    }
                }
                paper.envelopePane += Pane().apply {
                    this.bounds.x.set(0f * UI_SCALE)
                    this.bounds.y.set(14f * UI_SCALE)
                    this.bounds.width.set(100f * UI_SCALE)
                    this.bounds.height.set(13f * UI_SCALE)

                    this.visible.bind {
                        itemStateVar.use()?.completion != InboxItemCompletion.COMPLETED
                    }

                    val isHovered = BooleanVar(false)
                    val color = Var {
                        if (isHovered.use()) {
                            Color.YELLOW.cpy().lerp(Color.ORANGE, 0.75f)
                        } else {
                            Color.YELLOW.cpy().lerp(Color.ORANGE, 0.25f).apply {
                                this.a = 0.9f
                            }
                        }
                    }.asReadOnlyVar()
                    
                    this += ActionablePane().apply {
                        this.setOnHoverStart {
                            isHovered.set(true)
                        }
                        this.setOnHoverEnd {
                            isHovered.set(false)
                        }
                        this.setOnAction {
                            scenario.inboxState.putItemState(item, InboxItemState.BRAND_NEW.copy(completion = InboxItemCompletion.COMPLETED))

                            desktopUI?.controller?.playSFX(DesktopController.SFXType.CONTRACT_SIGNATURE)

                            val desktopUI = desktopUI
                            if (desktopUI != null) {
                                val delaySec = 1.25f
                                Gdx.app.postRunnable(GdxRunnableTransition(0f, 1f, delaySec) { _, progress ->
                                    if (progress == 1f) {
                                        desktopUI.updateAndShowNewlyAvailableInboxItems()
                                    }
                                })
                            } else {
                                scenario.updateProgression()
                                scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())
                            }
                        }
                        
                        this += RectElement(Color.CLEAR).apply {
                            this.border.set(Insets(1f * UI_SCALE))
                            this.borderStyle.set(SolidBorder().apply {
                                this.color.bind { color.use() }
                            })
                        }
                        this += TextLabel(StoryL10N.getVar("inboxItem.employmentContract.clickToSign")).apply {
                            Anchor.TopCentre.configure(this, offsetY = { bounds.height.use() })
                            this.visible.bind { isHovered.use() }
                            this.markup.set(robotoItalicMarkup)
                            this.textColor.set(Color.BLACK)
                            this.backgroundColor.bind { color.use() }
                            this.bgPadding.set(Insets(2f * UI_SCALE))
                            this.renderAlign.set(RenderAlign.top)
                            this.renderBackground.set(true)
                            this.setScaleXY(0.75f)
                        }
                    }
                }

                paper.root
            }
            is InboxItem.Debug -> {
                RectElement(Color.WHITE).apply {
                    this.doClipping.set(true)
                    this.border.set(Insets(2f))
                    this.borderStyle.set(SolidBorder(Color.YELLOW))
                    this.bounds.height.set(600f)
                    this.bindWidthToSelfHeight(multiplier = 1f / sqrt(2f))

                    this.padding.set(Insets(16f))

                    this += VBox().apply {
                        this.spacing.set(6f)
                        this.temporarilyDisableLayouts {
                            this += TextLabel("DEBUG ITEM", font = main.fontMainMenuHeading).apply {
                                this.bounds.height.set(40f)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.top)
                                this.padding.set(Insets(0f, 8f, 0f, 8f))
                            }
                            this += RectElement(Color.BLACK).apply {
                                this.bounds.height.set(2f)
                            }
                            this += ColumnarPane(3, true).apply {
                                this.bounds.height.set(32f * this.numRealColumns)

                                fun addField(index: Int, key: String, valueField: ReadOnlyVar<String>,
                                             valueMarkup: Markup? = null) {
                                    this[index] += Pane().apply {
                                        this.margin.set(Insets(2f))
                                        this += TextLabel(key, font = main.fontRobotoBold).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 0f, 10f))
                                            this.bounds.width.set(90f)
                                        }
                                        this += TextLabel(valueField, font = main.fontRoboto).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 4f, 0f))
                                            this.bounds.x.set(90f)
                                            this.bindWidthToParent(adjust = -90f)
                                            if (valueMarkup != null) {
                                                this.markup.set(valueMarkup)
                                            }
                                        }
                                    }
                                }
                                fun addField(index: Int, key: String, valueField: String,
                                             valueMarkup: Markup? = null) {
                                    addField(index, key, ReadOnlyVar.const(valueField), valueMarkup)
                                }

                                addField(0, "Type", "${item.subtype}")
                                addField(1, "ID", item.id)
                                val itemStateVar = scenario.inboxState.itemStateVar(item.id)
                                addField(2, "InboxItemCompletion", Var.bind {
                                    (itemStateVar.use()?.completion ?: InboxItemCompletion.UNAVAILABLE).toString()
                                })
                            }
                            this += RectElement(Color.BLACK).apply {
                                this.bounds.height.set(2f)
                            }

                            this += TextLabel(item.description).apply {
                                this.markup.set(slabMarkup)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.topLeft)
                                this.padding.set(Insets(8f, 0f, 0f, 0f))
                                this.bounds.height.set(150f)
                                this.doLineWrapping.set(true)
                            }

                            when (item.subtype) {
                                InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER -> {
                                    this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                        this.bounds.height.set(48f)
                                        this.padding.set(Insets(8f))
                                        this += Button("Mark Available (w/ flashing)").apply {
                                            this.bindWidthToParent(multiplier = 0.5f, adjust = -2f)
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.BRAND_NEW.copy(completion = InboxItemCompletion.AVAILABLE, newIndicator = true))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())
                                            }
                                        }
                                        this += Button("Mark Available (w/o flashing)").apply {
                                            this.bindWidthToParent(multiplier = 0.5f, adjust = -2f)
                                            Anchor.TopRight.configure(this)
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.BRAND_NEW.copy(completion = InboxItemCompletion.AVAILABLE, newIndicator = false))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())
                                            }
                                        }
                                    }
                                    this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                        this.bounds.height.set(48f)
                                        this.padding.set(Insets(8f))
                                        this += Button("Mark Skipped").apply {
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.BRAND_NEW.copy(completion = InboxItemCompletion.SKIPPED))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())
                                            }
                                        }
                                    }
                                    this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                        this.bounds.height.set(48f)
                                        this.padding.set(Insets(8f))
                                        this += Button("Mark Completed").apply {
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.BRAND_NEW.copy(completion = InboxItemCompletion.COMPLETED))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
}