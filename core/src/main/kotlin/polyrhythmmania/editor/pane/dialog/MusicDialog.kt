package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.packing.PackedSheet
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.*
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.ui.layout.HBox
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.util.TinyFDWrapper
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.util.DecimalFormats
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs


class MusicDialog(editorPane: EditorPane) : BasicDialog(editorPane) {

    companion object {
        const val WAVEFORM_HEIGHT: Int = 48
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

    val markerPlaybackStart: Color = PRManiaColors.PLAYBACK_START.cpy()
    val markerFirstBeat: Color = PRManiaColors.MARKER_FIRST_BEAT.cpy()
    val markerLoopStart: Color = PRManiaColors.MARKER_LOOP_START.cpy()
    val markerLoopEnd: Color = PRManiaColors.MARKER_LOOP_END.cpy()

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.music.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bounds.width.bind { bounds.height.use() }
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptCloseDialog()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.generic.close")))
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
        fileDialogOpenPane.addChild(TextLabel(binding = { Localization.getVar("editor.dialog.music.closeFileDialog").use() }).apply {
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
                attemptSelectMusic()
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
            this.visible.bind {
                val ss = substate.use()
                ss == Substate.LOAD_ERROR
            }
        }
        bottomPane.addChild(this.loadMusicErrorBottomPane)
        this.loadMusicErrorBottomPane.addChild(Button(binding = { Localization.getVar("editor.dialog.music.okButton").use() }, font = editorPane.palette.musicDialogFont).apply {
            this.bounds.width.set(250f)
            this.applyDialogStyleBottom()
            this.setOnAction {
                substate.set(Substate.NO_MUSIC)
            }
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
                    this.textLabel.textColor.set(white)
                    toggleGroup.addToggle(this)
                    this.imageNode.tint.set(white)
                    this.imageNode.padding.set(Insets(4f))
                    this.bounds.width.set(radioWidth)
                    this.selectedState.addListener {
                        if (it.getOrCompute()) currentMarker.set(MarkerType.FIRST_BEAT)
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
                    this.textLabel.textColor.set(white)
                    toggleGroup.addToggle(this)
                    this.imageNode.tint.set(white)
                    this.imageNode.padding.set(Insets(4f))
                    this.bounds.width.set(radioWidth)
                    this.selectedState.addListener {
                        if (it.getOrCompute()) currentMarker.set(MarkerType.LOOP_START)
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
                    this.textLabel.textColor.set(white)
                    toggleGroup.addToggle(this)
                    this.imageNode.tint.set(white)
                    this.imageNode.padding.set(Insets(4f))
                    this.bounds.width.set(radioWidth)
                    this.selectedState.addListener {
                        if (it.getOrCompute()) currentMarker.set(MarkerType.LOOP_END)
                    }
                })
            }
        }

        val otherControlsPane = Pane().apply {
            this.bounds.height.set(44f)
            this.margin.set(Insets(4f, 0f, 0f, 0f))

            val hbox = HBox().apply {
                Anchor.TopCentre.configure(this)
                this.spacing.set(8f)
            }
            this += hbox

            hbox.temporarilyDisableLayouts {
                hbox.addChild(CheckBox(binding = { Localization.getVar("editor.dialog.music.settings.enableLooping").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.checkedState.set(window.doLooping.getOrCompute())
                    window.doLooping.addListener {
                        this.checkedState.set(it.getOrCompute())
                    }
                    this.checkedState.addListener {
                        window.doLooping.set(it.getOrCompute())
                    }
                    this.imageNode.tint.set(Color.WHITE.cpy())
                    this.textLabel.textColor.set(Color.WHITE.cpy())
                    this.bounds.width.set(220f)
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.enableLooping.tooltip")))
                })
                hbox.addChild(Button(binding = { Localization.getVar("editor.dialog.music.settings.resetLoopPoints").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleContent()
                    this.bounds.width.set(250f)
                    this.setOnAction {
                        window.loopStart.set(0f)
                        window.loopEnd.set(0f)
                    }
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.resetLoopPoints.tooltip")))
                })
                hbox.addChild(Button(binding = { Localization.getVar("editor.dialog.music.settings.setLoopEndToEnd").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleContent()
                    this.bounds.width.set(250f)
                    this.setOnAction {
                        window.loopEnd.set(window.musicDurationSec.getOrCompute())
                    }
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.setLoopEndToEnd.tooltip")))
                })
                hbox.addChild(Button(binding = { Localization.getVar("editor.dialog.music.settings.skipSilence").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleContent()
                    this.bounds.width.set(250f)
                    this.setOnAction {
                        val estimate = findStartOfNonSilence()
                        if (estimate >= 0f) {
                            window.firstBeat.set(estimate)
                        }
                    }
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.music.settings.skipSilence.tooltip")))
                })
            }
        }

        vbox.temporarilyDisableLayouts {
            vbox += title
            vbox += overallWaveformLabels
            vbox += overallWaveform
            vbox += zoomedWaveformLabels
            vbox += zoomedWaveformCtr
            vbox += otherControlsPane
        }
    }

    private fun attemptCloseDialog() {
        val substate = substate.getOrCompute()
        if (substate == Substate.LOADING || substate == Substate.LOAD_ERROR || substate == Substate.FILE_DIALOG_OPEN) return

        stopMusicPlayback()

        // Push changes to editor
        editor.musicData.firstBeat.set(this.window.firstBeat.getOrCompute())
        editor.musicData.loopParams.set(this.window.createLoopParams())

        editorPane.closeDialog()
    }

    fun prepareShow(): MusicDialog {
        val currentMusic = editor.musicData.beadsMusic
        substate.set(if (currentMusic == null) Substate.NO_MUSIC else Substate.HAS_MUSIC)
        val load = loadingProgress
        load.bytesSoFar.set(0L)
        load.durationMs.set(currentMusic?.musicSample?.lengthMs?.toFloat() ?: 0f)

        // Copy over music details
        this.window.firstBeat.set(editor.musicData.firstBeat.getOrCompute())
        val loopParams = editor.musicData.loopParams.getOrCompute()
        if (loopParams == LoopParams.NO_LOOP_FORWARDS) {
            this.window.doLooping.set(false)
            this.window.loopStart.set(0f)
            this.window.loopEnd.set(0f)
        } else {
            this.window.doLooping.set(true)
            this.window.loopStart.set((loopParams.startPointMs / 1000).toFloat())
            this.window.loopEnd.set((loopParams.endPointMs / 1000).toFloat())
        }

        return this
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
            player.position = window.playbackStart.getOrCompute() * 1000.0
//            player.loopType = SamplePlayer.LoopType.NO_LOOP_FORWARDS
            player.useLoopParams(window.createLoopParams())
            player.pause(false)
            editor.soundSystem.setPaused(false)
        }
    }

    private fun attemptSelectMusic() {
        val prevSubstate = substate.getOrCompute()
        substate.set(Substate.FILE_DIALOG_OPEN)
        val main = editor.main
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val title = Localization.getValue("fileDialog.musicSelect.title")
                val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileDialog.musicSelect.filter"), listOf("*.ogg", "*.mp3", "*.wav")).copyWithExtensionsInDesc()
                TinyFDWrapper.openFile(title,
                        main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_MUSIC)
                                ?: main.getDefaultDirectory(), filter) { file: File? ->
                    completionCallback()
                    if (file != null) {
                        val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                        Gdx.app.postRunnable {
                            main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_MUSIC, newInitialDirectory)
                            loadingProgress.reset()
                            window.reset()
                            substate.set(Substate.LOADING)
                        }
                        editor.musicData.removeMusic()
                        try {
                            var lastUpdateMs = 0L
                            val listener = GdxAudioReader.AudioLoadListener { bytesSoFar, _ ->
                                if ((System.currentTimeMillis() - lastUpdateMs) >= 200L) {
                                    lastUpdateMs = System.currentTimeMillis()
                                    loadingProgress.bytesSoFar.set(bytesSoFar)
                                }
                            }
                            val newMusic: BeadsMusic = GdxAudioReader.newMusic(FileHandle(file), listener)
                            Gdx.app.postRunnable {
                                loadingProgress.generatingWaveform.set(true)
                                loadingProgress.durationMs.set(newMusic.musicSample.lengthMs.toFloat())
                            }
                            editor.musicData.setMusic(newMusic)
                            editor.musicData.waveform?.generateSummaries()
                            Gdx.app.postRunnable {
                                window.reset()
                                window.musicDurationSec.set((newMusic.musicSample.lengthMs / 1000).toFloat())
                                window.limitWindow()
                                editor.compileEditorMusicInfo()
                                editor.waveformWindow.generateOverall()
                                substate.set(Substate.HAS_MUSIC)
                            }
                        } catch (e: Exception) {
                            Paintbox.LOGGER.warn("Error occurred while loading music:")
                            e.printStackTrace()
                            Gdx.app.postRunnable {
                                substate.set(Substate.LOAD_ERROR)
                                val exClassName = e.javaClass.name
                                errorLabel.text.set(Localization.getValue("editor.dialog.music.loadError", exClassName))
                            }
                        }
                    } else {
                        Gdx.app.postRunnable {
                            substate.set(prevSubstate)
                        }
                    }
                }
            }
        }
    }

    fun convertMsToTimestamp(ms: Float): String {
        val min = (ms / 60_000).toInt()
        val sec = (ms / 1000 % 60).toInt()
        val msPart = (ms % 1000).toInt()

        return "${DecimalFormats.format("00", min)}:${DecimalFormats.format("00", sec)}.${DecimalFormats.format("000", msPart)}"
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
                    val startSec = variable.getOrCompute()
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
            val generatingWaveform: Var<Boolean> = Var(false),
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
            val doLooping: Var<Boolean> = Var(false),
            val playbackStart: FloatVar = FloatVar(0f),
            val firstBeat: FloatVar = FloatVar(0f),
            val loopStart: FloatVar = FloatVar(0f),
            val loopEnd: FloatVar = FloatVar(0f),
    ) {
        companion object {
            const val DEFAULT_WIDTH: Float = 5f
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
            return if (!doLooping.getOrCompute())
                LoopParams.NO_LOOP_FORWARDS
            else LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, loopStart.getOrCompute() * 1000.0, loopEnd.getOrCompute() * 1000.0)
        }

        fun limitWindow() {
            val dur = musicDurationSec.getOrCompute()
            if (widthSec.getOrCompute() > dur) {
                widthSec.set(dur)
            }
        }
    }
}

