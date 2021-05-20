package polyrhythmmania.engine

import polyrhythmmania.engine.music.MusicVolMap


class MusicData(val engine: Engine) {
    
    var musicDelaySec: Float = 0f
    val volumeMap: MusicVolMap = MusicVolMap()
    
}