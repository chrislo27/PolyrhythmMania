package polyrhythmmania

import com.badlogic.gdx.Gdx
import paintbox.i18n.LocalizationBase


object Localization
    : LocalizationBase(LocalizationBase.DEFAULT_BASE_HANDLE, LocalizationBase.DEFAULT_LANG_DEFINITION_FILE)

object UpdateNotesL10N
    : LocalizationBase(Gdx.files.internal("localization/update_notes"), LocalizationBase.DEFAULT_LANG_DEFINITION_FILE)