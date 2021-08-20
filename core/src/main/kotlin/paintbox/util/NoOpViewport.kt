package paintbox.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.utils.viewport.Viewport


/**
 * A [Viewport] that doesn't adjust the camera at all.
 */
class NoOpViewport(val camera: OrthographicCamera) : Viewport() {
    
    init {
        setCamera(camera)
        setWorldSize(camera.viewportWidth, camera.viewportHeight)
        setScreenBounds()
    }
    
    override fun apply(centerCamera: Boolean) {
        setScreenBounds()
        HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        setScreenBounds()
        setWorldSize(camera.viewportWidth, camera.viewportHeight)
        apply(centerCamera)
    }

    override fun setCamera(camera: Camera?) {
        if (camera !== this.camera) return
        super.setCamera(camera)
    }
    
    private fun setScreenBounds() {
        setScreenBounds(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun unproject(screenCoords: Vector2?): Vector2 {
        setScreenBounds()
        return super.unproject(screenCoords)
    }

    override fun unproject(screenCoords: Vector3?): Vector3 {
        setScreenBounds()
        return super.unproject(screenCoords)
    }

    override fun project(worldCoords: Vector2?): Vector2 {
        setScreenBounds()
        return super.project(worldCoords)
    }

    override fun project(worldCoords: Vector3?): Vector3 {
        setScreenBounds()
        return super.project(worldCoords)
    }

    override fun getPickRay(screenX: Float, screenY: Float): Ray {
        setScreenBounds()
        return super.getPickRay(screenX, screenY)
    }
}
