package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import io.github.chrislo27.paintbox.registry.AssetRegistry
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.world.World


class TestWorldRenderScreen(main: PRManiaGame) : PRManiaScreen(main) {

    val world = World()
    val renderer by lazy {
        WorldRenderer(world, GBATileset(AssetRegistry["tileset_gba"]))
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        val batch = main.batch
        
        renderer.render(batch)
        
        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            main.screen = TestWorldRenderScreen(main)
            Gdx.app.postRunnable { 
                this.dispose()
            }
        }
    }

    override fun getDebugString(): String {
        return """e: ${world.entities.size}"""
    }

    override fun dispose() {
    }
}