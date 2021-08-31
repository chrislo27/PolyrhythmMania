package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.Paintbox
import paintbox.logging.SysOutPiper
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.Settings
import polyrhythmmania.container.Container
import polyrhythmmania.discord.DiscordCore
import polyrhythmmania.sidemodes.endlessmode.EndlessHighScore
import polyrhythmmania.ui.PRManiaSkins


class DataSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.dataSettings.title").use() }
        this.contentPane.bounds.height.set(300f)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
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
            this.bounds.height.set(300f)
            this.spacing.set(0f)
        }

        val deleteRecoveryButton: Button = createLongButton { 
            Localization.getVar("mainMenu.dataSettings.deleteAllRecovery").use()
        }.apply {
            this.setOnAction {
                this.disabled.set(true)
                Gdx.app.postRunnable {
                    try {
                        PRMania.RECOVERY_FOLDER.listFiles()?.filter { f ->
                            f != null && f.isFile && f.extension == Container.FILE_EXTENSION
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
        
        vbox.temporarilyDisableLayouts {
            vbox += createLongButton {
                Localization.getVar("mainMenu.dataSettings.openLogsFolder").use()
            }.apply {
                this.setOnAction {
                    val uri = SysOutPiper.logFile.parentFile.toURI()
                    Gdx.net.openURI(uri.toString())
                }
            }
            vbox += deleteRecoveryButton
            vbox += resetEndlessModeHighScoreButton
            vbox += resetSideModeHighScoresButton
            
            val (drpcPane, drpcCheckbox) = createCheckboxOption({Localization.getVar("mainMenu.dataSettings.discordRichPresence").use()})
            drpcCheckbox.checkedState.set(settings.discordRichPresence.getOrCompute())
            drpcCheckbox.onCheckChanged = { newState ->
                settings.discordRichPresence.set(newState)
            }
            vbox += drpcPane
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
                    }
                }
            }
        }
    }

}