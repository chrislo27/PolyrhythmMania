package paintbox.i18n

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.ObjectMap
import paintbox.Paintbox
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import java.util.*


/**
 * Base class for a localization helper. Recommended to create an `object` extension of this class.
 */
abstract class LocalizationBase(val baseHandle: FileHandle, val langDefFile: FileHandle) {

    companion object {
        val DEFAULT_BASE_HANDLE: FileHandle by lazy {
            Gdx.files.internal("localization/default")
        }
        val DEFAULT_LANG_DEFINITION_FILE: FileHandle by lazy {
            Gdx.files.internal("localization/langs.json")
        }
    }

    val bundles: ReadOnlyVar<List<NamedLocaleBundle>> = Var(listOf())
    val currentBundle: Var<NamedLocaleBundle?> = Var(null)
    
    init {
        loadBundles()
        currentBundle.set(bundles.getOrCompute().firstOrNull())
    }
    
    protected fun loadBundles() {
        val list = getBundlesFromLangFile(langDefFile, baseHandle)
        (bundles as Var).set(list)
    }

    open fun reloadAll() {
        val lastBundle = currentBundle.getOrCompute()
        loadBundles()
        if (lastBundle != null) {
            val bundles = bundles.getOrCompute()
            currentBundle.set(bundles.find { it.locale == lastBundle.locale })
        }
    }

    fun getValue(key: String): String {
        val bundle = currentBundle.getOrCompute() ?: return key
        return bundle.getValue(key)
    }

    fun getValue(key: String, vararg args: Any?): String {
        val bundle = currentBundle.getOrCompute() ?: return key
        return bundle.getValue(key, *args)
    }
    
    fun getVar(key: String): ReadOnlyVar<String> {
        return Var {
            currentBundle.use()?.getValue(key) ?: key
        }
    }

    fun getVar(key: String, argsProvider: ReadOnlyVar<List<Any?>>): ReadOnlyVar<String> {
        return Var {
            currentBundle.use()?.getValue(key, *argsProvider.use().toTypedArray()) ?: key
        }
    }

    protected fun createNamedLocaleBundle(locale: NamedLocale, baseHandle: FileHandle): NamedLocaleBundle {
        return NamedLocaleBundle(locale, I18NBundle.createBundle(baseHandle, locale.locale, "UTF-8"))
    }

    protected fun getBundlesFromLangFile(langDefFile: FileHandle, baseHandle: FileHandle): List<NamedLocaleBundle> {
        return Json().fromJson(Array<LanguageObject>::class.java, langDefFile)
                .map(LanguageObject::toNamedLocale)
                .map {
                    createNamedLocaleBundle(it, baseHandle)
                }
    }

    @Suppress("UNCHECKED_CAST")
    fun logMissingLocalizations() {
        val bundles = bundles.getOrCompute()
        val keys: List<String> = bundles.firstOrNull()?.bundle?.let { bundle ->
            val field = bundle::class.java.getDeclaredField("properties")
            field.isAccessible = true
            val map = field.get(bundle) as ObjectMap<String, String>
            map.keys().toList()
        } ?: return
        val missing: List<Pair<NamedLocaleBundle, List<String>>> = bundles.drop(1).map { tbundle ->
            val bundle = tbundle.bundle
            val field = bundle::class.java.getDeclaredField("properties")
            field.isAccessible = true
            val map = (field.get(bundle) as ObjectMap<String, String>).associate { it.key to it.value }

            tbundle to (keys.filter { key -> map.getOrDefault(key, "").isNotBlank() }).sorted()
        }

        missing.filter { it.second.isNotEmpty() }.forEach {
            Paintbox.LOGGER.warn("Missing ${it.second.size} keys for bundle ${it.first.locale}:${it.second.joinToString(separator = "") { i -> "\n  * $i" }}")
        }
    }

}