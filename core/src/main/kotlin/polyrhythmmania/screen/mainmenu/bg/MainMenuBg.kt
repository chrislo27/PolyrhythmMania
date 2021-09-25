package polyrhythmmania.screen.mainmenu.bg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import paintbox.util.gdxutils.drawQuad
import polyrhythmmania.container.Container
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.screen.mainmenu.EntityAsmWidgetHovering
import polyrhythmmania.screen.mainmenu.EntityCubeHovering
import polyrhythmmania.screen.mainmenu.MainMenuScreen
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.entity.EntityCube
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.entity.EntityPlatform
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.WorldRenderer


class MainMenuBg(val mainMenu: MainMenuScreen) {
    
    private val container: Container = Container(null, SimpleTimingProvider { 
        Gdx.app.postRunnable { throw it } 
        false
    }, GlobalContainerSettings(ForceTexturePack.FORCE_GBA))
    private val gradientStart: Color = Color(0f, 32f / 255f, 55f / 255f, 1f)
    private val gradientEnd: Color = Color.BLACK.cpy()
    
    private val world: World = container.world
    val renderer: WorldRenderer = container.renderer
    
    init {
        renderer.camera.position.x = -2f
        renderer.camera.position.y = 0.5f

        world.clearEntities()
        initializeNormal()
    }
    
    fun initializeFromType(type: BgType) {
        return when (type) {
            BgType.NORMAL, BgType.PRACTICE_NORMAL -> initializeNormal()
            BgType.DUNK -> initializeDunk()
            BgType.ENDLESS -> initializeEndless()
            BgType.ASSEMBLE -> initializeAssemble()
        }
    }
    
    private fun initializeNormal() {
        val world = this.world
        world.clearEntities()

        for (x in 0 until 7) {
            for (z in -2..0) {
                world.addEntity(EntityCube(world, withLine = false, withBorder = z == 0).apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
            val y = 1f + MathUtils.FLOAT_ROUNDING_ERROR * 1
            if (x == 0) {
                world.addEntity(EntityPiston(world).apply {
                    this.type = EntityPiston.Type.PISTON_A
                    this.position.set(x.toFloat(), y, -1f)
                })
            } else {
                world.addEntity(EntityPlatform(world).apply {
                    this.position.set(x.toFloat(), y, -1f)
                })
            }
        }

        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-2f, 2f, -3f)
        })
        world.addEntity(EntityCubeHovering(world, withLine = true).apply {
            this.position.set(2f, 2f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(6f, 0f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-3.5f, 1f, 0f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(0f, -2f, 1f)
        })
    }
    
    private fun initializeDunk() {
        val world = this.world
        world.clearEntities()

        // Hoop
        for (y in 1..5) {
            val ent: Entity = EntityPlatform(world, false)
            world.addEntity(ent.apply {
                this.position.set(1f, y.toFloat() - 1, -1f)
            })
        }
        world.addEntity(EntityDunkBacking(world).apply {
            this.position.set(1f, 6f - 1, -1f)
        })
        world.addEntity(EntityDunkBasketRear(world).apply {
            this.position.set(0f + 1, 5f - 1 - 1, -1f - 1)
        })
        world.addEntity(EntityDunkBasketFront(world).apply {
            this.position.set(0f, 5f - 1, -1f)
        })
        world.addEntity(EntityDunkBasketFrontFaceZ(world).apply {
            this.position.set(0f, 5f - 1, 0f)
        })
        world.addEntity(EntityDunkBasketBack(world).apply {
            this.position.set(1f, 5f - 1, -1f)
        })
        for (x in -1..1) {
            for (z in -1..1) {
                if (x == 0 && z == 0) continue
                if (x == -1 && z == 0) continue
                world.addEntity(EntityCube(world, withBorder = x == 0 && z == 1).apply {
                    this.position.set(1f + x, 0f, -1f + z)
                })
            }
        }
        

        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-2f, 2f, -3f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(2f, 2f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(6f, 0f, -4f)
        })
        world.addEntity(EntityCubeHovering(world, withLine = true).apply {
            this.position.set(-3.5f, 1f, 0f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(0f, -2f, 1f)
        })
    }

    
    private fun initializeEndless() {
        val world = this.world
        world.clearEntities()

        for (x in 0 until 7) {
            for (z in -2..0) {
                world.addEntity(EntityCube(world, withLine = false, withBorder = z == 0).apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
            val y = 1f + MathUtils.FLOAT_ROUNDING_ERROR * 1
            if (x % 2 == 0) {
                world.addEntity(EntityPiston(world).apply {
                    this.type = EntityPiston.Type.PISTON_DPAD
                    this.position.set(x.toFloat(), y, -1f)
                })
            } else {
                world.addEntity(EntityPlatform(world).apply {
                    this.position.set(x.toFloat(), y, -1f)
                })
            }
        }

        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-2f, 2f, -3f)
        })
        world.addEntity(EntityCubeHovering(world, withLine = true).apply {
            this.position.set(2f, 2f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(6f, 0f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-3.5f, 1f, 0f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(0f, -2f, 1f)
        })
    }

    private fun initializeAssemble() {
        val world = this.world
        world.clearEntities()

        for (x in 0 until 7) {
            for (z in -2..0) {
                world.addEntity(EntityCube(world, withLine = false, withBorder = z == 0).apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
            val y = 1f + MathUtils.FLOAT_ROUNDING_ERROR * 1
            world.addEntity(when (x) {
                2 -> EntityPistonAsm(world).apply {
                    this.type = EntityPiston.Type.PISTON_A
                    this.position.set(x.toFloat(), y, -1f)
                    this.tint = EntityPistonAsm.playerTint.cpy()
                }
                in 0..3 -> EntityPistonAsm(world).apply {
                    this.type = EntityPiston.Type.PISTON_DPAD
                    this.position.set(x.toFloat(), y, -1f)
                }
                else -> EntityPlatform(world).apply {
                    this.position.set(x.toFloat(), y, -1f)
                }
            })
        }


        world.addEntity(EntityAsmWidgetHovering(world).apply {
            this.position.set(-2f, 2f, -3f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(2f, 2f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(6f, 0f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-3.5f, 1f, 0f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(0f, -2f, 1f)
        })
    }
    
    fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        // Render background
        batch.projectionMatrix = camera.combined
        batch.begin()

        batch.drawQuad(-400f, 0f, gradientEnd, camera.viewportWidth, 0f, gradientEnd, camera.viewportWidth,
                camera.viewportHeight, gradientStart, -400f, camera.viewportHeight + 400f, gradientStart)

        batch.end()

        // Render world
        container.renderer.render(batch)
    }
}