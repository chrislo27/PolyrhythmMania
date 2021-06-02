package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.util.gdxutils.maxX
import io.github.chrislo27.paintbox.util.gdxutils.maxY
import polyrhythmmania.screen.mainmenu.MainMenuScreen
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.ceil
import kotlin.math.floor


class MenuCollection(val mainMenu: MainMenuScreen, val sceneRoot: SceneRoot, val menuPane: Pane) {
    
    val menus: List<MMMenu> = mutableListOf()
    val activeMenu: ReadOnlyVar<MMMenu?> = Var(null)
    
    val uppermostMenu: UppermostMenu = UppermostMenu(this)
    val quitMenu: QuitMenu = QuitMenu(this)
    
    init {
        addMenu(uppermostMenu)
        addMenu(quitMenu)
        
        changeActiveMenu(uppermostMenu, false, instant = true)
    }
    
    fun addMenu(menu: MMMenu) {
        menus as MutableList
        menus.add(menu)
        
        menu.visible.set(false)
        menuPane.addChild(menu)
        Anchor.BottomLeft.configure(menu)
    }
    
    fun changeActiveMenu(menu: MMMenu, backOut: Boolean, instant: Boolean = false) {
        if (!instant) {
            val changedBounds = RectangleStack.getAndPush().apply {
                val currentBounds = menu.bounds
                val relToRoot = menu.getPosRelativeToRoot(Vector2())
                this.set(relToRoot.x, relToRoot.y,
                        currentBounds.width.getOrCompute(), currentBounds.height.getOrCompute())
            }
            
            val currentActive = activeMenu.getOrCompute()
            if (currentActive != null) {
                val secondBounds = RectangleStack.getAndPush()
                val curActiveBounds = currentActive.bounds
                val relToRoot = currentActive.getPosRelativeToRoot(Vector2())
                secondBounds.set(relToRoot.x, relToRoot.y,
                        curActiveBounds.width.getOrCompute(), curActiveBounds.height.getOrCompute())
                
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
            val tileW = (ceil(changedBounds.width / rootWidth * mainMenu.tilesWidth).toInt()).coerceAtLeast(1)
            val tileH = (ceil(changedBounds.height / rootHeight * mainMenu.tilesHeight).toInt() + 1).coerceAtLeast(1)
            mainMenu.flipAnimation = MainMenuScreen.TileFlip(tileX, tileY, tileW, tileH,
                    if (backOut) Corner.TOP_RIGHT else Corner.TOP_LEFT)
            
            RectangleStack.pop()
        }
        menus.forEach { it.visible.set(false) }
        menu.visible.set(true)
        (activeMenu as Var).set(menu)
    }
}