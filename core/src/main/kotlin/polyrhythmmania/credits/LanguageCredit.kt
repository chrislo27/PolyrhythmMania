package polyrhythmmania.credits

import java.util.*


data class LanguageCredit(
        val locale: Locale,
        /**
         * Main localizer(s) for this language
         */
        val primaryLocalizers: List<String>,
        /**
         * Other people who helped with the localization, perhaps for double-checking, etc
         */
        val secondaryLocalizers: List<String>,
)
