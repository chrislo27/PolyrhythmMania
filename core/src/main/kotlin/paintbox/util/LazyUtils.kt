package paintbox.util

import kotlin.reflect.KProperty


interface SettableLazy<T> {
    /**
     * The value will be initialized on first-get (but NOT first-set).
     */
    var value: T

    fun isInitialized(): Boolean

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        if (!isInitialized()) {
            value
        }
        value = newValue
    }
}


private class SettableLazyImpl<T>(private val initBlock: () -> T) : SettableLazy<T> {

    private val lock: Any = this
    
    @Volatile
    private var inited: Boolean = false
    
    @Volatile
    private var backing: T? = null

    override var value: T
        get() {
            if (!isInitialized()) {
                initFirstTime()
            }
            
            @Suppress("UNCHECKED_CAST")
            return (backing as T)
        }
        set(value) {
            synchronized(lock) {
                backing = value
                inited = true
            }
        }

    private fun initFirstTime() {
        synchronized(lock) {
            if (!inited) {
                backing = initBlock()
                inited = true
            }
        }
    }

    override fun isInitialized(): Boolean {
        return inited
    }

}

fun <T> settableLazy(initBlock: () -> T): SettableLazy<T> = SettableLazyImpl(initBlock)