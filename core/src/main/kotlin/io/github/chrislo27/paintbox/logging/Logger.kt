package io.github.chrislo27.paintbox.logging


open class Logger {

    val msTimeStarted: Long = System.currentTimeMillis()
    val msTimeElapsed: Long
        get() = System.currentTimeMillis() - msTimeStarted

    enum class LogLevel(val levelNumber: Int) {
        ALL(Int.MIN_VALUE), DEBUG(0), INFO(1), WARN(2), ERROR(3), NONE(Int.MAX_VALUE)
    }

    var loggingLevel: LogLevel = LogLevel.DEBUG

    protected open fun defaultPrint(level: LogLevel, msg: String) {
        val millis = msTimeElapsed
        val second = (millis / 1000) % 60
        val minute = millis / (1000 * 60) % 60
        val hour = millis / (1000 * 60 * 60) % 24
        val text = "${String.format("%02d:%02d:%02d.%03d", hour, minute, second, millis % 1000)}: [${level.name}][${Thread.currentThread().name}] $msg"

        if (level.ordinal >= LogLevel.WARN.ordinal) {
            System.err.println(text)
        } else {
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            System.out.println(text)
        }
    }

    open fun debug(msg: String) {
        if (loggingLevel.levelNumber <= LogLevel.DEBUG.levelNumber) {
            defaultPrint(LogLevel.DEBUG, msg)
        }
    }

    open fun info(msg: String) {
        if (loggingLevel.levelNumber <= LogLevel.INFO.levelNumber) {
            defaultPrint(LogLevel.INFO, msg)
        }
    }

    open fun warn(msg: String) {
        if (loggingLevel.levelNumber <= LogLevel.WARN.levelNumber) {
            defaultPrint(LogLevel.WARN, msg)
        }
    }

    open fun error(msg: String) {
        if (loggingLevel.levelNumber <= LogLevel.ERROR.levelNumber) {
            defaultPrint(LogLevel.ERROR, msg)
        }
    }

}