package polyrhythmmania.achievements.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.binding.FloatVar
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.animation.Animation
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaGame
import polyrhythmmania.achievements.*
import java.time.Instant


class AchievementsUIOverlay {
    
    companion object {
        private const val MAX_TOASTS_ON_SCREEN: Int = 3
    }
    
    private class ActiveToast(val toast: Toast) {
        
        val showPercentage: FloatVar = FloatVar(0f)
        val containingPane: Pane = Pane()
        
        init {
            Anchor.TopRight.configure(containingPane)
            containingPane.doClipping.set(true)
            containingPane.bounds.width.bind { toast.bounds.width.use() }
            containingPane.bounds.height.bind { toast.bounds.height.use() * showPercentage.use() }
            
            containingPane += toast
            Anchor.BottomLeft.configure(toast)
        }
        
        fun frameUpdate(list: List<ActiveToast>, thisIndex: Int) {
            val heightsBefore = if (thisIndex == 0) 0f else (list[thisIndex - 1].containingPane.let { it.bounds.y.get() + it.bounds.height.get() })
            containingPane.bounds.y.set(heightsBefore)
        }
    }
    
    private val tmpMatrix: Matrix4 = Matrix4()
    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    private val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    
    private val queue: MutableList<Toast> = mutableListOf()
    private val activeToasts: MutableList<ActiveToast> = mutableListOf()
    private val activeToastsReversed: List<ActiveToast> = activeToasts.asReversed()
    
    init {
        Achievements.fulfillmentListeners += Achievements.FulfillmentListener { ach, ful ->
            enqueueToast(Toast(ach, ful))
        }
    }
    
    fun debugReloadToast() {
        enqueueToast(Toast(Achievement.Ordinary("test", AchievementRank.OBJECTIVE, AchievementCategory.GENERAL, false), Fulfillment(Instant.now())))
    }
    
    fun render(main: PRManiaGame, batch: SpriteBatch) {
        tmpMatrix.set(batch.projectionMatrix)
        batch.projectionMatrix = uiCamera.combined
        sceneRoot.renderAsRoot(batch)

        val mouseX = sceneRoot.mousePosition.x.get()
        val mouseY = sceneRoot.mousePosition.y.get()
        // Note: This works because all toasts are immediate children of the SceneRoot.
        val mouseInAny = !Gdx.input.isCursorCatched && activeToasts.any { at -> at.containingPane.bounds.containsPointLocal(mouseX, mouseY) }
        sceneRoot.opacity.set(if (mouseInAny) 0.25f else 1f)
        
        batch.projectionMatrix.set(tmpMatrix)
        
        while (queue.isNotEmpty() && activeToasts.size < MAX_TOASTS_ON_SCREEN) {
            val next = queue.removeFirst()
            activeToasts += ActiveToast(next).also { activeToast ->
                sceneRoot += activeToast.containingPane
                val interpolation = Interpolation.smoother
                val animationDur = 0.35f
                val showTime = 5f
                sceneRoot.animations.enqueueAnimation(Animation(interpolation, animationDur, 0f, 1f).apply { 
                    onComplete = {
                        // Post animation going back up
                        Gdx.app.postRunnable {
                            sceneRoot.animations.enqueueAnimation(Animation(interpolation, animationDur, 1f, 0f, delay = showTime).apply {
                                onComplete = {
                                    Gdx.app.postRunnable {
                                        activeToasts.remove(activeToast)
                                        sceneRoot -= activeToast.containingPane
                                    }
                                }
                            }, activeToast.showPercentage) 
                        }
                    }
                }, activeToast.showPercentage)
            }
        }
        
        activeToastsReversed.forEachIndexed { index, toast -> 
            toast.frameUpdate(activeToastsReversed, index)
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) { // FIXME remove
            repeat(if (Gdx.input.isShiftDown()) 3 else 1) {
                debugReloadToast()
            }
        }
    }

    fun resize(width: Int, height: Int) {
        uiViewport.update(width, height)
    }
    
    fun enqueueToast(toast: Toast) {
        queue += toast
    }
}