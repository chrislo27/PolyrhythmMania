package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import paintbox.i18n.LocalePickerBase
import paintbox.i18n.LocalizationBase


object PRManiaLocalePicker : LocalePickerBase(
    if (PRMania.isDevVersion || PRMania.isPrereleaseVersion)
        Gdx.files.internal("localization/langs.json")
    else Gdx.files.internal("localization/langs_en-only.json")
)


abstract class PRManiaLocalizationBase(baseHandle: FileHandle, localePicker: LocalePickerBase)
    : LocalizationBase(baseHandle, localePicker)

object Localization
    : PRManiaLocalizationBase(LocalizationBase.DEFAULT_BASE_HANDLE, PRManiaLocalePicker)

object UpdateNotesL10N
    : PRManiaLocalizationBase(Gdx.files.internal("localization/update_notes"), PRManiaLocalePicker)
