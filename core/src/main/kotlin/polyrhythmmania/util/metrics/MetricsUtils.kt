package polyrhythmmania.util.metrics

import com.codahale.metrics.Clock
import com.codahale.metrics.Timer
import java.util.concurrent.TimeUnit


inline fun Timer?.timeInline(block: () -> Unit) {
    if (this == null) {
        block()
    } else {
        val clock = Clock.defaultClock()
        val nano = clock.tick
        block()
        val endNano = clock.tick
        this.update(endNano - nano, TimeUnit.NANOSECONDS)
    }
}
