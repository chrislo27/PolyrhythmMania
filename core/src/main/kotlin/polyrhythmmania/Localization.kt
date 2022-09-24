package polyrhythmmania

import com.badlogic.gdx.Gdx
import paintbox.i18n.LocalePickerBase
import paintbox.i18n.LocalizationBase


object LocalePicker
    : LocalePickerBase(LocalePickerBase.DEFAULT_LANG_DEFINITION_FILE)

object Localization
    : LocalizationBase(LocalizationBase.DEFAULT_BASE_HANDLE, LocalePicker)

object UpdateNotesL10N
    : LocalizationBase(Gdx.files.internal("localization/update_notes"), LocalePicker)