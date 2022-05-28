package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.Paintbox
import paintbox.logging.SysOutPiper
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.Settings
import polyrhythmmania.container.Container
import polyrhythmmania.library.score.GlobalScoreCache
import polyrhythmmania.gamemodes.endlessmode.EndlessHighScore
import polyrhythmmania.ui.PRManiaSkins


class DataSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.dataSettings.title").use() }
        this.contentPane.bounds.height.set(320f)

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
            this.margin.set(Insets(0f, 0f, 0f, 4f))
        }

        val deleteRecoveryButton: Button = createLongButton { 
            Localization.getVar("mainMenu.dataSettings.deleteAllRecovery").use()
        }.apply {
            this.setOnAction {
                this.disabled.set(true)
                Gdx.app.postRunnable {
                    try {
                        PRMania.RECOVERY_FOLDER.listFiles()?.filter { f ->
                            val ext = f.extension
                            f != null && f.isFile && (ext == Container.PROJECT_FILE_EXTENSION || ext == Container.LEVEL_FILE_EXTENSION)
                        }?.forEach {
                            it.delete()
                            Paintbox.LOGGER.info("Deleted recovery file ${it.name}, lastModified() = ${it.lastModified()}")
                        }
                        Paintbox.LOGGER.info("Deleted all recovery files")
                    } catch (s: SecurityException) {
                        s.printStackTrace()
                    }
                }
            }
        }
        val resetEndlessModeHighScoreButton: Button = createLongButton { 
            Localization.getVar("mainMenu.dataSettings.resetEndlessModeHighScore").use() 
        }.apply {
            this.setOnAction {
                settings.endlessHighScore.set(EndlessHighScore.ZERO)
                settings.persist()
                this.disabled.set(true)
            }
        }
        val resetLibraryHighScoresButton: Button = createLongButton { 
            Localization.getVar("mainMenu.dataSettings.resetLibraryHighScores").use() 
        }.apply {
            this.setOnAction {
                GlobalScoreCache.clearAll()
                this.disabled.set(true)
            }
        }
        val resetSideModeHighScoresButton: Button = createLongButton { 
            Localization.getVar("mainMenu.dataSettings.resetSideModeHighScores").use() 
        }.apply {
            this.setOnAction {
                settings.endlessDunkHighScore.set(0)
                settings.sidemodeAssembleHighScore.set(0)
                settings.persist()
                this.disabled.set(true)
            }
        }
        val resetLibraryFolderButton: Button = createLongButton {
            Localization.getVar("mainMenu.dataSettings.resetLibraryFolder").use()
        }.apply {
            this.setOnAction {
                main.persistDirectory(PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW, PRMania.DEFAULT_LEVELS_FOLDER)
                menuCol.libraryMenu.interruptSearchThread()
                this.disabled.set(true)
            }
        }
        val confirmResettingAchievementsButton: Button = createLongButton { Localization.getVar("mainMenu.achievementsStatsFork.delete").use() }.apply {
            this.setOnAction {
                menuCol.pushNextMenu(ConfirmResettingAchievementsMenu(menuCol).also { menuCol.addMenu(it) })
            }
        }
        
        vbox.temporarilyDisableLayouts {
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 0f, 0f))
                }
            }

            val (drpcPane, drpcCheckbox) = createCheckboxOption({Localization.getVar("mainMenu.dataSettings.discordRichPresence").use()})
            drpcCheckbox.checkedState.set(settings.discordRichPresence.getOrCompute())
            drpcCheckbox.onCheckChanged = { newState ->
                settings.discordRichPresence.set(newState)
            }
            drpcCheckbox.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.dataSettings.discordRichPresence.tooltip")))
            vbox += drpcPane
            
            vbox += separator()
            
            vbox += createLongButton {
                Localization.getVar("mainMenu.dataSettings.openLogsFolder").use()
            }.apply {
                this.setOnAction {
                    val uri = SysOutPiper.logFile.parentFile.toURI()
                    Gdx.net.openURI(uri.toString())
                }
            }
            vbox += deleteRecoveryButton
            vbox += resetLibraryFolderButton
            vbox += resetEndlessModeHighScoreButton
            vbox += resetSideModeHighScoresButton
            vbox += resetLibraryHighScoresButton
            vbox += confirmResettingAchievementsButton
            
        }
        
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                    Gdx.app.postRunnable { 
                        deleteRecoveryButton.disabled.set(false)
                        resetSideModeHighScoresButton.disabled.set(false)
                        resetEndlessModeHighScoreButton.disabled.set(false)
                        resetLibraryHighScoresButton.disabled.set(false)
                        resetLibraryFolderButton.disabled.set(false)
                    }
                }
            }
        }
    }

}