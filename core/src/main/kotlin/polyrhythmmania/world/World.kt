package polyrhythmmania.world

import polyrhythmmania.world.render.WorldRenderer


class World {

    val entities: List<Entity> = ArrayList()

    init {
        populateScene()
    }

    fun addEntity(entity: Entity) {
        if (entity !in entities) {
            (entities as MutableList).add(entity)
        }
    }

    fun removeEntity(entity: Entity) {
        (entities as MutableList).remove(entity)
    }

    fun sortEntitiesByRenderOrder() {
        (entities as MutableList).sortWith(WorldRenderer.comparatorRenderOrder)
    }

    private fun populateScene() {
        // Beginning platform
        for (x in -1..4) {
            for (z in 0..1) {
                addEntity(EntityPlatform(this, x == 4).apply {
                    this.position.set(x.toFloat(), 2f, z.toFloat() * -3)
                })
            }
        }
        // End platform
        for (x in 13..16) {
            for (z in 0..1) {
                addEntity(EntityPlatform(this, false).apply {
                    this.position.set(x.toFloat(), 2f, z.toFloat() * -3)
                })
            }
        }
        // Main floor
        for (x in 0 until 17) {
            for (z in -6 until 3) {
                val ent = if (z == 0 || z == -3) {
                    if (x in 5..12) EntityPlatform(this, x == 4) else continue
                } else EntityCube(this, x == 4)
                addEntity(ent.apply {
                    this.position.set(x.toFloat(), 1f, z.toFloat())
                })
            }
        }
        // Bottom floor
        for (x in 5..12) {
            addEntity(EntityCube(this, x == 4).apply {
                this.position.set(x.toFloat(), 0f, 3f)
            })
        }
        for (x in 7..11) {
            addEntity(EntityCube(this, false).apply {
                this.position.set(x.toFloat(), 0f, 4f)
            })
        }
        for (x in 8..9) {
            addEntity(EntityCube(this, false).apply {
                this.position.set(x.toFloat(), 0f, 5f)
            })
        }
        // Top steps
        for (x in 5 until 15) {
            addEntity(EntityCube(this, false).apply {
                this.position.set(x.toFloat(), 2f, -7f)
            })
        }
        for (x in 6 until 13) {
            addEntity(EntityCube(this, false).apply {
                this.position.set(x.toFloat(), 3f, -8f)
            })
        }
        addEntity(EntityCube(this, false).apply {
            this.position.set(8f, 3f, -9f)
        })

        (entities as MutableList).shuffle()
    }

}