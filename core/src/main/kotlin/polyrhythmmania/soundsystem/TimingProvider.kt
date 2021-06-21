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
     * Called when an exception occurs during update of [seconds] or calling of [listeners] ([onUpdate] function).
     * 
     * Implementors of [TimingProvider] are required to implement this behaviour and when it is called.
     * @return True if the [TimingProvider] should continue updating listeners after hitting an exception, false to stop.
     *         If an exception occurs as part of updating [seconds], the return value is ignored. 
     */
    fun exceptionHandler(throwable: Throwable): Boolean

    /**
     * Called when the [seconds] field updates. It is the responsibility of the implementor to ensure this happens.
     */
    fun onUpdate(oldSeconds: Float, newSeconds: Float) {
        try {
            for (listener in listeners) {
                try {
                    listener.onUpdate(oldSeconds, newSeconds)
                } catch (t: Throwable) {
                    if (!exceptionHandler(t)) {
                        break
                    }
                }
            }
        } catch (t: Throwable) {
            exceptionHandler(t)
        }
    }
    
}

fun interface TimingListener {
    fun onUpdate(oldSeconds: Float, newSeconds: Float)
}

/**
 * A [TimingProvider] where an external object updates the [seconds] field.
 */
class SimpleTimingProvider(private val exceptionHandler: (Throwable) -> Boolean) : TimingProvider {

    @Volatile
    override var seconds: Float = 0f
        set(value) {
            val old = field
            field = value
            onUpdate(old, value)
        }
    override val listeners: MutableList<TimingListener> = CopyOnWriteArrayList()

    override fun exceptionHandler(throwable: Throwable): Boolean {
        return exceptionHandler.invoke(throwable)
    }
}
