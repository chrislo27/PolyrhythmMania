package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import io.github.chrislo27.paintbox.registry.AssetRegistry
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.beads.SimpleTimingProvider
import polyrhythmmania.beads.TimingProvider
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World


class TestWorldRenderScreen(main: PRManiaGame) : PRManiaScreen(main) {
    
    val world: World = World()
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, GBATileset(AssetRegistry["tileset_gba"]))
    }
    val timing: TimingProvider = SimpleTimingProvider()
    val engine: Engine = Engine(timing, world)

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        val batch = main.batch
        
        renderer.render(batch)
        
        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        timing.seconds += Gdx.graphics.deltaTime
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            main.screen = TestWorldRenderScreen(main)
            Gdx.app.postRunnable { 
                this.dispose()
            }
        }


        // FIXME debug, remove later
        val camera = renderer.camera
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.position.y += Gdx.graphics.deltaTime * +4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.position.y += Gdx.graphics.deltaTime * -4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.position.x += Gdx.graphics.deltaTime * +4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.position.x += Gdx.graphics.deltaTime * -4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            camera.zoom += Gdx.graphics.deltaTime * -1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom += Gdx.graphics.deltaTime * +1f
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            camera.setToOrtho(false, 5 * (16f / 9f), 5f)
            camera.zoom = 1f
            camera.position.set(camera.zoom * camera.viewportWidth / 2.0f, camera.zoom * camera.viewportHeight / 2.0f, 0f)
        }
    }

    override fun getDebugString(): String {
        return """${engine.getDebugString()}
---
${renderer.getDebugString()}
"""
        
    }

    override fun dispose() {
    }
}