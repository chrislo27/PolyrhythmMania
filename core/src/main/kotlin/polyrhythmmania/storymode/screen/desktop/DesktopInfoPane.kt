package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.binding.asReadOnlyVar
import paintbox.font.Markup
import paintbox.ui.Anchor
import paintbox.ui.RenderAlign
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.VBox
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.ui.PRManiaSkins


class DesktopInfoPane(val desktopUI: DesktopUI) : VBox() {

    private val main: PRManiaGame get() = desktopUI.main
    private val scenario: DesktopScenario = desktopUI.scenario
    private val currentInboxItem: ReadOnlyVar<InboxItem?> = desktopUI.currentInboxItem
    
    private fun addRightSidePanel(title: ReadOnlyVar<String>, height: Float): VBox {
        val panel: UIElement = DesktopStyledPane().apply {
            Anchor.Centre.configure(this)
            this.bounds.height.set(height)
            this.padding.set(Insets(7f * DesktopUI.UI_SCALE, 4f * DesktopUI.UI_SCALE, 5f * DesktopUI.UI_SCALE, 5f * DesktopUI.UI_SCALE))
        }
        val vbox = VBox().apply {
            this.spacing.set(1f * DesktopUI.UI_SCALE)
            this.temporarilyDisableLayouts {
                this += TextLabel(title, font = main.fontMainMenuHeading).apply {
                    this.bounds.height.set(7f * DesktopUI.UI_SCALE)
                    this.textColor.set(Color.BLACK)
                    this.renderAlign.set(RenderAlign.center)
                    this.margin.set(Insets(1f, 1f, 4f, 4f))
                    this.setScaleXY(0.6f)
                }
            }
        }
        panel += vbox
        
        this += panel
        
        return vbox
    }
    
    fun updateForInboxItem(inboxItem: InboxItem) {
        when (inboxItem) {
            is InboxItem.ContractDoc ->  {
                val inboxItemState = scenario.inboxState.getItemState(inboxItem) ?: InboxItemState.DEFAULT_UNAVAILABLE
                val contract = inboxItem.contract
                val attribution = contract.attribution

                addRightSidePanel("".asReadOnlyVar(), 20f * DesktopUI.UI_SCALE).apply {
                    this.removeAllChildren()
                    this += Button(StoryL10N.getVar("desktop.pane.startContract"), font = main.fontRobotoBold).apply {
                        this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_DARK)
                        this.bounds.height.set(8f * DesktopUI.UI_SCALE)

                        this.setOnAction {
                            desktopUI.controller.playSFX(DesktopController.SFXType.ENTER_LEVEL)
                            desktopUI.controller.playLevel(inboxItem.contract, inboxItem, inboxItemState)
                        }
                    }
                }
                addRightSidePanel(StoryL10N.getVar("desktop.pane.performance"), 40f * DesktopUI.UI_SCALE).apply {
                    this.temporarilyDisableLayouts {
                        val completionData = inboxItemState.stageCompletionData // TODO remove me: ?: StageCompletionData(LocalDateTime.now(), LocalDateTime.now(), 70, true, true)
                        if (completionData != null) {
                            this += TextLabel(binding = {
                                if (contract.immediatePass) StoryL10N.getVar("play.scoreCard.pass").use() else "${completionData.score}"
                            }, font = main.fontResultsScore).apply {
                                this.bounds.height.set(11f * DesktopUI.UI_SCALE)
                                this.textColor.set(Color.LIGHT_GRAY)
                                this.renderAlign.set(RenderAlign.center)
                                this.setScaleXY(0.5f)
                                this.margin.set(Insets(1f, 1f, 4f, 4f))
                            }
                            val tmpListFeatures = listOfNotNull(if (completionData.skillStar) "Skill Star!" else null, if (completionData.noMiss) "No Miss!" else null)
                            this += TextLabel("[TMP!] " + tmpListFeatures.joinToString(separator = " "), font = main.fontRoboto).apply {
                                this.bounds.height.set(8f * DesktopUI.UI_SCALE)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(RenderAlign.center)
                                this.margin.set(Insets(1f, 1f, 4f, 4f))
                            }
                        } else {
                            this += TextLabel(StoryL10N.getVar("desktop.pane.performance.noInfo"), font = main.fontRobotoItalic).apply {
                                this.bounds.height.set(16f * DesktopUI.UI_SCALE)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(RenderAlign.center)
                                this.doLineWrapping.set(true)
                                this.margin.set(Insets(1f, 1f, 4f, 4f))
                            }
                        }
                    }
                }

                if (inboxItemState.playedBefore && attribution != null) {
                    val songInfo = attribution.song
                    if (songInfo != null) {
                        val numNewlines = songInfo.songNameAndSource.songNameWithLineBreaks.count { it == '\n' }
                        addRightSidePanel(StoryL10N.getVar("desktop.pane.musicInfo"), (36f + (numNewlines * 6)) * DesktopUI.UI_SCALE).apply {
                            this.temporarilyDisableLayouts {
                                val additionalMappings = mapOf("rodin" to main.fontMainMenuRodin)
                                val markupNormal = Markup.createWithBoldItalic(main.fontRoboto, main.fontRobotoBold,
                                        main.fontRobotoItalic, main.fontRobotoBoldItalic,
                                        additionalMappings = additionalMappings, lenientMode = false)
                                val markupCondensed = Markup.createWithBoldItalic(main.fontRobotoCondensed, main.fontRobotoCondensedBold,
                                        main.fontRobotoCondensedItalic, main.fontRobotoCondensedBoldItalic,
                                        additionalMappings = additionalMappings, lenientMode = false)
                                fun parseNonlatin(builder: Markup.Builder, text: String) {
                                    if (text.isEmpty()) return

                                    fun Char.isLatin() = this in 0.toChar()..127.toChar()

                                    var currentlyLatin = text[0].isLatin()
                                    var current = ""

                                    fun startTag() {
                                        builder.startTag()
                                        if (!currentlyLatin) {
                                            builder.font("rodin").bold(false)
                                        }
                                    }
                                    fun endTag() = builder.text(current).endTag()

                                    startTag()
                                    for (c in text) {
                                        val cLatin = c.isLatin()
                                        if (cLatin != currentlyLatin) {
                                            endTag()
                                            currentlyLatin = cLatin
                                            current = "$c"
                                            startTag()
                                        } else {
                                            current += c
                                        }
                                    }
                                    endTag()
                                }

                                val primarySourceMaterial = songInfo.songNameAndSource.songSourceMaterial
                                this += TextLabel(songInfo.songNameAndSource.songNameWithLineBreaks, font = main.fontRobotoBold).apply {
                                    this.markup.set(markupNormal)
                                    this.bounds.height.bind {
                                        6f * DesktopUI.UI_SCALE * (numNewlines + 1)
                                    }
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(RenderAlign.center)
                                    this.internalTextBlock.bind {
                                        val builder = markup.use()!!.Builder()

                                        // Song name
                                        builder.startTag().bold()
                                        parseNonlatin(builder, text.use())
                                        builder.endTag()

                                        builder.build()
                                    }
                                }
                                if (primarySourceMaterial != null) {
                                    this += TextLabel("", font = main.fontRobotoBold).apply {
                                        this.markup.set(markupCondensed)
                                        this.bounds.height.set(4f * DesktopUI.UI_SCALE)
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(RenderAlign.center)
                                        this.internalTextBlock.bind {
                                            val builder = markup.use()!!.Builder()

                                            // Song source (game)
                                            // Make sure to switch to Rodin for non-latin text
                                            builder.scale(0.75f).startTag()
                                            parseNonlatin(builder, primarySourceMaterial)
                                            builder.endTag()

                                            builder.build()
                                        }
                                    }
                                }
                                this += TextLabel(songInfo.songArtist, font = main.fontMainMenuRodin).apply {
                                    this.bounds.height.set(4f * DesktopUI.UI_SCALE)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(RenderAlign.center)
                                    this.setScaleXY(0.65f)
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}