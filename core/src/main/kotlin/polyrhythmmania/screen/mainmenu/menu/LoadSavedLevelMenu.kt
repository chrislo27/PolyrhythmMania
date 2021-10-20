package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.binding.invert
import paintbox.font.TextAlign
import paintbox.transition.FadeIn
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.control.Slider
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.Localization
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.Instantiators
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.library.score.LevelScoreAttempt
import polyrhythmmania.screen.PlayScreen
import polyrhythmmania.screen.mainmenu.bg.BgType
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import java.io.File
import java.util.function.Consumer
import kotlin.concurrent.thread

class LoadSavedLevelMenu(menuCol: MenuCollection, immediateLoad: File?,
                         val levelScoreAttemptConsumer: Consumer<LevelScoreAttempt>? = null)
    : StandardMenu(menuCol) {

    enum class Substate {
        FILE_DIALOG_OPEN,
        LOADING,
        LOADED,
        LOAD_ERROR,
    }

    val substate: Var<Substate> = Var(if (immediateLoad == null) Substate.FILE_DIALOG_OPEN else Substate.LOADING)

    val descLabel: TextLabel
    val challengeSetting: Pane
    
    val robotMode: BooleanVar = BooleanVar(false)
    val goForPerfect: BooleanVar = BooleanVar(false)
    val tempoUp: Var<Int> = Var(100)

    @Volatile
    private var loaded: LoadData? = null

    init {
        this.setSize(WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.play.playSavedLevel.title").use() }
        this.contentPane.bounds.height.set(300f)
        this.deleteWhenPopped.set(true)

        val content = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        descLabel = TextLabel(text = Localization.getValue("common.closeFileChooser")).apply {
            this.markup.set(this@LoadSavedLevelMenu.markup)
            this.padding.set(Insets(4f))
            this.bounds.height.set(96f)
            this.textColor.set(LongButtonSkin.TEXT_COLOR)
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        }
        content.addChild(descLabel)
        challengeSetting = Pane().apply {
            Anchor.BottomLeft.configure(this)
            this.bindHeightToParent(adjust = -96f)
            this.margin.set(Insets(4f, 0f, 0f, 0f))
            this.visible.set(false)
            this += VBox().apply {
                this.spacing.set(1f)
                this += TextLabel(binding = { Localization.getVar("mainMenu.play.challengeSettings").use() },
                        font = this@LoadSavedLevelMenu.font).apply {
                    this.bounds.height.set(32f)
                }
                this += Pane().apply {
                    this.bounds.height.set(32f)
                    val perfectCheckbox = CheckBox(binding = { Localization.getVar("mainMenu.play.challengeSettings.perfect").use() },
                            font = this@LoadSavedLevelMenu.font).apply {
                        this.bindWidthToParent(multiplier = 0.5f)
                        this.checkedState.set(goForPerfect.get())
                        this.color.set(LongButtonSkin.TEXT_COLOR)
                        this.color.bind {
                            if (apparentDisabledState.useB()) {
                                LongButtonSkin.DISABLED_TEXT
                            } else LongButtonSkin.TEXT_COLOR
                        }
                        this.textLabel.padding.set(Insets(0f, 0f, 4f, 0f))
                        this.onCheckChanged = { newState ->
                            goForPerfect.set(newState)
                        }
                        this.disabled.bind { robotMode.useB() }
                    }
                    this += perfectCheckbox
                    this += CheckBox(binding = { Localization.getVar("mainMenu.play.challengeSettings.robotMode").use() },
                            font = this@LoadSavedLevelMenu.font).apply {
                        this.bindWidthToParent(multiplier = 0.5f)
                        Anchor.TopRight.configure(this)
                        this.checkedState.set(robotMode.get())
                        this.color.set(LongButtonSkin.TEXT_COLOR)
                        this.textLabel.padding.set(Insets(0f, 0f, 4f, 0f))
                        this.onCheckChanged = { newState ->
                            robotMode.set(newState)
                            if (newState) {
                                perfectCheckbox.checkedState.set(false)
                            }
                        }
                        this.setOnAction {
                            val newState = checkedState.invert()
                            if (newState) {
                                menuCol.playMenuSound("sfx_pause_robot_on")
                            } else {
                                menuCol.playMenuSound("sfx_pause_robot_off")
                            }
                        }
                        this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.play.challengeSettings.robotMode.tooltip")))
                    }
                }
                this += HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(8f)
                    this += TextLabel(binding = { Localization.getVar("mainMenu.play.challengeSettings.speed").use() },
                            font = this@LoadSavedLevelMenu.font).apply {
                        this.bounds.width.set(100f)
                        this.textColor.set(LongButtonSkin.TEXT_COLOR)
                        this.renderAlign.set(Align.right)
                    }
                    val slider = Slider().apply slider@{
                        this.bounds.width.set(200f)
                        this.setValue(tempoUp.getOrCompute().toFloat())
                        this.minimum.set(10f)
                        this.maximum.set(250f)
                        this.tickUnit.set(5f)
                        this.value.addListener { 
                            tempoUp.set(it.getOrCompute().toInt())
                        }
                        (this.skin.getOrCompute() as Slider.SliderSkin).also { skin ->
                            val filledColors = listOf(Challenges.TEMPO_DOWN_COLOR, Color(0.24f, 0.74f, 0.94f, 1f), Challenges.TEMPO_UP_COLOR)
                            skin.filledColor.sideEffecting { existing -> 
                                val tempo = this@slider.value.useF().toInt()
                                existing.set(filledColors[if (tempo < 100) 0 else if (tempo > 100) 2 else 1])
                            }
                        }
                    }
                    this += slider
                    val percent = Localization.getVar("mainMenu.play.challengeSettings.speed.percent", Var {
                        listOf(slider.value.useF().toInt())
                    })
                    this += TextLabel(binding = { percent.use() },
                            font = this@LoadSavedLevelMenu.font).apply {
                        this.bounds.width.set(75f)
                        this.textColor.set(LongButtonSkin.TEXT_COLOR)
                        this.renderAlign.set(Align.left)
                        this.setScaleXY(0.9f)
                    }
                }
            }
        }
        content.addChild(challengeSetting)

        contentPane.addChild(content)
        contentPane.addChild(hbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.cancel").use() }).apply {
                this.bounds.width.set(100f)
                this.visible.bind {
                    when (substate.use()) {
                        Substate.LOADED, Substate.LOAD_ERROR -> true
                        else -> false
                    }
                }
                this.setOnAction {
                    loaded?.newContainer?.disposeQuietly()
                    loaded = null
                    menuCol.popLastMenu(playSound = true)
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.play.playAction").use() }).apply {
                this.bounds.width.set(125f)
                this.visible.bind {
                    when (substate.use()) {
                        Substate.LOADED -> true
                        else -> false
                    }
                }
                this.setOnAction {
                    val loadedData = loaded
                    if (loadedData != null) {
                        val engine = loadedData.newContainer.engine
                        val robotMode = this@LoadSavedLevelMenu.robotMode.get()
                        menuCol.playMenuSound("sfx_menu_enter_game")
                        if (robotMode) {
                            menuCol.playMenuSound("sfx_pause_robot_on")
                            engine.autoInputs = true
                        }
                        
                        // Set challenge settings
                        val challenges: Challenges = Challenges(tempoUp.getOrCompute(), goForPerfect.get() && !robotMode)
                        challenges.applyToEngine(engine)
                        
                        mainMenu.transitionAway {
                            val main = mainMenu.main
                            val playScreen = PlayScreen(main, null, loadedData.newContainer, challenges,
                                    inputCalibration = main.settings.inputCalibration.getOrCompute(),
                                    levelScoreAttemptConsumer = levelScoreAttemptConsumer,
                                    showResults = !robotMode)
                            main.screen = TransitionScreen(main, main.screen, playScreen, null, FadeIn(0.25f, Color(0f, 0f, 0f, 1f))).apply { 
                                this.onEntryEnd = {
                                    playScreen.prepareGameStart()
                                    menuCol.popLastMenu(playSound = false)
                                    DiscordCore.updateActivity(DefaultPresences.playingLevel())
                                    mainMenu.backgroundType = BgType.NORMAL
                                }
                            }
                        }
                    }
                }
            }
            val keyboardKeybindings = main.settings.inputKeymapKeyboard.getOrCompute()
            hbox += TextLabel("${Localization.getValue("mainMenu.inputSettings.keyboard.keybindPause")}: ${Input.Keys.toString(Input.Keys.ESCAPE)}/${Input.Keys.toString(keyboardKeybindings.pause)} | " + keyboardKeybindings.toKeyboardString(false, true),
                    font = main.fontMainMenuRodin).apply {
                this.bounds.width.set(300f)
                this.textColor.set(Color.BLACK)
                this.visible.bind {
                    substate.use() == Substate.LOADED
                }
                this.setScaleXY(0.8f)
            }
        }
    }

    init { // This init block should be LAST
        if (immediateLoad != null) {
            Gdx.app.postRunnable {
                thread(isDaemon = true) {
                    try {
                        loadFile(immediateLoad)
                    } catch (e: Exception) {
                        Gdx.app.postRunnable { 
                            throw e
                        }
                    }
                }
            }
        } else {
            Gdx.app.postRunnable {
                main.restoreForExternalDialog { completionCallback ->
                    thread(isDaemon = true) {
                        val title = Localization.getValue("fileChooser.load.level.title")
                        val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.load.level.filter"),
                                listOf("*.${Container.LEVEL_FILE_EXTENSION}")).copyWithExtensionsInDesc()
                        TinyFDWrapper.openFile(title,
                                main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_PLAY_SAVED_LEVEL)
                                        ?: main.getDefaultDirectory(), filter) { file: File? ->
                            completionCallback()
                            if (file != null) {
                                try {
                                    loadFile(file)
                                } catch (e: Exception) {
                                    Gdx.app.postRunnable {
                                        throw e
                                    }
                                }
                            } else { // Cancelled out
                                Gdx.app.postRunnable {
                                    menuCol.popLastMenu(playSound = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * To be called in the SAME thread that opened the file chooser.
     */
    private fun loadFile(newFile: File) {
        Gdx.app.postRunnable {
            descLabel.doLineWrapping.set(false)
            descLabel.text.set(Localization.getValue("editor.dialog.load.loading"))
            substate.set(Substate.LOADING)
        }

        val newSoundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem().apply {
            this.audioContext.out.gain = main.settings.gameplayVolume.getOrCompute() / 100f
        }
        val newContainer: Container = Container(newSoundSystem, SimpleTimingProvider {
            Gdx.app.postRunnable {
                throw it
            }
            true
        }, GlobalContainerSettings(main.settings.forceTexturePack.getOrCompute(), main.settings.onlyDefaultPalette.getOrCompute()))

        try {
            val loadMetadata = newContainer.readFromFile(newFile)

            if (newContainer.blocks.none { it is BlockEndState }) {
                Gdx.app.postRunnable {
                    substate.set(Substate.LOAD_ERROR)
                    descLabel.doLineWrapping.set(true)
                    descLabel.text.set(Localization.getValue("mainMenu.play.noEndState",
                            Localization.getValue(Instantiators.endStateInstantiator.name.getOrCompute())))
                    newContainer.disposeQuietly()
                }
            } else if (loadMetadata.isFutureVersion) {
                Gdx.app.postRunnable {
                    substate.set(Substate.LOAD_ERROR)
                    descLabel.doLineWrapping.set(true)
                    descLabel.text.set(Localization.getValue("editor.dialog.load.error.futureVersion", loadMetadata.programVersion.toString(), "${loadMetadata.containerVersion}"))
                    newContainer.disposeQuietly()
                }
            } else {
                Gdx.app.postRunnable {
                    substate.set(Substate.LOADED)
                    descLabel.text.set(Localization.getValue("editor.dialog.load.loadedInformation", loadMetadata.programVersion, "${loadMetadata.containerVersion}"))
                    loadMetadata.loadOnGLThread()
                    loaded = LoadData(newContainer, loadMetadata)

                    val newInitialDirectory = if (!newFile.isDirectory) newFile.parentFile else newFile
                    main.persistDirectory(PreferenceKeys.FILE_CHOOSER_PLAY_SAVED_LEVEL, newInitialDirectory)
                    challengeSetting.visible.set(true)
                }
            }
        } catch (e: Exception) {
            Paintbox.LOGGER.warn("Error occurred while loading container:")
            e.printStackTrace()
            val exClassName = e.javaClass.name
            Gdx.app.postRunnable {
                substate.set(Substate.LOAD_ERROR)
                descLabel.doLineWrapping.set(true)
                descLabel.setScaleXY(0.75f)
                descLabel.text.set(Localization.getValue("editor.dialog.load.loadError", exClassName))
                newContainer.disposeQuietly()
            }
        }
    }

    data class LoadData(val newContainer: Container, val loadMetadata: Container.LoadMetadata)
}