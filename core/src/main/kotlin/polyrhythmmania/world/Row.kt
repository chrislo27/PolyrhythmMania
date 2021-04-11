package polyrhythmmania.world


class Row(val world: World, val length: Int, val startX: Int, val startY: Int, val startZ: Int, val isDpad: Boolean) {

    val rowBlocks: List<EntityRowBlock> = List(length) { index ->
        EntityRowBlock(world, startY.toFloat(), this, index).apply {
            this.type = EntityRowBlock.Type.PLATFORM
            this.active = false
            this.position.x = (startX + index).toFloat()
            this.position.z = startZ.toFloat()
        }
    }
    val inputIndicators: List<EntityInputIndicator> = List(length) { index ->
        EntityInputIndicator(world, isDpad).apply {
            this.visible = false
            this.position.x = (startX + index).toFloat() + 1f
            this.position.z = startZ.toFloat()
            this.position.y = startY + 1f + (2f / 32f)

            // Offset to -X +Z for render order
            this.position.z += 1
            this.position.x -= 1
            this.position.y += 1
        }
    }

    /**
     * The index of the next piston-type [EntityRowBlock] to be triggered.
     * Updated with a call to [updateInputIndicators]. -1 if there are none.
     */
    var nextActiveIndex: Int = -1
        private set

    fun initWithWorld() {
        rowBlocks.forEach(world::addEntity)
        inputIndicators.forEach(world::addEntity)
        updateInputIndicators()
    }

    fun updateInputIndicators() {
        var foundActive = false
        for (i in 0 until length) {
            val rowBlock = rowBlocks[i]
            val inputInd = inputIndicators[i]

            if (foundActive) {
                inputInd.visible = false
            } else {
                if (rowBlock.active && rowBlock.type != EntityRowBlock.Type.PLATFORM && rowBlock.pistonState == EntityRowBlock.PistonState.RETRACTED) {
                    foundActive = true
                    inputInd.visible = true
                    nextActiveIndex = i
                } else {
                    inputInd.visible = false
                }
            }
        }
        if (!foundActive) {
            nextActiveIndex = -1
        }
    }

}
