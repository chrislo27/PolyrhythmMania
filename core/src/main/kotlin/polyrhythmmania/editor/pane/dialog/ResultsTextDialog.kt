package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.engine.input.ResultsText
import polyrhythmmania.engine.input.Score
import polyrhythmmania.screen.results.ResultsPane


class ResultsTextDialog(editorPane: EditorPane) 
    : EditorDialog(editorPane) {

    private val resultsText: Var<ResultsText> = Var(editor.container.resultsText)
    private val testScoreNoMiss: Var<Boolean> = Var(true)
    private val testScoreSkillStar: Var<Boolean> = Var(true)
    private val testScoreValue: FloatVar = FloatVar(0f)
    
    private val resultsPreview: ResultsPreview

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.resultsText.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bindWidthToSelfHeight()
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptClose()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })
        

        val previewHeight = 320f
        val leftVbox = VBox().apply { 
            this.bounds.width.set(previewHeight * 16f / 9)
            this.spacing.set(4f)
        }
        resultsPreview = ResultsPreview().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(previewHeight)
            this.border.set(Insets(2f))
            this.borderStyle.set(SolidBorder(Color.WHITE).apply { 
                this.roundedCorners.set(true)
            })
        }
        leftVbox.temporarilyDisableLayouts { 
            leftVbox += resultsPreview
            leftVbox += HBox().apply { 
                this.spacing.set(10f)
                this.bounds.height.set(32f)
                this += TextLabel(binding = {Localization.getVar("editor.dialog.resultsText.score").use()}).apply { 
                    this.markup.set(editorPane.palette.markup)
                    this.textColor.set(Color.WHITE)
                    this.bounds.width.set(100f)
                    this.padding.set(Insets(0f, 0f, 0f, 2f))
                    this.renderAlign.set(Align.right)
                }
                this += Slider().apply slider@{
                    this.bindWidthToParent(adjust = -(100f + 10))
                    this.minimum.set(0f)
                    this.maximum.set(100f)
                    this.tickUnit.set(1f)
                    this.setValue(0f)
                    testScoreValue.bind { this@slider.value.useF().coerceIn(0f, 100f) }
                }
            }
            leftVbox += CheckBox(binding = { Localization.getVar("editor.dialog.resultsText.noMiss").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.bounds.height.set(32f)
                this.selectedState.set(testScoreNoMiss.getOrCompute())
                this.onCheckChanged = {
                    testScoreNoMiss.set(this.selectedState.getOrCompute())
                }
                this.imageNode.padding.set(Insets(0f, 0f, 0f, 6f))
                this.imageNode.tint.set(Color.WHITE)
                this.textLabel.textColor.set(Color.WHITE)
            }
            leftVbox += CheckBox(binding = { Localization.getVar("editor.dialog.resultsText.skillStar").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.bounds.height.set(32f)
                this.selectedState.set(testScoreSkillStar.getOrCompute())
                this.onCheckChanged = {
                    testScoreSkillStar.set(this.selectedState.getOrCompute())
                }
                this.imageNode.padding.set(Insets(0f, 0f, 0f, 6f))
                this.imageNode.tint.set(Color.WHITE)
                this.textLabel.textColor.set(Color.WHITE)
            }
        }
        contentPane += leftVbox


        val rightVbox = VBox().apply {
            Anchor.TopRight.configure(this)
            this.bindWidthToParent(adjustBinding = {-(previewHeight * 16f / 9 + 10f)})
            this.spacing.set(8f)
        }
        rightVbox.temporarilyDisableLayouts { 
            fun addField(labelText: String, defaultText: String, getter: (ResultsText) -> String?,
                         allowNewlines: Boolean = false,
                         copyFunc: (ResultsText, newText: String) -> ResultsText, ): HBox {
                return HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(8f)
                    this += TextLabel(binding = { Localization.getVar(labelText).use() }).apply {
                        this.markup.set(editorPane.palette.markup)
                        this.bounds.width.set(150f)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(0f, 0f, 0f, 4f))
                    }
                    this += RectElement(Color.BLACK).apply {
                        this.bindWidthToParent(adjust = -160f)
                        this.padding.set(Insets(1f, 1f, 2f, 2f))
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this += TextField(editorPane.palette.rodinDialogFont).apply {
                            this.textColor.set(Color.WHITE)
                            this.canInputNewlines.set(allowNewlines)
                            this.emptyHintText.bind { Localization.getVar(defaultText).use() }
                            this.text.set(getter(resultsText.getOrCompute()) ?: "")
                            this.text.addListener { t ->
                                val newText = t.getOrCompute().takeUnless { it.isEmpty() }
                                if (this.hasFocus.getOrCompute()) {
                                    resultsText.set(copyFunc(resultsText.getOrCompute(), newText ?: Localization.getValue(defaultText)))
                                } else {
                                    if (newText == null) {
                                        resultsText.set(copyFunc(resultsText.getOrCompute(), Localization.getValue(defaultText)))
                                    }
                                }
                            }
                            this.setOnRightClick {
                                text.set("")
                                requestFocus()
                            }
                        }
                    }
                }
            }

            rightVbox += addField("editor.dialog.resultsText.text.title", "play.results.defaultTitle", { it.title }) { t, newText ->
                t.copy(title = newText) 
            }
            rightVbox += addField("editor.dialog.resultsText.text.firstNegative", "play.results.defaultTryAgain.1", { it.firstNegative }, allowNewlines = true) { t, newText ->
                t.copy(firstNegative = newText)
            }
            rightVbox += addField("editor.dialog.resultsText.text.secondNegative", "play.results.defaultTryAgain.3", { it.secondNegative }, allowNewlines = true) { t, newText ->
                t.copy(secondNegative = newText)
            }
            rightVbox += addField("editor.dialog.resultsText.text.ok", "play.results.defaultOK.1", { it.ok }, allowNewlines = true) { t, newText ->
                t.copy(ok = newText)
            }
            rightVbox += addField("editor.dialog.resultsText.text.firstPositive", "play.results.defaultSuperb.1", { it.firstPositive }, allowNewlines = true) { t, newText ->
                t.copy(firstPositive = newText)
            }
            rightVbox += addField("editor.dialog.resultsText.text.secondPositive", "play.results.defaultSuperb.2", { it.secondPositive }, allowNewlines = true) { t, newText ->
                t.copy(secondPositive = newText)
            }
        }
        
        contentPane += rightVbox
    }
    
    init {
        resultsText.addListener {
            editor.container.resultsText = it.getOrCompute()
        }
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
    }
    
    inner class ResultsPreview : UIElement() {

        private val camera: OrthographicCamera = OrthographicCamera().apply { 
            this.setToOrtho(false, 1280f, 720f)
            this.update()
        }
        private val innerSceneRoot: SceneRoot = SceneRoot(camera)
        private val scoreObj: Var<Score> = Var {
            val resultsText = resultsText.use()
            val scoreInt = testScoreValue.useF().toInt()
            val lines: Pair<String, String> = resultsText.generateLinesOfText(scoreInt, false, false)
            val noMiss = testScoreNoMiss.use()
            val skillStar = testScoreSkillStar.use()
            Score(scoreInt, scoreInt.toFloat(), if (noMiss) 8 else 7, 8,
                    skillStar, noMiss,
                    resultsText.title ?: Localization.getValue("play.results.defaultTitle"),
                    lines.first, lines.second
            )
        }
        private val resultsPane: ResultsPane
        
        init {
            this += ImageNode(editor.previewTextureRegion)
        }
        
        init {
            resultsPane = ResultsPane(main, scoreObj.getOrCompute()).apply { 
                this.score.bind { scoreObj.use() }
            }
            innerSceneRoot += resultsPane
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.paddingZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor

            
            val cam = this.camera
            cam.zoom = 1f / 2f
            cam.position.x = 3.5f
            cam.position.y = 1f
            cam.update()

            batch.end()
            val frameBuffer = editor.previewFrameBuffer
            frameBuffer.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            batch.begin()
            
            innerSceneRoot.renderAsRoot(batch)
            
            batch.end()
            frameBuffer.end()
            batch.begin()


            batch.packedColor = lastPackedColor
        }
    }
}