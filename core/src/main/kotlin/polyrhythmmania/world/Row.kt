package polyrhythmmania.world


class Row(val world: World, val length: Int, val startX: Int, val startY: Int, val startZ: Int) {

    val rowBlocks: List<EntityRowBlock> = List(length) { index ->
        EntityRowBlock(world, startY.toFloat(), this, index).apply {
            this.type = EntityRowBlock.Type.PLATFORM
            this.active = false
            this.position.x = (startX + index).toFloat()
            this.position.z = startZ.toFloat()
        }
    }

}
