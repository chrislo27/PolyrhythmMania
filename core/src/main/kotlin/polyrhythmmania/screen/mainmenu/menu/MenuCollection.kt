package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.audio.Sound
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.Corner
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.util.RectangleStack
import paintbox.util.Vector2Stack
import paintbox.util.gdxutils.maxX
import paintbox.util.gdxutils.maxY
import polyrhythmmania.PRManiaGame
import polyrhythmmania.Settings
import polyrhythmmania.screen.mainmenu.MainMenuScreen
import java.lang.Float.max
import java.lang.Float.min
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor


class MenuCollection(val mainMenu: MainMenuScreen, val sceneRoot: SceneRoot, val menuPane: Pane) {
    
    val main: PRManiaGame = mainMenu.main
    val settings: Settings = main.settings
    
    val menus: List<MMMenu> = mutableListOf()
    val activeMenu: ReadOnlyVar<MMMenu?> = Var(null)
    
    private val menuStack: Deque<MMMenu> = ArrayDeque()
    
    val uppermostMenu: UppermostMenu = UppermostMenu(this)
    val quitMenu: QuitMenu = QuitMenu(this)
    val creditsMenu: CreditsMenu = CreditsMenu(this)
    val dailyChallengeMenu: DailyChallengeMenu = DailyChallengeMenu(this) // Must be inited before playMenu
    val playMenu: PlayMenu = PlayMenu(this)
    val practiceMenu: PracticeMenu = PracticeMenu(this)
    val sideModesMenu: PlaySideModesMenu = PlaySideModesMenu(this)
    val endlessMenu: EndlessModeMenu = EndlessModeMenu(this)
    val settingsMenu: SettingsMenu = SettingsMenu(this)
    val audioSettingsMenu: AudioSettingsMenu = AudioSettingsMenu(this)
    val advancedAudioMenu: AdvAudioMenu = AdvAudioMenu(this)
    val videoSettingsMenu: VideoSettingsMenu = VideoSettingsMenu(this)
    val inputSettingsMenu: InputSettingsMenu = InputSettingsMenu(this)
    val dataSettingsMenu: DataSettingsMenu = DataSettingsMenu(this)
    val languageMenu: LanguageMenu = LanguageMenu(this)
    val calibrationSettingsMenu: CalibrationSettingsMenu = CalibrationSettingsMenu(this)
    
    init {
        addStockMenus()
        
        changeActiveMenu(uppermostMenu, false, instant = true)
        menuStack.push(uppermostMenu)
    }
    
    private fun addStockMenus() {
        addMenu(uppermostMenu)
        addMenu(quitMenu)
        addMenu(creditsMenu)
        addMenu(dailyChallengeMenu)
        addMenu(playMenu)
        addMenu(practiceMenu)
        addMenu(sideModesMenu)
        addMenu(endlessMenu)
        addMenu(settingsMenu)
        addMenu(videoSettingsMenu)
        addMenu(audioSettingsMenu)
        addMenu(advancedAudioMenu)
        addMenu(inputSettingsMenu)
        addMenu(dataSettingsMenu)
        addMenu(languageMenu)
        addMenu(calibrationSettingsMenu)
    }
    
    fun addMenu(menu: MMMenu) {
        menus as MutableList
        menus.add(menu)
        
        menu.visible.set(false)
        menuPane.addChild(menu)
        Anchor.BottomLeft.configure(menu)
    }
    
    fun removeMenu(menu: MMMenu) {
        menus as MutableList
        menus.remove(menu)
        menuPane.removeChild(menu)
    }
    
    fun resetMenuStack() {
        menuStack.clear()
        menuStack.push(uppermostMenu)
    }
    
    fun changeActiveMenu(menu: MMMenu, backOut: Boolean, instant: Boolean = false, playSound: Boolean = true) {
        if (!instant) {
            if (playSound) {
                main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_${if (backOut) "deselect" else "select"}"))
            }
            
            val changedBounds = RectangleStack.getAndPush().apply {
                val currentBounds = menu.bounds
                val relToRoot = menu.getPosRelativeToRoot(Vector2Stack.getAndPush())
                this.set(relToRoot.x, relToRoot.y,
                        currentBounds.width.get(), currentBounds.height.get())
                Vector2Stack.pop()
            }
            
            val currentActive = activeMenu.getOrCompute()
            if (currentActive != null) {
                val secondBounds = RectangleStack.getAndPush()
                val curActiveBounds = currentActive.bounds
                val relToRoot = currentActive.getPosRelativeToRoot(Vector2Stack.getAndPush())
                secondBounds.set(relToRoot.x, relToRoot.y,
                        curActiveBounds.width.get(), curActiveBounds.height.get())
                Vector2Stack.pop()
                
                // Merge the two rectangles to be maximal.
                changedBounds.x = min(changedBounds.x, secondBounds.x)
                changedBounds.y = min(changedBounds.y, secondBounds.y)
                changedBounds.width = max(changedBounds.maxX, secondBounds.maxX) - changedBounds.x
                changedBounds.height = max(changedBounds.maxY, secondBounds.maxY) - changedBounds.y
                
                RectangleStack.pop()
            }
            
            val rootWidth = 1280f
            val rootHeight = 720f
            val tileX = floor(changedBounds.x / rootWidth * mainMenu.tilesWidth).toInt()
            val tileY = floor(changedBounds.y / rootHeight * mainMenu.tilesHeight).toInt()
            val tileW = (ceil(changedBounds.maxX / rootWidth * mainMenu.tilesWidth).toInt() - tileX).coerceAtLeast(1)
            val tileH = (ceil(changedBounds.maxY / rootHeight * mainMenu.tilesHeight).toInt() - tileY).coerceAtLeast(1)
            mainMenu.requestTileFlip(MainMenuScreen.TileFlip(tileX, tileY, tileW, tileH,
                    if (backOut) Corner.TOP_RIGHT else Corner.TOP_LEFT))
            
            RectangleStack.pop()
        }
        menus.forEach { if (it !== menu) it.visible.set(false) }
        menu.visible.set(true)
        (activeMenu as Var).set(menu)
    }
    
    fun pushNextMenu(menu: MMMenu, instant: Boolean = false, playSound: Boolean = true) {
        changeActiveMenu(menu, false, instant, playSound)
        menuStack.push(menu)
    }

    fun popLastMenu(instant: Boolean = false, playSound: Boolean = true): MMMenu {
        if (menuStack.size <= 1) return menuStack.peek()
        val popped = menuStack.pop()
        val menu = menuStack.peek()
        changeActiveMenu(menu, true, instant, playSound)
        if (popped.deleteWhenPopped.getOrCompute()) {
            this.removeMenu(popped)
        }
        return popped
    }

    fun playBlipSound(volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f) {
        val sound = AssetRegistry.get<Sound>("sfx_menu_blip")
        sound.stop()
        main.playMenuSfx(sound)
    }
    
    fun playMenuSound(id: String, volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Pair<Sound, Long> {
        val sound: Sound = AssetRegistry[id]
        return sound to main.playMenuSfx(sound, volume, pitch, pan)
    }
}