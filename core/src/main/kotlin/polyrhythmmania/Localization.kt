package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import paintbox.i18n.ILocalizationWithBundle
import paintbox.i18n.LocalePickerBase
import paintbox.i18n.LocalizationBase


object PRManiaLocalePicker : LocalePickerBase(
    if (PRMania.isDevVersion || PRMania.isPrereleaseVersion)
        Gdx.files.internal("localization/langs.json")
    else Gdx.files.internal("localization/langs_en-only.json")
)


abstract class PRManiaLocalizationBase(baseHandle: FileHandle)
    : LocalizationBase(baseHandle, PRManiaLocalePicker)

object BaseLocalization
    : PRManiaLocalizationBase(LocalizationBase.DEFAULT_BASE_HANDLE)

object Localization : ILocalizationWithBundle by BaseLocalization

