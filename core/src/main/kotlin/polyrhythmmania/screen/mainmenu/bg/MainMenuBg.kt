package polyrhythmmania.screen.mainmenu.bg

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Disposable
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.drawQuad
import polyrhythmmania.screen.mainmenu.*
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.entity.EntityCube
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.entity.EntityPlatform
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.texturepack.*
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TilesetPalette
import polyrhythmmania.world.tileset.TintedRegion


class MainMenuBg(val mainMenu: MainMenuScreen) : Disposable {
    
    private inner class EntityCubeMM(withBorder: Boolean = false, val showLeftVerticalEdge: Boolean = false)
        : EntityCube(this@MainMenuBg.world, false, withBorder) {
        
        override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
            if (!showLeftVerticalEdge) { // This is "not" because the title texreg variants extend past the edge
                when (index) {
                    1 -> return titleTintedRegions["title_cube_border_z"]
                    2 -> return titleTintedRegions["title_cube_face_x"]
                }
            }
            return super.getTintedRegion(tileset, index)
        }
    }

    private val gradientStart: Color = Color(0f, 32f / 255f, 55f / 255f, 1f)
    private val gradientEnd: Color = Color.BLACK.cpy()
    
    private val world: World = World()
    private val texPack: CascadingTexturePack by lazy {
        CascadingTexturePack("gba_titleScreen", emptySet(), listOf(
                object : TexturePack("gba_titleScreen_noFallback", emptySet()) {
                    init {
                        val regionMap = AssetRegistry.get<PackedSheet>("tileset_gba_title")
                        add(PackTexRegion.create("title_cube_border_z", regionMap.getOrNull("cube_border_z"), RegionSpacing(1, 32, 32)))
                        add(PackTexRegion.create("title_cube_face_x", regionMap.getOrNull("cube_face_x"), RegionSpacing(1, 32, 32)))
                    }
                },
                StockTexturePacks.gba
        ), shouldThrowErrorOnMissing = false)
    }
    val renderer: WorldRenderer by lazy { 
        WorldRenderer(world, Tileset(texPack)).also { renderer ->
            TilesetPalette.createGBA1TilesetPalette().applyTo(renderer.tileset)
            renderer.camera.position.x = -2f
            renderer.camera.position.y = 0.5f
        }
    }
    private val titleTintedRegions: Map<String, TintedRegion> by lazy {
        val tileset = renderer.tileset
        listOf<TintedRegion>(
                TintedRegion("title_cube_border_z", tileset.cubeBorderZ.color),
                TintedRegion("title_cube_face_x", tileset.cubeFaceX.color),
        ).associateBy { it.regionID }
    }
    
    init {
        world.clearEntities()
        initializeNormal()
    }
    
    fun initializeFromType(type: BgType) {
        return when (type) {
            BgType.NORMAL, BgType.PRACTICE_NORMAL -> initializeNormal()
            BgType.DUNK -> initializeDunk()
            BgType.ENDLESS -> initializeEndless()
            BgType.ASSEMBLE -> initializeAssemble()
            BgType.STORY_MODE -> initializeStoryMode()
        }
    }
    
    private fun initializeNormal() {
        val world = this.world
        world.clearEntities()

        for (x in 0 until 7) {
            for (z in -2..0) {
                world.addEntity(EntityCubeMM(withBorder = z == 0, showLeftVerticalEdge = z == -2).apply {
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
        world.addEntity(EntityDunkBasketBack(world).apply {
            this.position.set(1f, 5f - 1, -1f)
        })
        world.addEntity(EntityDunkBasketRear(world).apply {
            this.position.set(0f, 5f - 1, -1f)
        })
        world.addEntity(EntityDunkBasketFront(world).apply {
            this.position.set(0f, 5f - 1, -1f)
        })
        world.addEntity(EntityDunkBasketFrontFaceZ(world).apply {
            this.position.set(0f, 5f - 1, 0f)
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
                world.addEntity(EntityCubeMM(withBorder = z == 0, showLeftVerticalEdge = z == -2).apply {
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
                world.addEntity(EntityCubeMM(withBorder = z == 0, showLeftVerticalEdge = z == -2).apply {
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

    private fun initializeStoryMode() {
        val world = this.world
        world.clearEntities()

        for (x in 0 until 3) {
            for (z in -2..2) {
                world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = z == -2).apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
        }
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = true).apply {
            this.position.set(1f, 1f, -2f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = true).apply {
            this.position.set(1f, 2f, -2f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 1f, -1f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 2f, -1f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 1f, 0f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 2f, 0f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 1f, 1f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 2f, 1f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 1f, 2f)
        })
        world.addEntity(EntityCubeMM(withBorder = false, showLeftVerticalEdge = false).apply {
            this.position.set(1f, 2f, 2f)
        })
        
        world.addEntity(EntityStoryModeDesktopInbox(world).apply {
            this.position.set(0f, 2f, -1f)
        })
        world.addEntity(EntityStoryModeDesktopTube(world).apply {
            this.position.set(-1f, -1f, 1f)
        })
        
        world.addEntity(EntityStoryModeDesktopPistonHovering(world).apply {
            this.position.set(-2f, 2f, -3f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(2f, 2f, -5f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(6f, 0f, -4f)
        })
        world.addEntity(EntityCubeHovering(world).apply {
            this.position.set(-3.75f, 0.5f, 0f)
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
        renderer.render(batch)
    }

    override fun dispose() {
        renderer.disposeQuietly()
    }
}
