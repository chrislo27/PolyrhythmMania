package paintbox.logging


open class Logger {

    enum class LogLevel(val levelNumber: Int) {
        ALL(Int.MIN_VALUE), TRACE(-1), DEBUG(0), INFO(1), WARN(2), ERROR(3), NONE(Int.MAX_VALUE)
    }

    val msTimeStarted: Long = System.currentTimeMillis()
    val msTimeElapsed: Long
        get() = System.currentTimeMillis() - msTimeStarted

    var loggingLevel: LogLevel = LogLevel.DEBUG

    protected open fun defaultPrint(level: LogLevel, msg: String, tag: String, throwable: Throwable?) {
        val millis = msTimeElapsed
        val second = (millis / 1000) % 60
        val minute = millis / (1000 * 60) % 60
        val hour = millis / (1000 * 60 * 60) % 24
        val text = "${String.format("%02d:%02d:%02d.%03d", hour, minute, second, millis % 1000)}: [${level.name}][${Thread.currentThread().name}] ${if (tag.isEmpty()) "" else "[$tag] "}$msg"

        if (level.ordinal >= LogLevel.WARN.ordinal) {
            System.err.println(text)
        } else {
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            System.out.println(text)
        }

        throwable?.printStackTrace(System.err)
    }

    fun log(logLevel: LogLevel, msg: String, tag: String = "", throwable: Throwable? = null) {
        if (loggingLevel.levelNumber <= logLevel.levelNumber) {
            defaultPrint(logLevel, msg, tag, throwable)
        }
    }

    // Special methods for each log level

    fun trace(msg: String, tag: String = "", throwable: Throwable? = null) {
        log(LogLevel.TRACE, msg, tag, throwable)
    }

    fun debug(msg: String, tag: String = "", throwable: Throwable? = null) {
        log(LogLevel.DEBUG, msg, tag, throwable)
    }

    fun info(msg: String, tag: String = "", throwable: Throwable? = null) {
        log(LogLevel.INFO, msg, tag, throwable)
    }

    fun warn(msg: String, tag: String = "", throwable: Throwable? = null) {
        log(LogLevel.WARN, msg, tag, throwable)
    }

    fun error(msg: String, tag: String = "", throwable: Throwable? = null) {
        log(LogLevel.ERROR, msg, tag, throwable)
    }

    // Optimized methods for each log level without going through defaults
    // No throwable param: no tag and with tag.

    fun trace(msg: String) {
        log(LogLevel.TRACE, msg, "", null)
    }

    fun debug(msg: String) {
        log(LogLevel.DEBUG, msg, "", null)
    }

    fun info(msg: String) {
        log(LogLevel.INFO, msg, "", null)
    }

    fun warn(msg: String) {
        log(LogLevel.WARN, msg, "", null)
    }

    fun error(msg: String) {
        log(LogLevel.ERROR, msg, "", null)
    }

    fun trace(msg: String, tag: String) {
        log(LogLevel.TRACE, msg, tag, null)
    }

    fun debug(msg: String, tag: String) {
        log(LogLevel.DEBUG, msg, tag, null)
    }

    fun info(msg: String, tag: String) {
        log(LogLevel.INFO, msg, tag, null)
    }

    fun warn(msg: String, tag: String) {
        log(LogLevel.WARN, msg, tag, null)
    }

    fun error(msg: String, tag: String) {
        log(LogLevel.ERROR, msg, tag, null)
    }

}