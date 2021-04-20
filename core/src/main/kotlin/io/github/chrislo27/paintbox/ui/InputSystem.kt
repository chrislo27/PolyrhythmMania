package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.util.sumByFloat


class InputSystem(private val sceneRoot: SceneRoot) : InputProcessor {

    private val vector: Vector2 = Vector2(0f, 0f)
    private val clickPressedList: MutableMap<Int, ClickPressedState> = mutableMapOf()

    /**
     * Represents the mouse x/y in UI space.
     */
    val mouseVector: Vector2
        get() = vector


    private fun dispatchEventBasedOnMouse(layer: SceneRoot.Layer, evt: InputEvent): UIElement? {
        val lastPath = layer.lastHoveredElementPath
        for (element in lastPath) {
            if (element.fireEvent(evt)) return element
        }
        return null
    }

    private fun dispatchEventBasedOnMouse(evt: InputEvent): Pair<SceneRoot.Layer, UIElement>? {
        for (layer in sceneRoot.allLayersReversed) {
            val result = dispatchEventBasedOnMouse(layer, evt)
            if (result != null) {
                return layer to result
            }
        }
        return null
    }

    private fun UIElement.fireEvent(event: InputEvent): Boolean {
        val listeners = this.inputListeners.getOrCompute()
        for (l in listeners) {
            if (l.handle(event)) return true
        }
        return false
    }

//    private fun dispatchEvent(evt: InputEvent): Boolean {
//        val lastPath = lastHoveredElementPath
//        for (element in lastPath) {
//            val consumed: Boolean = element.listeners.getOrCompute().any { it.handle(evt) }
//            if (consumed) return true
//        }
//        return false
//    }

    private fun updateDeepmostElementForMouseLocation(layer: SceneRoot.Layer, x: Float, y: Float) {
        val lastPath: MutableList<UIElement> = layer.lastHoveredElementPath
        if (lastPath.isEmpty()) {
            val newPath = layer.root.pathToForInput(x, y)
            lastPath.addAll(newPath)
            tooltipMouseEntered(newPath, x, y)
            val evt = MouseEntered(x, y)
            newPath.forEach {
                it.fireEvent(evt) 
            }
            return
        }

        // Backtrack from last element to find the closest element containing the position, and then
        // find the deepest element starting from there.
        // Note that if the current last element is already the deepest element that contains x,y
        // then the rest of the code does nothing, achieving maximum performance.

        var cursor: UIElement? = lastPath.lastOrNull()
        var offX: Float = lastPath.sumByFloat { it.bounds.x.getOrCompute() }
        var offY: Float = lastPath.sumByFloat { it.bounds.y.getOrCompute() }
        if (cursor != null) {
            offX -= cursor.bounds.x.getOrCompute()
            offY -= cursor.bounds.y.getOrCompute()
        }
        // offsets should be the absolute x/y of the parent of cursor
        while (cursor != null && !cursor.borderZone.containsPointLocal(x - offX, y - offY)) {
            val removed = lastPath.removeLast()
            onMouseExited(removed, x, y)
            removed.fireEvent(MouseExited(x, y))
            cursor = lastPath.lastOrNull()
            if (cursor != null) {
                offX -= cursor.bounds.x.getOrCompute()
                offY -= cursor.bounds.y.getOrCompute()
            }
        }
        // We found the closest parent that contains x, y, so we'll navigate to its deepest descendant that contains xy
        // starting from it, and the resulting "subpath" will be appended to our current path
        if (cursor != null && cursor.children.isNotEmpty()) {
            val subPath = cursor.pathToForInput(x - offX, y - offY)
            lastPath += subPath
            tooltipMouseEntered(subPath, x, y)
            val evt = MouseEntered(x, y)
            subPath.forEach {
                it.fireEvent(evt)
            }
        }
    }

    private fun updateDeepmostElementForMouseLocation(x: Float, y: Float) {
        sceneRoot.allLayersReversed.forEach { layer ->
            updateDeepmostElementForMouseLocation(layer, x, y)
        }
    }

    private fun tooltipMouseEntered(path: List<UIElement>, x: Float, y: Float) {
        for (element in path.asReversed()) {
            if (element is HasTooltip) {
                val tooltipElement = element.tooltipElement.getOrCompute()
                if (tooltipElement != null) {
                    sceneRoot.startTooltip(element, element.tooltipElement)
                    break
                }
            }
        }
    }
    
    private fun onMouseExited(element: UIElement, x: Float, y: Float) {
        val currentTooltipElement = sceneRoot.currentElementWithTooltip.getOrCompute()
        if (currentTooltipElement != null && element === currentTooltipElement) {
            // The element the mouse was over should no longer show its tooltip
            sceneRoot.cancelTooltip()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
//        return dispatchEvent(KeyDown(keycode))
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
//        return dispatchEvent(KeyUp(keycode))
    }

    override fun keyTyped(character: Char): Boolean {
        return false
//        return dispatchEvent(KeyTyped(character))
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val vec: Vector2 = sceneRoot.screenToUI(vector.set(screenX.toFloat(), screenY.toFloat()))
        updateDeepmostElementForMouseLocation(vec.x, vec.y)

        val touch = dispatchEventBasedOnMouse(TouchDown(vec.x, vec.y, button, pointer))
        val click = dispatchEventBasedOnMouse(ClickPressed(vec.x, vec.y, button))
        val allLayersPaths: Map<SceneRoot.Layer, List<UIElement>> = sceneRoot.allLayers.associateWith { it.lastHoveredElementPath.toList() }
        clickPressedList[button] = ClickPressedState(allLayersPaths, click)

        return touch != null || click != null
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val vec: Vector2 = sceneRoot.screenToUI(vector.set(screenX.toFloat(), screenY.toFloat()))
        updateDeepmostElementForMouseLocation(vec.x, vec.y)

        val touch = dispatchEventBasedOnMouse(TouchUp(vec.x, vec.y, button, pointer))

        var anyClick = false
        val previousClick = clickPressedList[button]
        if (previousClick != null) {
            clickPressedList.remove(button)
            sceneRoot.allLayersReversed.forEach { layer ->
                val lastHoveredElementPath = previousClick.lastHoveredElementPathPerLayer.getValue(layer)
                lastHoveredElementPath.forEach {
                    anyClick = it.fireEvent(ClickReleased(vec.x, vec.y, button, it === previousClick.accepted?.second, it in lastHoveredElementPath)) || anyClick
                }
            }
        }

        return touch != null || anyClick
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val vec: Vector2 = sceneRoot.screenToUI(vector.set(screenX.toFloat(), screenY.toFloat()))
        updateDeepmostElementForMouseLocation(vec.x, vec.y)
        return dispatchEventBasedOnMouse(TouchDragged(vec.x, vec.y, pointer)) != null
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val vec: Vector2 = sceneRoot.screenToUI(vector.set(screenX.toFloat(), screenY.toFloat()))
        updateDeepmostElementForMouseLocation(vec.x, vec.y)
        return dispatchEventBasedOnMouse(MouseMoved(vec.x, vec.y)) != null
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
//        return dispatchEvent(Scrolled(amountX, amountY))
    }

    private data class ClickPressedState(val lastHoveredElementPathPerLayer: Map<SceneRoot.Layer, List<UIElement>>,
                                         val accepted: Pair<SceneRoot.Layer, UIElement>?)
}
