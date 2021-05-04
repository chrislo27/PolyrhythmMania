package io.github.chrislo27.paintbox.ui.contextmenu

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.ui.control.Control
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory


/**
 * A [ContextMenu] is a container of [MenuItem]. When shown, it computes the widths and heights of
 * its [MenuItem] children and lays them out.
 * 
 * To add a root context menu, call [SceneRoot.showRootContextMenu]. To add a child menu, call [addChildMenu].
 * 
 * A [ContextMenu] can spawn more sub-menus through the [Menu] menu item. As such, the [childMenu] will be set to the
 * new sub-menu and the child's [parentMenu] will be set to the parent menu.
 */
open class ContextMenu : Control<ContextMenu>() {
    
    companion object {
        const val SKIN_ID: String = "ContextMenu"
        
        init {
            DefaultSkins.register(SKIN_ID, SkinFactory { element: ContextMenu ->
                element.ContextMenuSkin(element)
            })
        }
    }
    
    val parentMenu: Var<ContextMenu?> = Var(null)
    val childMenu: Var<ContextMenu?> = Var(null)
    
    var menuItems: List<MenuItem> = emptyList()
        private set

    /**
     * The list of [MenuItem]s that are currently displayed. Updated as part of [computeSize].
     */
    private var activeMenuItems: List<MenuItemMetadata> = emptyList()
    
    /**
     * Called by [SceneRoot] to compute the bounds of this context menu.
     */
    fun computeSize() {
        val currentItems = menuItems
        // TODO
        activeMenuItems = currentItems.map { MenuItemMetadata(it) }
    }

    fun addMenuItem(child: MenuItem) {
        if (child !in menuItems) {
            menuItems = menuItems + child
        }
    }

    fun removeMenuItem(child: MenuItem) {
        if (child in menuItems) {
            menuItems = menuItems - child
        }
    }
    
    /**
     * Adds the child menu to the scene and also connects the parent-child relationship.
     */
    fun addChildMenu(child: ContextMenu) {
        removeChildMenu()
        // Order of relationship changes should be in this order exactly: this.childMenu, child.parentMenu, sceneRoot
        // The children will also NOT be added, they have to be added later using addChildMenu
        childMenu.set(child)
        child.parentMenu.set(this)
        this.sceneRoot.getOrCompute()?.addContextMenuToScene(child)
    }

    /**
     * Removes the child menu from the scene and disconnects the parent-child relationship.
     */
    fun removeChildMenu() {
        val child = childMenu.getOrCompute()
        if (child != null) {
            // Order of relationship changes should be in this order exactly: sceneRoot, child.parentMenu, this.childMenu
            child.removeChildMenu()
            this.sceneRoot.getOrCompute()?.removeContextMenuFromScene(child)
            child.parentMenu.set(null)
            childMenu.set(null)
        }
    }

    override fun getDefaultSkinID(): String = ContextMenu.SKIN_ID
    
    open inner class ContextMenuSkin(element: ContextMenu) : Skin<ContextMenu>(element) {

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        }

        override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
}

data class MenuItemMetadata(val menuItem: MenuItem)