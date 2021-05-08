package polyrhythmmania.editor

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.util.MathHelper
import io.github.chrislo27.paintbox.util.gdxutils.intersects
import polyrhythmmania.editor.track.Track
import polyrhythmmania.editor.track.block.Block
import polyrhythmmania.editor.undo.ReversibleAction
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor


sealed class Click {
    object None : Click()

    class CreateSelection(val editor: Editor, val startBeat: Float, val startTrack: Float,
                          val previousSelection: Set<Block>)
        : Click() {
        val rectangle: Rectangle = Rectangle(startBeat, startTrack, 0f, 0f)
        private val tmpRect: Rectangle = Rectangle()

        fun updateRectangle(mouseBeat: Float, mouseTrack: Float) {
            val startX = startBeat
            val startY = startTrack
            val width = mouseBeat - startX
            val height = mouseTrack - startY

            if (width < 0) {
                val abs = abs(width)
                rectangle.x = startX - abs
                rectangle.width = abs
            } else {
                rectangle.x = startX
                rectangle.width = width
            }

            if (height < 0) {
                val abs = abs(height)
                rectangle.y = startY - abs
                rectangle.height = abs
            } else {
                rectangle.y = startY
                rectangle.height = height
            }
        }

        fun isBlockInSelection(block: Block): Boolean {
            tmpRect.set(block.beat, block.trackIndex.toFloat(), block.width, 1f)

            return rectangle.intersects(tmpRect)
        }

        override fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {
            updateRectangle(beat, trackY)
        }
    }

    class DragSelection private constructor(val editor: Editor, val blocks: List<Block>, val mouseOffset: Vector2,
                                            val originBlock: Block, val isNew: Boolean)
        : Click() {

        companion object {
            fun create(editor: Editor, blocks: List<Block>, mouseOffset: Vector2, originBlock: Block,
                       isNew: Boolean): DragSelection? {
                if (blocks.isEmpty() || originBlock !in blocks) return null
                return DragSelection(editor, blocks, mouseOffset, originBlock, isNew)
            }
        }

        data class BlockRegion(var beat: Float, var track: Int)

        val regions: Map<Block, BlockRegion> = blocks.associateWith { BlockRegion(it.beat, it.trackIndex) }
        val originalRegions: Map<Block, BlockRegion> = blocks.associateWith { BlockRegion(it.beat, it.trackIndex) }
        val originalOffsets: Map<Block, BlockRegion> = blocks.associateWith { block ->
            BlockRegion(block.beat - originBlock.beat, block.trackIndex - originBlock.trackIndex)
        }
        val topmost: Block = blocks.minByOrNull { it.trackIndex }!!
        val leftmost: Block = blocks.minByOrNull { it.beat }!!
        val rightmost: Block = blocks.maxByOrNull { it.beat + it.width }!!
        val bottommost: Block = blocks.maxByOrNull { it.trackIndex + 1 }!!
        val encompassingRegion: BlockRegion = run {
            BlockRegion(rightmost.beat + rightmost.width - leftmost.beat, bottommost.trackIndex - topmost.trackIndex + 1)
        }

        // Represents the last known position from onMouseMoved
        var hasBeenUpdated: Boolean = false
            private set
        var beat: Float = 0f
        var track: Int = -1
        var isPlacementInvalid: ReadOnlyVar<Boolean> = Var(true)
            private set
        var wouldBeDeleted: ReadOnlyVar<Boolean> = Var(false)
            private set

        fun complete() {
            if (!hasBeenUpdated || isPlacementInvalid.getOrCompute()) {
                abortAction()
                return
            }
            val beatLines = editor.beatLines
            beatLines.active = false

            this.blocks.forEach { block ->
                val region = regions.getValue(block)
                block.beat = region.beat
                block.trackIndex = region.track
            }
        }

        override fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {
            hasBeenUpdated = true
            this.beat = beat
            this.track = trackIndex
            (this.isPlacementInvalid as Var).set(trackIndex !in 0 until editor.tracks.size)
            (this.wouldBeDeleted as Var).set(trackIndex < 0)

            // Adjust block regions
            // Set origin block
            val snapping = editor.snapping.getOrCompute()
            val originRegion = regions.getValue(originBlock)
            val snapToRight = mouseOffset.x > originBlock.width / 2
            if (snapToRight) {
                val newBeatR = MathHelper.snapToNearest((beat - mouseOffset.x + originBlock.width), snapping)
                originRegion.beat = newBeatR - originBlock.width
            } else {
                val newBeat = MathHelper.snapToNearest((beat - mouseOffset.x), snapping)
                originRegion.beat = newBeat
            }
            // TODO make this more flexible? Selections spanning multiple tracks cannot move between tracks
            if (encompassingRegion.track <= 1) {
                val targetTrackIndex = (trackY - mouseOffset.y).toInt()
                val targetTrack: Track? = editor.tracks.getOrNull(targetTrackIndex)
                if (targetTrack != null) {
                    val allowedTypes = targetTrack.allowedTypes
                    if (blocks.all { b -> b.blockTypes.any { bt -> bt in allowedTypes } }) {
                        originRegion.track = targetTrackIndex
                    }
                }
            }

            // Set other blocks relative to origin block
            for (block in blocks) {
                if (block === originBlock) continue
                val region = regions.getValue(block)
                val originalOffset = originalOffsets.getValue(block)

                val originalOffsetBeat = originalOffset.beat
                val originalOffsetTrack = originalOffset.track

                region.beat = originRegion.beat + originalOffsetBeat
                region.track = originRegion.track + originalOffsetTrack
            }

            val beatLines = editor.beatLines
            beatLines.active = true
            beatLines.fromBeat = floor(beat).toInt()
            beatLines.toBeat = ceil(beat).toInt()
        }

        override fun renderUpdate() {
        }

        override fun abortAction() {
            val beatLines = editor.beatLines
            beatLines.active = false
        }
    }

    open fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {

    }

    open fun renderUpdate() {
    }

    open fun abortAction() {
    }
}
