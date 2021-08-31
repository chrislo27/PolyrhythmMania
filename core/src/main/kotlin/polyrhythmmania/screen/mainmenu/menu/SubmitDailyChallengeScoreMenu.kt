package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextField
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.discordrpc.DiscordHelper
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeScore
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeUtils
import polyrhythmmania.ui.PRManiaSkins
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


class SubmitDailyChallengeScoreMenu(menuCol: MenuCollection,
                                    val date: LocalDate, val nonce: UUID?, val score: DailyChallengeScore)
    : StandardMenu(menuCol) {


    private val didSubmit: Var<Boolean> = Var(false)
    private val canClickSubmit: Var<Boolean> = Var(false)
    
    init {
        this.setSize(MMMenu.WIDTH_MID, adjust = 32f)
        this.titleText.bind { Localization.getVar("mainMenu.submitDailyChallenge.title").use() }
        this.contentPane.bounds.height.set(315f)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(10f)
            this.vBar.blockIncrement.set(40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)


        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        
        val suggestedName = DiscordHelper.currentUser?.username ?: ""
        val nameText: Var<String> = Var(suggestedName.takeUnless { it.any { c -> c !in DailyChallengeUtils.allowedNameChars } } ?: "")
        val hideCountry: Var<Boolean> = Var(false)
        
        vbox.temporarilyDisableLayouts {
            canClickSubmit.bind { 
                val n = nameText.use()
                n.isNotBlank()
            }

            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 0f, 0f))
                }
            }
            
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.submitDailyChallenge.disclaimer").use() }).apply {
                this.markup.set(this@SubmitDailyChallengeScoreMenu.markup)
                this.bounds.height.set(64f)
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.setScaleXY(0.8f)
            }
            vbox += separator()

            vbox += TextLabel(binding = { Localization.getVar("mainMenu.submitDailyChallenge.score", Var {
                listOf(score.score, score.date.format(DateTimeFormatter.ISO_DATE))
            }).use() }).apply {
                this.markup.set(this@SubmitDailyChallengeScoreMenu.markup)
                this.bounds.height.set(40f)
                this.renderAlign.set(Align.left)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
            }
            
            vbox += separator()
            
            vbox += HBox().apply {
                this.spacing.set(8f)
                this.bounds.height.set(48f)
                this.margin.set(Insets(4f, 4f, 0f, 0f))
                this += TextLabel(binding = { Localization.getVar("mainMenu.submitDailyChallenge.name").use()}, font).apply {
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(100f)
                }
                val textField = TextField(font = font).apply {
                    this.characterLimit.set(24)
                    this.textColor.set(Color(1f, 1f, 1f, 1f))
                    this.text.set(nameText.getOrCompute())
                    this.inputFilter.set { it in DailyChallengeUtils.allowedNameChars }
                    this.setOnRightClick {
                        text.set("")
                        requestFocus()
                    }
                    this.text.addListener { t ->
                        if (hasFocus.getOrCompute()) {
                            nameText.set(t.getOrCompute())
                        }
                    }
                }
                this += RectElement(Color.BLACK).apply {
                    this.bounds.width.set(250f)
                    this.border.set(Insets(2f))
                    this.borderStyle.set(SolidBorder(Color.WHITE))
                    this.padding.set(Insets(4f))
                    this += textField
                }

                this += createSmallButton { Localization.getVar("mainMenu.play.endless.settings.clear").use() }.apply {
                    this.bounds.width.set(80f)
                    this.setOnAction {
                        textField.requestUnfocus()
                        nameText.set("")
                        textField.text.set("")
                    }
                }
            }

            vbox += separator()

            val (hideCountryPane, hideCountryCheck) = createCheckboxOption({ Localization.getVar("mainMenu.submitDailyChallenge.hideCountry").use() })
            hideCountryCheck.onCheckChanged = { newState ->
                hideCountry.set(newState)
            }
            vbox += hideCountryPane
        }

        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.submitDailyChallenge.backToMainMenu").use() }).apply {
                this.bounds.width.set(190f)
                this.setOnAction {
                    removeSelfFromMenuCol(true)
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.submitDailyChallenge.submitScore").use() }).apply {
                this.bounds.width.set(190f)
                this.disabled.bind { !canClickSubmit.use() || didSubmit.use() }
                this.setOnAction {
                    val name = nameText.getOrCompute()
                    if (name.isNotBlank() && canClickSubmit.getOrCompute() && !didSubmit.getOrCompute()) {
                        submitScore(name, hideCountry.getOrCompute())
                    }
                }
            }
            hbox += TextLabel(binding = { Localization.getVar("mainMenu.submitDailyChallenge.submitted").use() }).apply {
                this.bounds.width.set(120f)
                this.markup.set(this@SubmitDailyChallengeScoreMenu.markup)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.visible.bind { didSubmit.use() }
                this.renderAlign.set(Align.left)
                this.setScaleXY(0.9f)
            }
        }
    }

    fun removeSelfFromMenuCol(playSound: Boolean) {
        menuCol.popLastMenu(playSound = playSound)
        menuCol.removeMenu(this)
    }
    
    private fun submitScore(name: String, noCountry: Boolean) {
        didSubmit.set(true)
        if (nonce != null) {
            DailyChallengeUtils.submitHighScore(date, score.score, name, nonce, noCountry)
        }
    }
}