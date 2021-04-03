package polyrhythmmania.soundsystem

import java.util.concurrent.CopyOnWriteArrayList


/**
 * A [TimingProvider] has a [seconds] field whose implementation will update as necessary.
 * The [onUpdate] function shall be called when the [seconds] field updates.
 */
interface TimingProvider {
    
    var seconds: Float
    val listeners: MutableList<TimingListener>

    /**
     * Called when the [seconds] field updates. It is the responsibility of the implementor to ensure this happens.
     */
    fun onUpdate(oldSeconds: Float, newSeconds: Float) {
        listeners.forEach { listener -> 
            listener.onUpdate(oldSeconds, newSeconds)
        }
    }
    
}

fun interface TimingListener {
    fun onUpdate(oldSeconds: Float, newSeconds: Float)
}

/**
 * A [TimingProvider] where an external object updates the [seconds] field.
 */
class SimpleTimingProvider : TimingProvider {

    @Volatile
    override var seconds: Float = 0f
        set(value) {
            val old = field
            field = value
            onUpdate(old, value)
        }
    override val listeners: MutableList<TimingListener> = CopyOnWriteArrayList()

}
