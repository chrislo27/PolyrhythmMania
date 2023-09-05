package polyrhythmmania.statistics

import paintbox.binding.ReadOnlyIntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.i18n.ILocalization
import polyrhythmmania.Localization

open class LocalizedStatFormatter(
    val localizationKey: String,
    val localizationBase: ILocalization = Localization,
) : StatFormatter {

    companion object {

        val DEFAULT: LocalizedStatFormatter = LocalizedStatFormatter("statistics.formatter.default", Localization)
    }

    override fun format(value: ReadOnlyIntVar): ReadOnlyVar<String> {
        return localizationBase.getVar(localizationKey, Var { listOf(value.use()) })
    }
}