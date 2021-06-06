package paintbox.util


object MemoryUtils {

    val usedMemoryB: Long
        get() = (Runtime.getRuntime().totalMemory())

    val maxMemoryB: Long
        get() = (Runtime.getRuntime().maxMemory())

    val freeMemoryB: Long
        get() = (Runtime.getRuntime().freeMemory())

    val usedMemoryKiB: Long
        get() = (Runtime.getRuntime().totalMemory() / 1024)

    val maxMemoryKiB: Long
        get() = (Runtime.getRuntime().maxMemory() / 1024)

    val freeMemoryKiB: Long
        get() = (Runtime.getRuntime().freeMemory() / 1024)
    
    val usedMemoryMiB: Long
        get() = (Runtime.getRuntime().totalMemory() / 1_048_578)

    val maxMemoryMiB: Long
        get() = (Runtime.getRuntime().maxMemory() / 1_048_578)

    val freeMemoryMiB: Long
        get() = (Runtime.getRuntime().freeMemory() / 1_048_578)
    
}