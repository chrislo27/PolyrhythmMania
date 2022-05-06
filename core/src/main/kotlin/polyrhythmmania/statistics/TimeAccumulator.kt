package polyrhythmmania.statistics

import com.badlogic.gdx.Gdx

class TimeAccumulator(val stat: Stat) {
    private var msAcc: Float = 0f

    fun update() {
        msAcc += Gdx.graphics.deltaTime * 1000
        
        if (msAcc >= 1000f) {
            val addSeconds = msAcc.toLong() / 1000L
            stat.increment(addSeconds.toInt().coerceAtLeast(0))
            msAcc = (msAcc - addSeconds * 1000).coerceAtLeast(0f)
        }
    }
}
