package paintbox.util


fun Pair<Int, Int>.toWindowSize(): WindowSize = WindowSize(first, second)

data class WindowSize(val width: Int, val height: Int) {
    fun toPair(): Pair<Int, Int> = width to height

    override fun toString(): String {
        return "WindowSize=${width}x${height}"
    }
}
