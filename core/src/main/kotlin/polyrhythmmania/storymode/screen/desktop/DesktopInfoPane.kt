package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.binding.ReadOnlyVar
import paintbox.binding.toConstVar
import paintbox.font.Markup
import paintbox.ui.Anchor
import paintbox.ui.ImageIcon
import paintbox.ui.RenderAlign
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.contract.SongInfo
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import polyrhythmmania.ui.PRManiaSkins


class DesktopInfoPane(val desktopUI: DesktopUI) : VBox() {

    private val main: PRManiaGame get() = desktopUI.main
    private val scenario: DesktopScenario get() = desktopUI.scenario

    private fun addRightSidePanel(title: ReadOnlyVar<String>, height: Float): VBox {
        val panel: UIElement = DesktopStyledPane().apply {
            Anchor.Centre.configure(this)
            this.bounds.height.set(height)
            this.padding.set(Insets(7f * UI_SCALE, 4f * UI_SCALE, 5f * UI_SCALE, 5f * UI_SCALE))
        }
        val vbox = VBox().apply {
            this.spacing.set(1f * UI_SCALE)
            this.temporarilyDisableLayouts {
                this += TextLabel(title, font = main.fontMainMenuHeading).apply {
                    this.bounds.height.set(7f * UI_SCALE)
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
            is InboxItem.ContractDoc -> updateForContractDoc(inboxItem)
            else -> {}
        }
    }

    //region ContractDoc

    private fun updateForContractDoc(inboxItem: InboxItem.ContractDoc) {
        val inboxItemStateGetter: () -> InboxItemState = {
            scenario.inboxState.getItemState(inboxItem) ?: InboxItemState.DEFAULT_UNAVAILABLE
        }
        val inboxItemState = inboxItemStateGetter()
        val contract = inboxItem.contract
        val attribution = contract.attribution

        addStartContractPanel(inboxItem, inboxItemStateGetter)
        addContractConditionsPanel(inboxItem)
        addContractScorePanel(inboxItem, inboxItemState)
        if (inboxItem.showSongInfo(inboxItemState) && attribution != null) {
            val songInfo = attribution.song
            if (songInfo != null) {
                addSongInfoPanel(songInfo)
            }
        }
    }

    private fun addStartContractPanel(inboxItem: InboxItem.ContractDoc, inboxItemState: () -> InboxItemState) {
        addRightSidePanel("".toConstVar(), 20f * UI_SCALE).apply {
            this.removeAllChildren()
            this += Button(StoryL10N.getVar("desktop.pane.startContract"), font = main.fontRobotoBold).apply {
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_DARK)
                this.bounds.height.set(8f * UI_SCALE)

                this.setOnAction {
                    desktopUI.controller.playSFX(DesktopController.SFXType.ENTER_LEVEL)
                    desktopUI.controller.playLevel(inboxItem.contract, inboxItem, inboxItemState())
                }
            }
        }
    }

    private fun addContractScorePanel(inboxItem: InboxItem.ContractDoc, inboxItemState: InboxItemState) {
        val contract = inboxItem.contract
        addRightSidePanel(StoryL10N.getVar("desktop.pane.performance"), 40f * UI_SCALE).apply {
            this.temporarilyDisableLayouts {
                val completionData = inboxItemState.stageCompletionData
                if (completionData != null) {
                    this += TextLabel(binding = {
                        if (contract.immediatePass) StoryL10N.getVar("play.scoreCard.pass").use() else "${completionData.score}"
                    }, font = main.fontResultsScore).apply {
                        this.bounds.height.set(11f * UI_SCALE)
                        this.textColor.set(Color.LIGHT_GRAY)
                        this.renderAlign.set(RenderAlign.center)
                        this.setScaleXY(0.5f)
                        this.margin.set(Insets(1f, 1f, 4f, 4f))
                    }

                    this += HBox().apply {
                        this.bounds.height.set(8f * UI_SCALE)
                        this.align.set(HBox.Align.CENTRE)
                        this.margin.set(Insets(1f, 1f, 4f, 4f))
                        this.spacing.set(2f * UI_SCALE)

                        if (completionData.skillStar && !inboxItem.ignoreSkillStar) {
                            this += ImageIcon(TextureRegion(StoryAssets.get<Texture>("desk_ui_icon_skillstar"))).apply {
                                this.bounds.width.set(8f * UI_SCALE)
                            }
                        }
                        if (completionData.noMiss && !inboxItem.ignoreNoMiss) {
                            this += ImageIcon(TextureRegion(StoryAssets.get<Texture>("desk_ui_icon_nomiss"))).apply {
                                this.bounds.width.set(8f * UI_SCALE)
                            }
                        }
                    }
                } else {
                    this += TextLabel(StoryL10N.getVar("desktop.pane.performance.noInfo"), font = main.fontRobotoItalic).apply {
                        this.bounds.height.set(19f * UI_SCALE)
                        this.textColor.set(Color.BLACK)
                        this.renderAlign.set(RenderAlign.center)
                        this.doLineWrapping.set(true)
                        this.margin.set(Insets(1f, 1f, 4f, 4f))
                    }
                }
            }
        }
    }

    private fun addSongInfoPanel(songInfo: SongInfo) {
        val numNewlines = songInfo.songNameAndSource.songNameWithLineBreaks.count { it == '\n' }
        addRightSidePanel(StoryL10N.getVar("desktop.pane.musicInfo"), (36f + (numNewlines * 6)) * UI_SCALE).apply {
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
                        6f * UI_SCALE * (numNewlines + 1)
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
                        this.bounds.height.set(4f * UI_SCALE)
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
                    this.bounds.height.set(4f * UI_SCALE)
                    this.textColor.set(Color.BLACK)
                    this.renderAlign.set(RenderAlign.center)
                    this.setScaleXY(0.65f)
                }
            }
        }
    }

    private fun addContractConditionsPanel(inboxItem: InboxItem.ContractDoc) {
        val contract = inboxItem.contract

        val primaryText = when {
            contract.id == Contracts.ID_BOSS -> StoryL10N.getValue("desktop.pane.conditions.primary.secret")
            contract.immediatePass -> StoryL10N.getValue("desktop.pane.conditions.primary.noMinScore")
            else -> StoryL10N.getValue("desktop.pane.conditions.primary.minScore", contract.minimumScore)
        }
        
        var clearText = primaryText
        val ellipse = StoryL10N.getValue("desktop.pane.conditions.ellipsis")
        val conjunction1 = StoryL10N.getValue("desktop.pane.conditions.conjunction.1")
        val conjunction2 = StoryL10N.getValue("desktop.pane.conditions.conjunction.2")
        val extras: List<String> = contract.extraConditions.sorted().map { c -> c.text.getOrCompute() }
        if (extras.isNotEmpty()) {
            clearText += extras.withIndex().joinToString(separator = "\n", prefix = "\n") { (index, s) ->
                val conjunction = if (index == 0) "" else "${if (index % 2 == 0) conjunction2 else conjunction1} "
                "${ellipse}${conjunction}${s}"
            }
        }

        val numNewlines = clearText.count { c -> c == '\n' }
        val addHeightDueToNl = 5f * UI_SCALE * numNewlines
        addRightSidePanel(StoryL10N.getVar("desktop.pane.conditions"), 30f * UI_SCALE + addHeightDueToNl).apply {
            this += TextLabel(clearText, font = main.fontRobotoItalic).apply {
                this.bounds.height.set(10f * UI_SCALE + addHeightDueToNl)
                this.textColor.set(Color.BLACK)
                this.renderAlign.set(RenderAlign.center)
                this.margin.set(Insets(1f, 1f, 4f, 4f))
            }
        }
    }

    //endregion
}
