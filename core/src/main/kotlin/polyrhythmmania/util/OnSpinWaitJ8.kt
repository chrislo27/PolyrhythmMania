package polyrhythmmania.util

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType


/**
 * A simple Thread.onSpinWait adapter for JRE 8.
 * TODO To be deleted once the project moves to JDK 11.
 */
object OnSpinWaitJ8 {
    private val ON_SPIN_WAIT_HANDLE: MethodHandle? = try {
        MethodHandles.lookup().findStatic(Thread::class.java, "onSpinWait", MethodType.methodType(Void::class.java))
    } catch (ignored: Throwable) {
        null
    }

    /**
     * Attempts to call Thread.onSpinWait. Returns true if it did succeed, false otherwise.
     */
    fun onSpinWait(): Boolean {
        if (ON_SPIN_WAIT_HANDLE != null) {
            try {
                ON_SPIN_WAIT_HANDLE.invokeExact()
                return true
            } catch (ignored: Throwable) {
            }
        }
        return false
    }
}