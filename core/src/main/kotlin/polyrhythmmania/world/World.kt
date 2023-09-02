package polyrhythmmania.world

import com.badlogic.gdx.graphics.Color
import paintbox.util.gdxutils.grey
import paintbox.util.settableLazy
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.world.entity.*
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.spotlights.Spotlights
import polyrhythmmania.world.tileset.TilesetPalette
import java.util.concurrent.CopyOnWriteArrayList


class World(numOfSpotlights: Int?) {
    
    companion object {
        const val DEFAULT_ROW_LENGTH: Int = 16
        private const val DEFAULT_NUM_SPOTLIGHTS_PER_ROW: Int = 10
    }
    
    fun interface WorldResetListener {
        fun onWorldReset(world: World)
    }
    
    val tilesetPalette: TilesetPalette = TilesetPalette.createGBA1TilesetPalette()
    var worldMode: WorldMode = WorldMode(WorldType.Polyrhythm())
    
    val entities: List<Entity> = CopyOnWriteArrayList()
    val spotlights: Spotlights = Spotlights(this, numOfSpotlights ?: DEFAULT_NUM_SPOTLIGHTS_PER_ROW)

    // Settings
    var showInputFeedback: Boolean = PRManiaGame.instance.settings.showInputFeedbackBar.getOrCompute()
    var worldSettings: WorldSettings = WorldSettings.DEFAULT
    
    val worldResetListeners: MutableList<WorldResetListener> = mutableListOf()
    
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
    var asmPistons: List<EntityPistonAsm> by settableLazy { createAsmPistons() }
        private set
    val asmPlayerPiston: EntityPistonAsm get() = asmPistons[2]
    
    constructor() : this(DEFAULT_NUM_SPOTLIGHTS_PER_ROW)
    
    init {
        resetWorld()
    }

    fun addEntity(entity: Entity) {
        if (entity !in entities) {
            (entities as MutableList).add(entity)
        }
    }

    fun removeEntity(entity: Entity) {
        (entities as MutableList).remove(entity)
    }
    
    fun clearEntities() {
        (entities as MutableList).clear()
    }
    
    fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        val entities = this.entities as MutableList
        
        entities.forEach { entity ->
            entity.engineUpdate(engine, beat, seconds)
        }

        if (worldMode.worldType == WorldType.Dunk) {
            val endlessScore = engine.modifiers.endlessScore
            if (endlessScore.enabled.get() && endlessScore.lives.get() <= 0) {
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

    fun sortEntitiesByRenderOrder(comparator: Comparator<Entity> = WorldRenderer.comparatorRenderOrder) {
        (entities as MutableList).sortWith(comparator)
    }
    
    // ------------------------------------------------------------------------------------------------------

    /**
     * Removes [TemporaryEntity]s, repopulates the scenes, and calls [triggerWorldResetListeners].
     */
    fun resetWorld() {
        spotlights.onWorldReset()
        (entities as MutableList).removeIf { it is TemporaryEntity }
        when (worldMode.worldType) {
            is WorldType.Polyrhythm -> {
                populateRegularScene()
                rows.forEach(Row::onWorldReset)
            }
            WorldType.Dunk -> {
                populateDunkScene()
            }
            WorldType.Assemble -> {
                populateAssembleScene()
            }
        }
        triggerWorldResetListeners()
    }
    
    fun triggerWorldResetListeners() {
        worldResetListeners.toList().forEach { it.onWorldReset(this) }
    }
    
    private fun createSignEntities(x: Float, y: Float, z: Float, symbol: EntitySign.Type, english: Boolean): List<EntitySign> {
        val signs = mutableListOf<EntitySign>()
        
        if (!english) {
            signs += EntitySign(this, symbol, EntitySign.RenderCriteria.ONLY_JP).apply {
                this.position.set(x, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.JP_BO, EntitySign.RenderCriteria.ONLY_JP).apply {
                this.position.set(x + 1f, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.JP_TA, EntitySign.RenderCriteria.ONLY_JP).apply {
                this.position.set(x + 2f, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.JP_N, EntitySign.RenderCriteria.ONLY_JP).apply {
                this.position.set(x + 3f, y, z)
            }
        } else {
            signs += EntitySign(this, EntitySign.Type.EN_P, EntitySign.RenderCriteria.ONLY_EN).apply {
                this.position.set(x, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.EN_R, EntitySign.RenderCriteria.ONLY_EN).apply {
                this.position.set(x + 0.75f * 1, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.EN_E, EntitySign.RenderCriteria.ONLY_EN).apply {
                this.position.set(x + 0.75f * 2, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.EN_S, EntitySign.RenderCriteria.ONLY_EN).apply {
                this.position.set(x + 0.75f * 3, y, z)
            }
            signs += EntitySign(this, EntitySign.Type.EN_S, EntitySign.RenderCriteria.ONLY_EN).apply {
                this.position.set(x + 0.75f * 4, y, z)
            }
            signs += EntitySign(this, symbol, EntitySign.RenderCriteria.ONLY_EN).apply {
                this.position.set(x + 4f, y, z)
            }
        }
        
        return signs
    }

    private fun populateRegularScene() {
        clearEntities()
        
        fun getNewEntities(showRaisedPlatformsAtBeginning: Boolean): List<Entity> {
            val list = mutableListOf<Entity>()
            
            fun showRedLine(x: Int): Boolean = x == 4
            
            // Main floor
            for (x in -3..20) {
                for (z in -6..<3) {
                    val ent: Entity = when (z) {
                        0, -3 -> EntityPlatform(this, false)
                        1, -2 -> EntityCube(this, withBorder = true, withLine = showRedLine(x))
                        else -> EntityCube(this, showRedLine(x))
                    }
                    list += ent.apply {
                        this.position.set(x.toFloat(), 1f, z.toFloat())
                    }
                }
            }

            // Raised platforms at beginning
            if (showRaisedPlatformsAtBeginning) {
                for (x in -3..4) {
                    for (zMul in 0..1) {
                        val ent: Entity = EntityPlatform(this, showRedLine(x))
                        list += ent.apply {
                            this.position.set(x.toFloat(), 2f, zMul * -3f)
                        }
                    }
                }
            }

            // Bottom floor
            for (x in -3..20) {
                for (z in 3..9) {
                    val ent: Entity = EntityCube(this, showRedLine(x))
                    list += ent.apply {
                        this.position.set(x.toFloat(), 0f, z.toFloat())
                    }
                }
            }

            // Upper steps
            for (x in -3..20) {
                for (z in -7 downTo -11) {
                    val ent: Entity = EntityCube(this, showRedLine(x))
                    list += ent.apply {
                        this.position.set(x.toFloat(), if (z == -7) 2f else 3f, z.toFloat())
                    }
                }
            }

            // Button signs
            listOf(false, true).forEach { english ->
                list.addAll(createSignEntities(7f, 2f, 1f, EntitySign.Type.SYMBOL_A, english))
                list.addAll(createSignEntities(7f, 2f, -2f, EntitySign.Type.SYMBOL_DPAD, english))
            }

            list += EntityBackgroundImg(this, EntityBackgroundImg.Layer.FORE).apply {
                this.position.set(0f, 0f, 11f)
            }
            list += EntityBackgroundImg(this, EntityBackgroundImg.Layer.MIDDLE).apply {
                this.position.set(0f, 0f, -1f)
            }
            list += EntityBackgroundImg(this, EntityBackgroundImg.Layer.BACK).apply {
                this.position.set(0f, 0f, -4f)
            }


            if (showInputFeedback) {
                list += EntityInputFeedback(this, EntityInputFeedback.End.LEFT, EntityInputFeedback.BARELY_COLOUR, InputScore.BARELY, 0).apply {
                    this.position.set(7f, 1f, 3f)
                }
                list += EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, InputScore.GOOD, 1).apply {
                    this.position.set(8f, 1f, 3f)
                }
                list += EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.ACE_COLOUR, InputScore.ACE, 2).apply {
                    this.position.set(9f, 1f, 3f)
                }
                list += EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, InputScore.GOOD, 3).apply {
                    this.position.set(10f, 1f, 3f)
                }
                list += EntityInputFeedback(this, EntityInputFeedback.End.RIGHT, EntityInputFeedback.BARELY_COLOUR, InputScore.BARELY, 4).apply {
                    this.position.set(11f, 1f, 3f)
                }
            }
            
            return list
        }
        
        getNewEntities(true).forEach(this::addEntity)
        
        val worldType = this.worldMode.worldType
        if (worldType is WorldType.Polyrhythm) {
            // Extra entities for continuous mode
            
            val ents = getNewEntities(worldType.showRaisedPlatformsRepeated).toMutableList()

            // Last 6 raised platforms
            for (x in -3..4) {
                for (zMul in 0..1) {
                    val ent: Entity = EntityPlatform(this)
                    ents += ent.apply {
                        this.position.set(x.toFloat() + (8 + 8 + 2), 2f, zMul * -3f)
                    }
                }
            }
            ents.forEach {// Move all copied blocks over
                it.position.x += 24f
                addEntity(it)
            }
        }
    }

    private fun populateDunkScene() {
        clearEntities()

        // Main floor
        for (x in -2..9) {
            for (z in -3..<2) {
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
        addEntity(EntityDunkBasketBack(this).apply { // Back skirt against the wall
            this.position.set(9f, 5f, -1f)
        })
        addEntity(EntityDunkBasketRear(this).apply {
            this.position.set(8f, 5f, -1f)
        })
        addEntity(EntityDunkBasketFront(this).apply {
            this.position.set(8f, 5f, -1f)
        })
        addEntity(EntityDunkBasketFrontFaceZ(this).apply {
            this.position.set(8f, 5f, 0f)
        })

        // Button signs
        val signs = listOf(false, true).flatMap { english ->
            createSignEntities(3f, 2f, 0f, EntitySign.Type.SYMBOL_A, english)
        }
        signs.forEach { sign ->
            addEntity(sign)
        }


        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.LEFT, EntityInputFeedback.BARELY_COLOUR, InputScore.BARELY, 0).apply {
            this.position.set(5f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, InputScore.GOOD, 1).apply {
            this.position.set(6f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.ACE_COLOUR, InputScore.ACE, 2).apply {
            this.position.set(7f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR, InputScore.GOOD, 3).apply {
            this.position.set(8f, 1f, 2f)
        })
        addEntity(EntityInputFeedback(this, EntityInputFeedback.End.RIGHT, EntityInputFeedback.BARELY_COLOUR, InputScore.BARELY, 4).apply {
            this.position.set(9f, 1f, 2f)
        })

    }
    
    private fun populateAssembleScene() {
        clearEntities()

        // Main floor
        for (x in 4..22) {
            for (z in -7..<-4) {
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


        // Background
        for (x in 8..22) {
            for (y in -1 downTo -7) {
                if (y <= -(x * 0.5f + 2)) continue
                if (y >= -(x * 0.5f - 5)) continue
                val ent: Entity = EntityAsmCube(this, Color().grey((11 + y) * (1f / 11f)))

                addEntity(ent.apply {
                    this.position.set(x.toFloat(), y.toFloat() + 4f, -10f)
                })
            }
        }

        asmPistons = createAsmPistons()
        addEntity(asmPistons[0].apply {
            this.type = EntityPiston.Type.PISTON_DPAD
            this.position.set(6f + 0.5f, 0f, -0f)
        })
        addEntity(asmPistons[1].apply {
            this.type = EntityPiston.Type.PISTON_DPAD
            this.position.set(8.5f + 0.5f, 0f, -0f)
        })
        addEntity(asmPistons[2].apply {
            this.type = EntityPiston.Type.PISTON_A
            this.position.set(11f + 0.5f, 0f, -0f)
            this.tint = EntityPistonAsm.playerTint.cpy()
        })
        addEntity(asmPistons[3].apply {
            this.type = EntityPiston.Type.PISTON_DPAD
            this.position.set(13.5f + 0.5f, 0f, -0f)
        })


        if (showInputFeedback) {
            val xOff = 4f
            val yPos = -1f
            val zPos = -9f
            val tint = Color().grey(0.8f)
            
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.LEFT, EntityInputFeedback.BARELY_COLOUR.cpy().mul(tint), InputScore.BARELY, 0).apply {
                this.position.set(7f + xOff, yPos, zPos)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR.cpy().mul(tint), InputScore.GOOD, 1).apply {
                this.position.set(8f + xOff, yPos, zPos)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.ACE_COLOUR.cpy().mul(tint), InputScore.ACE, 2).apply {
                this.position.set(9f + xOff, yPos, zPos)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.MIDDLE, EntityInputFeedback.GOOD_COLOUR.cpy().mul(tint), InputScore.GOOD, 3).apply {
                this.position.set(10f + xOff, yPos, zPos)
            })
            addEntity(EntityInputFeedback(this, EntityInputFeedback.End.RIGHT, EntityInputFeedback.BARELY_COLOUR.cpy().mul(tint), InputScore.BARELY, 4).apply {
                this.position.set(11f + xOff, yPos, zPos)
            })
        }
    }
    
    private fun createAsmPistons(): List<EntityPistonAsm> {
        return List(4) { EntityPistonAsm(this) }
    }
}