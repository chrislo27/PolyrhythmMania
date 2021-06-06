package paintbox.registry

import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.Disposable
import paintbox.PaintboxScreen


object ScreenRegistry : Disposable {

    val screens: Map<String, PaintboxScreen> = mutableMapOf()

    operator fun get(key: String): PaintboxScreen? {
        return screens[key]
    }

    fun getNonNull(key: String): PaintboxScreen {
        return get(key) ?: throw IllegalArgumentException("No screen found with key $key")
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified S : PaintboxScreen> getAs(key: String): S? {
        return screens[key] as? S?
    }

    inline fun <reified S : PaintboxScreen> getNonNullAs(key: String): S =
            getAs<S>(key) ?: throw IllegalArgumentException("No screen found with key $key")

    operator fun plusAssign(pair: Pair<String, PaintboxScreen>) {
        add(pair.first, pair.second)
    }

    fun add(key: String, screen: PaintboxScreen) {
        if (screens.containsKey(key)) {
            throw IllegalArgumentException("Already contains key $key")
        }
        (screens as MutableMap)[key] = screen
    }

    fun remove(key: String) {
        (screens as MutableMap).remove(key)?.dispose()
    }

    override fun dispose() {
        screens.values.forEach(Screen::dispose)
    }

}