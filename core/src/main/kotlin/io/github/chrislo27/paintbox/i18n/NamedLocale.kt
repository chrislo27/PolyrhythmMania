package io.github.chrislo27.paintbox.i18n

import java.util.*


data class NamedLocale(val name: String, val locale: Locale) {

    override fun toString(): String {
        return "$name ($locale)"
    }
}