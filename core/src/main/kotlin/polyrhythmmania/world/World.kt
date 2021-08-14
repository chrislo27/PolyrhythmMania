package polyrhythmmania.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.LongMap
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.entity.*
import polyrhythmmania.world.tileset.TilesetPalette
import polyrhythmmania.world.render.WorldRenderer
import java.util.concurrent.CopyOnWriteArrayList


class World {
    
    companion object {
        const val DEFAULT_ROW_LENGTH: Int = 16
    }
    
    val tilesetPalette: TilesetPalette = TilesetPalette.createGBA1TilesetPalette()
    var worldMode: WorldMode = WorldMode.POLYRHYTHM
    
    val entities: List<Entity> = CopyOnWriteArrayList()
    val cubeMap: LongMap<EntityCube> = LongMap(100)

    // Settings
    var showInputFeedback: Boolean = PRManiaGame.instance.settings.showInputFeedbackBar.getOrCompute()
    var worldSettings: WorldSettings = WorldSettings.DEFAULT
    
    // World mode-specific things
    // POLYRHYTHM
    val rows: List<Row> = listOf(
            Row(this, DEFAULT_ROW_LENGTH, 5, 2, 0, false),
            Row(this, DEFAULT_ROW_LENGTH, 5, 2, -3, true)
    )
    val rowA: Row get() = rows[0]
    val rowDpad: Row get() = rows[1]
    // DUNK
    val dunkPiston: EntityPistonDunk by lazy { EntityPistonDunk(this) }
    // ASSEMBLE
    val asmPistons: List<EntityPistonAsm> by lazy { List(4) { EntityPistonAsm(this) } }
    
    
    init {
        resetWorld()
    }

    fun addEntity(entity: Entity) {
        if (entity !in entities) {
            (entities as MutableList).add(entity)
//            if (entity is EntityCube) { // Uncomment if cube map culling is to be used
//                synchronized(cubeMap) {
//                    cubeMap.put(entity.getCubemapIndex(), entity)
//                }
//            }
        }
    }

    fun removeEntity(entity: Entity) {
        (entities as MutableList).remove(entity)
//        if (entity is EntityCube) { // Uncomment if cube map culling is to be used
//            synchronized(cubeMap) {
//                cubeMap.remove(entity.getCubemapIndex())
//            }
//        }
    }
    
    fun clearEntities() {
        (entities as MutableList).clear()
        synchronized(cubeMap) {
            cubeMap.clear()
        }
    }
    
    fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        val entities = this.entities as MutableList
        
        entities.forEach { entity ->
            entity.engineUpdate(engine, beat, seconds)
        }

        if (worldMode == WorldMode.DUNK) {
            if (engine.inputter.endlessScore.lives.getOrCompute() <= 0) {
                entities.forEach {
                    if (it is EntityRodDunk && !it.exploded) {
                        it.explode(engine, playSound = false)
                        it.kill()
                    }
                }
            }
        }
        
        // Remove killed entities
        val toRemove = entities.filter { it.isKilled }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { it.onRemovedFromWorld(engine) }
            entities.removeAll(toRemove)
        }
    }

    fun sortEntitiesByRenderOrder() {
        (entities as MutableList).sortWith(WorldRenderer.comparatorRenderOrder)
    }
    
    // ------------------------------------------------------------------------------------------------------
    
    fun resetWorld() {
        (entities as MutableList).removeIf { it is TemporaryEntity }
        when (worldMode) {
            WorldMode.POLYRHYTHM, WorldMode.POLYRHYTHM_ENDLESS -> {
                populateRegularScene()
                rows.forEach(Row::initWithWorld)
            }
            WorldMode.DUNK -> {
                populateDunkScene()
            }
            WorldMode.ASSEMBLE -> {
                populateAssembleScene()
            }
            WorldMode.DASH -> TODO()
        }
    }

    private fun populateRegularScene() {
        clearEntities()
        
        // Main floor
        for (x in -1..20) {
            for (z in -6 until 3) {
                val ent: Entity = if (z == 0 || z == -3) {
                    EntityPlatform(this, x == 4)
                } else if ((z == 1 || z == -2) && x >= 0) {
                    EntityCube(this, withBorder = true, withLine = x == 4)
                } else EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 1f, z.toFloat())
                })
            }
        }

        // Raised platforms
        for (x in -3..4) {
            for (zMul in 0..1) {
                val ent: Entity = EntityPlatform(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 2f, zMul * -3f)
                })
            }
        }

        // Bottom floor
        for (x in -1..20) {
            for (z in 3..7) {
                val ent: Entity = EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
        }

        // Upper steps
        for (x in -1..20) {
            for (z in -7 downTo -9) {
                val ent: Entity = EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), if (z == -7) 2f else 3f, z.toFloat())
                })
            }
        }
        
        // Button signs
        val signs = mutableListOf<EntitySign>()
        signs += EntitySign(this, EntitySign.Type.A).apply {
            this.position.set(7f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.BO).apply {
            this.position.set(8f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.TA).apply {
            this.position.set(9f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.N).apply {
            this.position.set(10f, 2f, 1f)
        }
        signs += EntitySign(this, EntitySign.Type.DPAD).apply {
            this.position.set(7f, 2f, -2f)
        }
        signs += EntitySign(this, EntitySign.Type.BO).apply {
            this.position.set(8f, 2f, -2f)
        }
        signs += EntitySign(this, EntitySign.Type.TA).apply {
            this.position.set(9f, 2f, -2f)
        }
        signs += EntitySign(this, EntitySign.Type.N).apply {
            this.position.set(10f, 2f, -2f)
        }
        signs.forEach { sign ->
            sign.position.x += (12 / 32f)
            sign.position.z += (8 / 32f)
            addEntity(sign)
        }
        
        addEntity(EntityBackgroundImg(this, EntityBackgroundImg.Layer.FORE).apply { 
            this.position.set(0f, 0f, 11f)
        })
        addEntity(EntityBackgroundImg(this, EntityBackgroundImg.Layer.MIDDLE).apply { 
            this.position.set(0f, 0f, -1f)
        })
        addEntity(EntityBackgroundImg(this, EntityBackgroundImg.Layer.BACK).apply { 
            this.position.set(0f, 0f, -4f)
        })


        if (showInputFeedback) {
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.LEFT, EntityInputFeedback.BARELY_COLOUR, 0).apply {
                this.position.set(7f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, 1).apply {
                this.position.set(8f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.ACE_COLOUR, 2).apply {
                this.position.set(9f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, 3).apply {
                this.position.set(10f, 1f, 3f)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.RIGHT, EntityInputFeedback.BARELY_COLOUR, 4).apply {
                this.position.set(11f, 1f, 3f)
            })
        }
        
    }

    private fun populateDunkScene() {
        clearEntities()

        // Main floor
        for (x in -1..9) {
            for (z in -3 until 2) {
                if (x >= 8 && z == -1) continue
                val ent: Entity = if (z == -1) {
                    EntityPlatform(this, x == 4)
                } else if (z == 0 && x <= 7) {
                    EntityCube(this, withBorder = true, withLine = x == 4)
                } else EntityCube(this, withLine = x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 1f, z.toFloat())
                })
            }
        }

        // Raised platform
        for (x in -3..3) {
            val ent: Entity = EntityPlatform(this)
            addEntity(ent.apply {
                this.position.set(x.toFloat(), 2f, -1f)
            })
        }
        
        addEntity(dunkPiston.apply { 
            this.position.set(4f, 2f, -1f)
            this.type = EntityPiston.Type.PISTON_A
        })
        
        // Hoop
        for (y in 1..5) {
            val ent: Entity = EntityPlatform(this, false)
            addEntity(ent.apply { 
                this.position.set(9f, y.toFloat(), -1f)
            })
        }
        addEntity(EntityDunkBacking(this).apply { 
            this.position.set(9f, 6f, -1f)
        })
        addEntity(EntityDunkBasketRear(this).apply {
            this.position.set(8f + 1, 5f - 1, -1f - 1)
        })
        addEntity(EntityDunkBasketFront(this).apply {
            this.position.set(8f, 5f, -1f)
        })
        addEntity(EntityDunkBasketFrontFaceZ(this).apply {
            this.position.set(8f, 5f, 0f)
        })
        addEntity(EntityDunkBasketBack(this).apply { 
            this.position.set(9f, 5f, -1f)
        })

        // Button signs
        val signs = mutableListOf<EntitySign>()
        signs += EntitySign(this, EntitySign.Type.A).apply {
            this.position.set(3f, 2f, 0f)
        }
        signs += EntitySign(this, EntitySign.Type.BO).apply {
            this.position.set(4f, 2f, 0f)
        }
        signs += EntitySign(this, EntitySign.Type.TA).apply {
            this.position.set(5f, 2f, 0f)
        }
        signs += EntitySign(this, EntitySign.Type.N).apply {
            this.position.set(6f, 2f, 0f)
        }
        signs.forEach { sign ->
            sign.position.x += (12 / 32f)
            sign.position.z += (8 / 32f)
            addEntity(sign)
        }


        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.LEFT, EntityInputFeedback.MISS_COLOUR, 0).apply {
            this.position.set(5f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.MISS_COLOUR, 1).apply {
            this.position.set(6f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.ACE_COLOUR, 2).apply {
            this.position.set(7f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.MISS_COLOUR, 3).apply {
            this.position.set(8f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.RIGHT, EntityInputFeedback.MISS_COLOUR, 4).apply {
            this.position.set(9f, 1f, 2f)
        })

    }
    
    private fun populateAssembleScene() {
        clearEntities()

        // Main floor
        for (x in 4..22) {
            for (z in -7 until -4) {
                for (y in 0 downTo -11) {
                    if (y != 0 && z != -5) continue
                    if (y <= -(x * 0.5f + 2)) continue
                    val ent: Entity = if (x == 12) {
                        EntityAsmPerp(this, isTarget = z == -6).apply { 
                            this.tint = Color(1f, 1f, 1f, 1f)
                        }
                    } else if (z == -6) {
                        EntityAsmLane(this)
                    } else {
                        EntityAsmCube(this, Color().grey((15 + y) * (1f / 15f)))
                    }
                    addEntity(ent.apply {
                        this.position.set(x.toFloat(), y.toFloat() - 1, z.toFloat())
                    })
                }
            }
        }

        
        addEntity(asmPistons[0].apply {
            this.type = EntityPiston.Type.PISTON_DPAD
            this.position.set(6f, 0f, -0f)
        })
        addEntity(asmPistons[1].apply {
            this.type = EntityPiston.Type.PISTON_DPAD
            this.position.set(8.5f, 0f, -0f)
        })
        addEntity(asmPistons[2].apply {
            this.type = EntityPiston.Type.PISTON_A
            this.position.set(11f, 0f, -0f)
            this.tint = EntityPistonAsm.playerTint.cpy()
        })
        addEntity(asmPistons[3].apply {
            this.type = EntityPiston.Type.PISTON_DPAD
            this.position.set(13.5f, 0f, -0f)
        })
    }
}