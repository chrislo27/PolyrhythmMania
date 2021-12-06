package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.binding.BooleanVar
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.ExternalResource
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.util.TempFileUtils
import polyrhythmmania.util.TimeUtils
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs


class MusicDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    companion object {
        const val WAVEFORM_HEIGHT: Int = 48
        
        val SUPPORTED_MUSIC_EXTENSIONS: List<String> = listOf("*.ogg", "*.mp3", "*.wav")
    }

    enum class Substate {
        NO_MUSIC,
        FILE_DIALOG_OPEN,
        LOADING,
        LOAD_ERROR,
        HAS_MUSIC,
    }

    enum class MarkerType {
        FIRST_BEAT,
        LOOP_START,
        LOOP_END,
    }

    val substate: Var<Substate> = Var(Substate.NO_MUSIC)
    val loadingProgress: LoadingIndicator = LoadingIndicator()
    val window: Window = Window()
    val currentMarker: Var<MarkerType> = Var(MarkerType.FIRST_BEAT)
    val currentMusicPosition: FloatVar = FloatVar(-1f)

    val noMusicPane: Pane
    val musicSettingsPane: Pane
    val fileDialogOpenPane: Pane
    val loadingPane: Pane
    val errorPane: Pane

    val errorLabel: TextLabel

    val selectMusicBottomPane: Pane
    val loadMusicErrorBottomPane: Pane

    val selectMusicButton: Button
    val removeMusicButton: Button
    
    val loopingCheckbox: CheckBox
    val rateSlider: Slider

    val markerPlaybackStart: Color = PRManiaColors.PLAYBACK_START.cpy()
    val markerFirstBeat: Color = PRManiaColors.MARKER_FIRST_BEAT.cpy()
    val markerLoopStart: Color = PRManiaColors.MARKER_LOOP_START.cpy()
    val markerLoopEnd: Color = PRManiaColors.MARKER_LOOP_END.cpy()

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.music.title").use() }

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
            this.disabled.bind {
                val ss = substate.use()
                !(ss == Substate.NO_MUSIC || ss == Substate.HAS_MUSIC)
            }
        })

        noMusicPane = Pane().apply {
            this.visible.bind { substate.use() == Substate.NO_MUSIC }
        }
        contentPane.addChild(noMusicPane)
        musicSettingsPane = Pane().apply {
            this.visible.bind { substate.use() == Substate.HAS_MUSIC }
        }
        contentPane.addChild(musicSettingsPane)
        loadingPane = Pane().apply {
            this.visible.bind { substate.use() == Substate.LOADING }
        }
        contentPane.addChild(loadingPane)
        errorPane = Pane().apply {
            this.visible.bind { substate.use() == Substate.LOAD_ERROR }
        }
        contentPane.addChild(errorPane)
        fileDialogOpenPane = Pane().apply {
            this.visible.bind { substate.use() == Substate.FILE_DIALOG_OPEN }
        }
        contentPane.addChild(fileDialogOpenPane)

        // File dialog open
        fileDialogOpenPane.addChild(TextLabel(binding = { Localization.getVar("common.closeFileChooser").use() }).apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        })
        // No music
        noMusicPane.addChild(TextLabel(binding = { Localization.getVar("editor.dialog.music.none").use() }).apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        })
        // Music loading
        loadingPane.addChild(TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
            val localizationDecoding = Localization.getVar("editor.dialog.music.loading.decoding", Var {
                val load = loadingProgress
                listOf(load.bytesSoFar.use() / 1024)
            })
            val localizationWaveform = Localization.getVar("editor.dialog.music.loading.waveform", Var {
                val load = loadingProgress
                listOf(load.bytesSoFar.use() / 1024, convertMsToTimestamp(load.durationMs.use()))
            })
            this.text.bind {
                val load = loadingProgress
                if (load.generatingWaveform.use()) {
                    localizationWaveform.use()
                } else {
                    localizationDecoding.use()
                }
            }
        })
        // Music load error
        errorLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        }
        errorPane.addChild(errorLabel)

        // Bottom pane buttons
        val selectMusicButtomPane = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bindWidthToParent { -1 * bounds.height.use() * 3 }
            this.visible.bind {
                val ss = substate.use()
                ss == Substate.HAS_MUSIC || ss == Substate.NO_MUSIC
            }
        }
        this.selectMusicBottomPane = selectMusicButtomPane
        bottomPane.addChild(selectMusicButtomPane)
        selectMusicButton = Button(binding = { Localization.getVar("editor.dialog.music.loadMusicButton").use() }, font = editorPane.palette.musicDialogFont).apply {
            this.bounds.width.set(400f)
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptSelectMusic(null)
            }
        }
        selectMusicButtomPane.addChild(selectMusicButton)
        removeMusicButton = Button(binding = { Localization.getVar("editor.dialog.music.removeMusicButton").use() }, font = editorPane.palette.musicDialogFont).apply {
            this.bounds.width.set(275f)
            this.applyDialogStyleBottom()
            this.disabled.bind { substate.use() != Substate.HAS_MUSIC }
            this.setOnAction {
                substate.set(Substate.NO_MUSIC)
                Gdx.app.postRunnable {
                    editor.musicData.removeMusic()
                }
            }
        }
        selectMusicButtomPane.addChild(removeMusicButton)

        // Bottom pane buttons
        this.loadMusicErrorBottomPane = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bindWidthToParent { -1 * bounds.height.use() * 3 }
            this.bindWidthToParent { -200f }
            this.visible.bind {
                val ss = substate.use()
                ss == Substate.LOAD_ERROR
            }
        }
        bottomPane.addChild(this.loadMusicErrorBottomPane)
        this.loadMusicErrorBottomPane.addChild(Button(binding = { Localization.getVar("common.ok").use() }, font = editorPane.palette.musicDialogFont).apply {
            this.bounds.width.set(250f)
            this.applyDialogStyleBottom()
            this.setOnAction {
                substate.set(Substate.NO_MUSIC)
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip("Test tooltip."))
        })
    }

    init {
        val pane = this.musicSettingsPane
        val vbox = VBox().apply {
            this.align.set(VBox.Align.TOP)
            this.spacing.set(2f)
        }
        pane.addChild(vbox)

        val title = TextLabel(binding = { Localization.getVar("editor.dialog.music.settings.heading.title").use() }).apply {
            this.bounds.height.set(140f)
            this.textColor.set(Color.WHITE)
            this.markup.set(editorPane.palette.markup)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.topLeft)
        }
        val overallWavePane = OverallWavePane(this)
        val overallWaveformLabels = Pane().apply {
            this.bounds.height.set(28f)
            this.addChild(TextLabel(convertMsToTimestamp(0f), font = editor.main.mainFont).also { label ->
                Anchor.TopLeft.configure(label)
                label.margin.set(Insets(0f, 2f, 0f, 0f))
                label.bounds.width.set(120f)
                label.renderAlign.set(Align.bottomLeft)
                label.textAlign.set(TextAlign.LEFT)
                label.textColor.set(Color.WHITE)
            })
            this.addChild(TextLabel(binding = { convertMsToTimestamp(loadingProgress.durationMs.use()) }, font = editorPane.palette.musicDialogFont).also { label ->
                Anchor.TopRight.configure(label)
                label.margin.set(Insets(0f, 2f, 0f, 0f))
                label.bounds.width.set(120f)
                label.renderAlign.set(Align.bottomRight)
                label.textAlign.set(TextAlign.RIGHT)
                label.textColor.set(Color.WHITE)
            })

            val hbox = HBox().apply {
                Anchor.Centre.configure(this)
                this.spacing.set(8f)
                this.align.set(HBox.Align.CENTRE)
                this.bounds.width.set(900f)
            }
            this.addChild(hbox)

            hbox.temporarilyDisableLayouts {
                val playbackStartLoc = Localization.getVar("editor.dialog.music.settings.playbackStart", Var { listOf(convertMsToTimestamp(window.playbackStart.use() * 1000)) })
                hbox.addChild(TextLabel(binding = { playbackStartLoc.use() }).apply {
                    this.textColor.set(Color.WHITE.cpy())
                    this.markup.set(editorPane.palette.markup)
                    this.renderAlign.set(Align.center)
                    this.textAlign.set(TextAlign.CENTRE)
                    this.bounds.width.set(350f)
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.playbackStart.tooltip")))
                })
            }
        }
        val overallWaveform = Pane().apply {
            this.border.set(Insets(1f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
            this.bounds.height.set(2f + WAVEFORM_HEIGHT * 2)
            val bg = RectElement(Color.BLACK)
            this.addChild(bg)
            bg.addChild(overallWavePane)
        }
        val zoomedWavePane = ZoomedWavePane(this, overallWavePane)
        val zoomedWaveformCtr = Pane().apply {
            this.border.set(Insets(1f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
            this.bounds.height.set(2f + WAVEFORM_HEIGHT * 2)
            val bg = RectElement(Color.BLACK)
            this.addChild(bg)
            bg.addChild(zoomedWavePane)
        }
        val zoomedWaveformLabels = Pane().apply {
            this.bounds.height.set(32f)
            this.addChild(TextLabel(binding = { convertMsToTimestamp(window.x.use() * 1000) },
                    font = editor.main.mainFont).also { label ->
                Anchor.TopLeft.configure(label)
                label.margin.set(Insets(0f, 2f, 0f, 0f))
                label.bounds.width.set(120f)
                label.renderAlign.set(Align.bottomLeft)
                label.textAlign.set(TextAlign.LEFT)
                label.textColor.set(Color.WHITE)
            })
            this.addChild(TextLabel(binding = { convertMsToTimestamp((window.x.use() + window.widthSec.use()) * 1000) },
                    font = editor.main.mainFont).also { label ->
                Anchor.TopRight.configure(label)
                label.margin.set(Insets(0f, 2f, 0f, 0f))
                label.bounds.width.set(120f)
                label.renderAlign.set(Align.bottomRight)
                label.textAlign.set(TextAlign.RIGHT)
                label.textColor.set(Color.WHITE)
            })

            val hbox = HBox().apply {
                Anchor.Centre.configure(this)
                this.spacing.set(8f)
                this.align.set(HBox.Align.CENTRE)
                this.bounds.width.set(920f)
            }
            this.addChild(hbox)

            hbox.temporarilyDisableLayouts {
                val white = Color.WHITE.cpy()
                hbox.addChild(TextLabel(binding = { Localization.getVar("editor.dialog.music.settings.heading.marker").use() }, font = editorPane.palette.musicDialogFontBold).apply {
                    this.textColor.set(white)
                    this.renderAlign.set(Align.right)
                    this.textAlign.set(TextAlign.RIGHT)
                    this.bounds.width.set(200f)
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.heading.marker.tooltip")))
                })
                val toggleGroup = ToggleGroup()
                val radioWidth = 230f
                hbox.addChild(RadioButton("").apply {
                    val textColor = markerFirstBeat
                    val loc = Localization.getVar("editor.dialog.music.settings.marker.firstBeat", Var {
                        listOf(/* intentional space */ " " + convertMsToTimestamp(window.firstBeat.use() * 1000))
                    })
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.marker.firstBeat.tooltip")))
                    this.textLabel.text.bind { loc.use() }
                    this.textLabel.markup.set(editorPane.palette.markup)
                    toggleGroup.addToggle(this)
                    this.imageNode.padding.set(Insets(4f))
                    this.color.set(white)
                    this.bounds.width.set(radioWidth)
                    this.onSelected = {
                        currentMarker.set(MarkerType.FIRST_BEAT)
                    }
                    this.checkedState.set(true) // First one only
                })
                hbox.addChild(RadioButton("").apply {
                    val textColor = markerLoopStart
                    val loc = Localization.getVar("editor.dialog.music.settings.marker.loopStart", Var {
                        listOf(/* intentional space */ " " + convertMsToTimestamp(window.loopStart.use() * 1000))
                    })
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.marker.loopStart.tooltip")))
                    this.textLabel.text.bind { loc.use() }
                    this.textLabel.markup.set(editorPane.palette.markup)
                    toggleGroup.addToggle(this)
                    this.imageNode.padding.set(Insets(4f))
                    this.color.set(white)
                    this.bounds.width.set(radioWidth)
                    this.selectedState.addListener {
                        if (it.getOrCompute()) currentMarker.set(MarkerType.LOOP_START)
                    }
                    this.onSelected = {
                        currentMarker.set(MarkerType.LOOP_START)
                    }
                })
                hbox.addChild(RadioButton("").apply {
                    val textColor = markerLoopEnd
                    val loc = Localization.getVar("editor.dialog.music.settings.marker.loopEnd", Var {
                        listOf(/* intentional space */ " " + convertMsToTimestamp(window.loopEnd.use() * 1000))
                    })
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.marker.loopEnd.tooltip")))
                    this.textLabel.text.bind { loc.use() }
                    this.textLabel.markup.set(editorPane.palette.markup)
                    toggleGroup.addToggle(this)
                    this.color.set(white)
                    this.imageNode.padding.set(Insets(4f))
                    this.bounds.width.set(radioWidth)
                    this.onSelected = {
                        currentMarker.set(MarkerType.LOOP_END)
                    }
                })
            }
        }
        
        var lCheckBox: CheckBox? = null
        var rateSlider: Slider? = null

        val otherControlsPane = Pane().apply {
            this.bounds.height.set(44f)
            this.margin.set(Insets(4f, 0f, 0f, 0f))

            val hbox = HBox().apply {
                Anchor.TopCentre.configure(this)
                this.spacing.set(8f)
            }
            this += hbox

            hbox.temporarilyDisableLayouts {
                lCheckBox = CheckBox(binding = { Localization.getVar("editor.dialog.music.settings.enableLooping").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.checkedState.set(window.doLooping.get())
                    this.checkedState.addListener {
                        window.doLooping.set(it.getOrCompute())
                    }
                    this.imageNode.padding.set(Insets(4f))
                    this.color.set(Color.WHITE.cpy())
                    this.bounds.width.set(220f)
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.enableLooping.tooltip")))
                }
                hbox.addChild(lCheckBox!!)
                hbox.addChild(Button(binding = { Localization.getVar("editor.dialog.music.settings.resetLoopPoints").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleContent()
                    this.bounds.width.set(200f)
                    this.setOnAction {
                        window.loopStart.set(0f)
                        window.loopEnd.set(0f)
                    }
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.resetLoopPoints.tooltip")))
                })
                hbox.addChild(Button(binding = { Localization.getVar("editor.dialog.music.settings.setLoopEndToEnd").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleContent()
                    this.bounds.width.set(230f)
                    this.setOnAction {
                        window.loopEnd.set(window.musicDurationSec.get())
                    }
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.setLoopEndToEnd.tooltip")))
                })
                hbox.addChild(Button(binding = { Localization.getVar("editor.dialog.music.settings.skipSilence").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleContent()
                    this.bounds.width.set(230f)
                    this.setOnAction {
                        val estimate = findStartOfNonSilence()
                        if (estimate >= 0f) {
                            window.firstBeat.set(estimate)
                        }
                    }
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.skipSilence.tooltip")))
                })
                hbox.addChild(Pane().apply { 
                    this.bounds.width.set(250f)
                    val labelVar = Localization.getVar("editor.dialog.music.settings.rate", Var {
                        listOf(DecimalFormats.format("0.00", editor.musicData.rate.use()))
                    })
                    this += TextLabel(binding = { labelVar.use() }, font = editorPane.palette.musicDialogFont).apply {
                        this.textColor.set(Color.WHITE)
                        this.bindHeightToParent(multiplier = 0.5f)
                        this.padding.set(Insets(1f))
                        this.setScaleXY(0.8f)
                        this.markup.set(editorPane.palette.markup)
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.rate.tooltip")))
                    }
                    rateSlider = Slider().apply {
                        Anchor.BottomLeft.configure(this)
                        this.bindHeightToParent(multiplier = 0.5f)
                        this.padding.set(Insets(1f))
                        this.minimum.set(10f)
                        this.maximum.set(200f)
                        this.tickUnit.set(1f)
                        this.setValue(100f)
                        this.value.addListener { v ->
                            val newRate = v.getOrCompute() / 100f
                            editor.musicData.rate.set(newRate)

                            val engine = editor.engine
                            val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
                            if (player != null && !player.isPaused) {
                                player.pitch = newRate
                            }
                        }
                        this.setOnRightClick {
                            this.setValue(100f)
                        }
                    }
                    this += rateSlider!!
                })
            }
        }
        
        this.loopingCheckbox = lCheckBox!!
        this.rateSlider = rateSlider!!

        vbox.temporarilyDisableLayouts {
            vbox += title
            vbox += overallWaveformLabels
            vbox += overallWaveform
            vbox += zoomedWaveformLabels
            vbox += zoomedWaveformCtr
            vbox += otherControlsPane
        }
    }

    override fun canCloseDialog(): Boolean {
        val substate = substate.getOrCompute()
        if (!(substate == Substate.NO_MUSIC || substate == Substate.HAS_MUSIC)) return false
        
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        stopMusicPlayback()

        // Push changes to editor
        editor.musicData.firstBeatSec.set(this.window.firstBeat.get())
        editor.musicData.loopParams.set(this.window.createLoopParams())
        editor.compileEditorMusicInfo()
        editor.compileEditorMusicVolumes()
    }

    fun prepareShow(): MusicDialog {
        val editorMusicData = editor.musicData
        val currentMusic = editorMusicData.beadsMusic
        substate.set(if (currentMusic == null) Substate.NO_MUSIC else Substate.HAS_MUSIC)
        val load = loadingProgress
        load.bytesSoFar.set(0L)
        load.durationMs.set(currentMusic?.musicSample?.lengthMs?.toFloat() ?: 0f)

        // Copy over music details
        setControlsToMusicDetails()

        stopMusicPlayback()

        return this
    }
    
    private fun setControlsToMusicDetails() {
        val editorMusicData = editor.musicData
        // Copy over music details
        this.window.firstBeat.set(editorMusicData.firstBeatSec.get())
        val loopParams = editorMusicData.loopParams.getOrCompute()
        if (loopParams.loopType == SamplePlayer.LoopType.NO_LOOP_FORWARDS) {
            this.window.doLooping.set(false)
            this.window.loopStart.set(0f)
            this.window.loopEnd.set(0f)
            loopingCheckbox.checkedState.set(false)
        } else {
            this.window.doLooping.set(true)
            this.window.loopStart.set((loopParams.startPointMs / 1000).toFloat())
            this.window.loopEnd.set((loopParams.endPointMs / 1000).toFloat())
            loopingCheckbox.checkedState.set(true)
        }
        rateSlider.setValue(editorMusicData.rate.get() * 100f)
    }

    fun stopMusicPlayback() {
        val engine = editor.engine
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) {
            player.pause(true)
            editor.soundSystem.setPaused(true)
        }
    }

    fun startMusicPlayback() {
        val engine = editor.engine
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) {
            player.position = window.playbackStart.get() * 1000.0
            player.useLoopParams(window.createLoopParams())
            player.pitch = editor.musicData.rate.get()
            player.pause(false)
            editor.soundSystem.setPaused(false)
        }
    }

    fun attemptSelectMusic(dropPath: String?) {
        val prevSubstate = substate.getOrCompute()
        if (prevSubstate == Substate.FILE_DIALOG_OPEN) return
        stopMusicPlayback()
        substate.set(Substate.FILE_DIALOG_OPEN)
        val main = editor.main
        
        fun loadFile(file: File) {
            try {
                val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                Gdx.app.postRunnable {
                    main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_MUSIC, newInitialDirectory)
                    loadingProgress.reset()
                    window.reset()
                    substate.set(Substate.LOADING)
                }

                val tmp: File = TempFileUtils.createTempFile("music", suffix = ".${file.extension}")
                file.copyTo(tmp, overwrite = true)

                loadMusic(tmp, true)
            } catch (e: Exception) {
                Paintbox.LOGGER.warn("Error occurred while loading music:")
                e.printStackTrace()
                Gdx.app.postRunnable {
                    substate.set(Substate.LOAD_ERROR)
                    val exClassName = e.javaClass.name
                    errorLabel.text.set(Localization.getValue("editor.dialog.music.loadError", exClassName))
                }
            }
        }
        
        if (dropPath != null) {
            thread(isDaemon = true) {
                loadFile(File(dropPath))
            }
        } else {
            editorPane.main.restoreForExternalDialog { completionCallback ->
                thread(isDaemon = true) {
                    val title = Localization.getValue("fileChooser.musicSelect.title")
                    val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.musicSelect.filter"), SUPPORTED_MUSIC_EXTENSIONS).copyWithExtensionsInDesc()
                    TinyFDWrapper.openFile(title,
                            main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_MUSIC)
                                    ?: main.getDefaultDirectory(), filter) { file: File? ->
                        completionCallback()
                        if (file != null) {
                            loadFile(file)
                        } else {
                            Gdx.app.postRunnable {
                                substate.set(prevSubstate)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadMusic(compressedFile: File, isFileTemp: Boolean) {
        editor.musicData.removeMusic()
        var lastUpdateMs = 0L
        val listener = GdxAudioReader.AudioLoadListener { bytesSoFar, _ ->
            if ((System.currentTimeMillis() - lastUpdateMs) >= 200L) {
                lastUpdateMs = System.currentTimeMillis()
                loadingProgress.bytesSoFar.set(bytesSoFar)
            }
        }
        val externalRes = ExternalResource(Container.RES_KEY_COMPRESSED_MUSIC, compressedFile, isFileTemp)
        val newMusic: BeadsMusic = GdxAudioReader.newMusic(FileHandle(compressedFile), listener)
        Gdx.app.postRunnable {
            loadingProgress.generatingWaveform.set(true)
            loadingProgress.durationMs.set(newMusic.musicSample.lengthMs.toFloat())
        }
        editor.musicData.setMusic(newMusic, externalRes)
        editor.musicData.waveform?.generateSummaries()
        Gdx.app.postRunnable {
            editor.updateForNewMusicData()
            
            setControlsToMusicDetails()
            substate.set(Substate.HAS_MUSIC)
        }
    }

    private fun convertMsToTimestamp(ms: Float): String {
        return TimeUtils.convertMsToTimestamp(ms)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)

        val engine = editor.engine
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        // TODO make this not polling based.
        if (player != null) {
            val wasPaused = player.isPaused
            if (wasPaused) {
                currentMusicPosition.set(-1f)
            } else {
                currentMusicPosition.set((player.position / 1000).toFloat())
            }
            
            if (player.position > player.musicSample.lengthMs) {
                stopMusicPlayback()
            }

            if (substate.getOrCompute() == Substate.HAS_MUSIC) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (wasPaused) {
                        startMusicPlayback()
                    } else {
                        stopMusicPlayback()
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && wasPaused) {
                    val marker = currentMarker.getOrCompute()
                    val variable: FloatVar = when (marker) {
                        MarkerType.FIRST_BEAT -> window.firstBeat
                        MarkerType.LOOP_START -> window.loopStart
                        MarkerType.LOOP_END -> window.loopEnd
                    }
                    val startSec = variable.get()
                    var zeroCross = startSec
                    for (i in 0 until 3) { // Found that repeated iterations improves accuracy (?)
                        zeroCross = findZeroCrossing(startSec)
                    }
                    variable.set(zeroCross)
                }
            }
        }
    }

    fun findZeroCrossing(startSec: Float): Float {
        // Code adapted from Audacity. https://github.com/audacity/audacity/blob/cf666c9bfc1cdae95e325f372c13489f797c18a0/src/menus/SelectMenus.cpp#L55
        val sample = editor.musicData.beadsMusic?.musicSample ?: return startSec
        val sampleRate = sample.sampleRate
        val startSample = (startSec * sampleRate).toInt()
        val windowSize = (sampleRate * 0.01f).toInt().coerceAtLeast(1)
        val channels = sample.nChannels

        val left = startSample - windowSize / 2

        val frameData: FloatArray = FloatArray(channels)
        val dist: FloatArray = FloatArray(windowSize) { 0f }
        val prev: FloatArray = FloatArray(channels) { 2f }
        for (i in 0 until windowSize) {
            frameData.fill(2f)
            sample.getFrame(i + left, frameData)

            for (ch in 0 until channels) {
                val fd = frameData[ch]
                var fDist = abs(fd) // score is abs value
                if (prev[ch] * fd > 0) // Both values are the same sign
                    fDist += 0.4f
                else if (prev[ch] > 0) {
                    fDist += 0.1f // medium penalty for downward crossing
                }
                prev[ch] = fd
                dist[i] += fDist
            }

            // Apply a small penalty for distance from the original endpoint
            // We'll always prefer an upward
            dist[i] += 0.1f * (abs(i - (windowSize / 2))) / (windowSize / 2f)
        }

        // Find minimum
        var minIndex = 0
        var minValue = 3f
        for (i in 0 until windowSize) {
            if (dist[i] < minValue) {
                minIndex = i
                minValue = dist[i]
            }
        }

        // If we're worse than 0.2 on average, on one track, then no good.
        if (channels == 1 && minValue > 0.2f) return startSec
        // If we're worse than 0.6 on average, on multi-track, then no good.
        if (channels > 1 && minValue > 0.6f * channels) return startSec

        return startSec + ((minIndex - windowSize / 2) / sampleRate)
    }

    fun findStartOfNonSilence(): Float {
        val sample = editor.musicData.beadsMusic?.musicSample ?: return -1f
        val array = FloatArray(sample.nChannels) { 0f }
        for (i in 0 until sample.nFrames) {
            sample.getFrame(i.toInt(), array)
            if (array.any { !MathUtils.isEqual(it, 0f, 0.0005f) }) {
                return sample.samplesToMs(i.toDouble()).toFloat() / 1000f
            }
        }
        return -1f
    }

    data class LoadingIndicator(
            val bytesSoFar: Var<Long> = Var(0L),
            val generatingWaveform: BooleanVar = BooleanVar(false),
            val durationMs: FloatVar = FloatVar(0f),
    ) {

        fun reset() {
            bytesSoFar.set(0L)
            generatingWaveform.set(false)
            durationMs.set(0f)
        }
    }

    data class Window(
            val x: FloatVar = FloatVar(0f),
            val widthSec: FloatVar = FloatVar(0f),
            val musicDurationSec: FloatVar = FloatVar(0f),
            val doLooping: BooleanVar = BooleanVar(false),
            val playbackStart: FloatVar = FloatVar(0f),
            val firstBeat: FloatVar = FloatVar(0f),
            val loopStart: FloatVar = FloatVar(0f),
            val loopEnd: FloatVar = FloatVar(0f),
    ) {
        companion object {
            const val DEFAULT_WIDTH: Float = 4f
        }

        init {
            reset()
        }

        fun reset() {
            x.set(0f)
            widthSec.set(DEFAULT_WIDTH)
            musicDurationSec.set(0f)
            playbackStart.set(0f)
            firstBeat.set(0f)
            loopStart.set(0f)
            loopEnd.set(0f)
            doLooping.set(false)
        }

        fun createLoopParams(): LoopParams {
            return LoopParams(if (doLooping.get()) 
                SamplePlayer.LoopType.LOOP_FORWARDS 
            else SamplePlayer.LoopType.NO_LOOP_FORWARDS, 
                    loopStart.get() * 1000.0, loopEnd.get() * 1000.0)
        }

        fun limitWindow() {
            val dur = musicDurationSec.get()
            if (widthSec.get() > dur) {
                widthSec.set(dur)
            }
        }
    }
}

