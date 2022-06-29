package polyrhythmmania.gamemodes

import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.music.MusicVolume
import java.util.*


@Deprecated("Eventually replace with mutable volume coeff in Engine that can be reset")
class ResetMusicVolumeBlock(engine: Engine, val startingVolume: Int = 100)
    : Block(engine, EnumSet.allOf(BlockType::class.java)) {
    
    override fun compileIntoEvents(): List<Event> {
        return listOf(
            object : Event(engine) {
                override fun onStart(currentBeat: Float) {
                    super.onStart(currentBeat)

                    val volumeMap = engine.musicData.volumeMap
                    volumeMap.removeMusicVolumesBulk(volumeMap.getAllMusicVolumes().toList())
                    volumeMap.addMusicVolume(MusicVolume(currentBeat, 0f, startingVolume))
                }
            }
        )
    }

    override fun copy(): ResetMusicVolumeBlock {
        return ResetMusicVolumeBlock(engine, this.startingVolume).also { 
            this.copyBaseInfoTo(it)
        }
    }
}