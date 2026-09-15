package polyrhythmmania.engine.music

import com.badlogic.gdx.math.Interpolation
import java.util.*
import kotlin.math.roundToInt


/**
 * Represents a mapping of music volume changes. The starting volume is always 100.
 */
class MusicVolMap() {

    private var allMusicVolumes: List<MusicVolume> = emptyList()
    private val beatMap: NavigableMap<Float, MusicVolume> = TreeMap()
    private val beatMapData: NavigableMap<Float, MusicVolData> = TreeMap()

    fun getAllMusicVolumes(): List<MusicVolume> = allMusicVolumes
    
    fun addMusicVolume(musicVolume: MusicVolume): Boolean =
            addMusicVolume(musicVolume, true)
    
    fun removeMusicVolume(musicVolume: MusicVolume): Boolean =
            removeMusicVolume(musicVolume, true)
    
    private fun addMusicVolume(musicVolume: MusicVolume, update: Boolean): Boolean {
        if (musicVolume.beat < 0f) return false
        
        beatMap[musicVolume.beat] = musicVolume
        if (update) update()
        
        return true
    }
    
    private fun removeMusicVolume(musicVolume: MusicVolume, update: Boolean): Boolean {
        val removed = beatMap.remove(musicVolume.beat, musicVolume)
        if (update) update()
        
        return removed
    }
    
    fun addMusicVolumesBulk(musicVolumes: List<MusicVolume>) {
        musicVolumes.forEach { tc ->
            addMusicVolume(tc, false)
        }
        update()
    }
    
    fun removeMusicVolumesBulk(musicVolumes: List<MusicVolume>) {
        musicVolumes.forEach { tc ->
            removeMusicVolume(tc, false)
        }
        update()
    }
    
    private fun update() {
        beatMapData.clear()
        
        var previous: MusicVolData? = null
        beatMap.values.forEach { mv ->
            val prev = previous
            val data: MusicVolData = if (prev == null) {
                MusicVolData(100, mv)
            } else {
                MusicVolData(prev.musicVolume.newVolume, mv)
            }
            
            beatMapData[data.musicVolume.beat] = data
            previous = data
        }
        
        allMusicVolumes = beatMap.values.toList()
    }

    fun getMusicVolDataAtBeat(beat: Float): MusicVolData? {
        return beatMapData.floorEntry(beat)?.value
    }
    
    fun volumeAtBeat(beat: Float): Int {
        if (beat < 0f) return 100
        val volData = getMusicVolDataAtBeat(beat) ?: return 100
        val mv = volData.musicVolume
        val endpoint = mv.beat + mv.width
        
        return if (mv.width > 0f && beat in mv.beat..endpoint) {
            Interpolation.linear.apply(volData.previousVol.toFloat(), mv.newVolume.toFloat(), ((beat - mv.beat) / mv.width).coerceIn(0f, 1f)).roundToInt()
        } else {
            mv.newVolume
        }
    }
    
    data class MusicVolData(val previousVol: Int, val musicVolume: MusicVolume)

}