package polyrhythmmania.editor

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.util.MathHelper
import io.github.chrislo27.paintbox.util.gdxutils.intersects
import polyrhythmmania.editor.block.Block
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
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

        private val editorBlocksCollidable: Map<Int, List<Block>> = (editor.blocks - blocks).groupBy { it.trackIndex }

        // Represents the last known position from onMouseMoved
        var hasBeenUpdated: Boolean = false
            private set
        var beat: Float = 0f
        var track: Int = -1
        val isPlacementInvalid: ReadOnlyVar<Boolean> = Var(true)
        val wouldBeDeleted: ReadOnlyVar<Boolean> = Var(false)
        val collidesWithOtherBlocks: ReadOnlyVar<Boolean> = Var(false)

        fun didOriginBlockChange(): Boolean {
            val originalRegion = originalRegions.getValue(originBlock)
            val newRegion = regions.getValue(originBlock)
            return originalRegion != newRegion
        }

        fun anyBlocksWouldCollide(): Boolean {
            val epsilon = 0.001f
            return regions.any { (block, region) ->
                val beat = region.beat
                val width = block.width
                editorBlocksCollidable[region.track]?.any { other ->
                    beat < (other.beat + other.width - epsilon) && other.beat < (beat + width - epsilon)
                } ?: false
            }
        }

        fun complete() {
            if (!hasBeenUpdated || isPlacementInvalid.getOrCompute() || !didOriginBlockChange()) {
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
            var beat = beat.coerceAtLeast(0f)
            this.beat = beat
            this.track = trackIndex
            (this.isPlacementInvalid as Var).set(trackIndex !in 0 until editor.tracks.size)
//            (this.wouldBeDeleted as Var).set(trackIndex < 0)
            (this.wouldBeDeleted as Var).set(trackIndex !in 0 until editor.tracks.size)

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
                val targetTrackIndex = (trackY /*- mouseOffset.y*/).toInt()
                val targetTrack: Track? = editor.tracks.getOrNull(targetTrackIndex)
                if (targetTrack != null) {
                    if (blocks.all { b -> targetTrack.acceptsBlock(b) }) {
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

            if (anyBlocksWouldCollide()) {
                (this.collidesWithOtherBlocks as Var).set(true)
                this.isPlacementInvalid.set(true)
            } else {
                (this.collidesWithOtherBlocks as Var).set(false)
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

    class MoveMarker(val editor: Editor, val point: FloatVar, val type: MarkerType)
        : Click() {

        val originalPosition: Float = point.getOrCompute()

        fun didChange(): Boolean = point.getOrCompute() != originalPosition

        fun complete() {
            if (!didChange()) {
                abortAction()
                return
            }
            val beatLines = editor.beatLines
            beatLines.active = false
        }

        override fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {
            val snapping = editor.snapping.getOrCompute()
            val snapBeat = MathHelper.snapToNearest(beat, snapping).coerceAtLeast(0f)
            point.set(snapBeat)
            
            val beatLines = editor.beatLines
            beatLines.active = true
            beatLines.fromBeat = floor(beat).toInt()
            beatLines.toBeat = ceil(beat).toInt()
        }

        override fun abortAction() {
            point.set(originalPosition)
            val beatLines = editor.beatLines
            beatLines.active = false
        }
    }

    class MoveTempoChange(val editor: Editor, val tempoChange: TempoChange)
        : Click() {

        var newPosition: Float = tempoChange.beat
            private set
        var lastValidPosition: Float = tempoChange.beat
            private set
        var lastValidTempoChangePos: TempoChange = tempoChange.copy(beat = lastValidPosition)
            private set

        val isCurrentlyValid: Var<Boolean> = Var(false)

        fun didChange(): Boolean = newPosition != tempoChange.beat

        fun complete(): TempoChange? {
            if (!didChange() || !isPositionValid(newPosition)) {
                abortAction()
                return null
            }
            return tempoChange.copy(beat = newPosition)
        }

        fun isPositionValid(beat: Float): Boolean {
            return beat > 0f && !editor.tempoChanges.getOrCompute().any { tc ->
                tc !== tempoChange && tc.beat == beat
            }
        }

        override fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {
            val snapping = editor.snapping.getOrCompute()
            val snapBeat = MathHelper.snapToNearest(beat, snapping).coerceAtLeast(0f)
            newPosition = snapBeat
            val isPosValid = isPositionValid(snapBeat)
            val newPos = if (isPosValid) snapBeat else tempoChange.beat
            if (lastValidPosition != newPos) {
                lastValidPosition = newPos
                lastValidTempoChangePos = tempoChange.copy(beat = newPos)
            }
            isCurrentlyValid.set(isPosValid)
        }

        override fun abortAction() {
        }
    }

    class DragMusicVolume(val editor: Editor, val musicVol: MusicVolume, val left: Boolean)
        : Click() {

        var beat: Float = musicVol.beat
            private set
        var width: Float = musicVol.width
            private set
        
//        var newPosition: Float = musicVol.beat
//            private set
//        var lastValidPosition: Float = musicVol.beat
//            private set
//        var lastValidMusicVolPos: MusicVolume = musicVol.copy(beat = lastValidPosition)
//            private set

        val isCurrentlyValid: Var<Boolean> = Var(false)

        fun normalizeWidth() {
            if (width < 0) {
                width = abs(width)
                beat -= width
            }
        }
        
        fun didChange(): Boolean = beat != musicVol.beat || width != musicVol.width

        fun complete(): MusicVolume? {
            if (!didChange() || !isPositionValid()) {
                abortAction()
                return null
            }
            normalizeWidth()
            return musicVol.copy(beat = beat, width = width)
        }

        fun isPositionValid(): Boolean {
            return beat >= 0f && !editor.musicVolumes.getOrCompute().any { mv ->
                mv !== musicVol &&
                        ((beat < mv.beat + mv.width && beat + width > mv.beat) || mv.beat == beat)
            }
        }

        override fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {
            val snapping = editor.snapping.getOrCompute()
            val newPos = MathHelper.snapToNearest(beat, snapping).coerceAtLeast(0f)

            val originalX = musicVol.beat
            val originalEndX = musicVol.beat + musicVol.width

            if (left) {
                this.beat = newPos
                this.width = originalEndX - newPos
            } else {
                this.beat = originalX
                this.width = newPos - originalX
            }

            normalizeWidth()
            
            val isPosValid = isPositionValid()
            isCurrentlyValid.set(isPosValid)
        }

        override fun abortAction() {
        }
    }

    open fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {

    }

    open fun renderUpdate() {
    }

    open fun abortAction() {
    }
}
