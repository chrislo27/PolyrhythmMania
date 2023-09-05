package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import paintbox.i18n.LocalePickerBase
import paintbox.i18n.LocalizationBase
import paintbox.i18n.LocalizationGroup


object PRManiaLocalePicker : LocalePickerBase(
    if (PRMania.isDevVersion || PRMania.isPrereleaseVersion)
        Gdx.files.internal("localization/langs.json")
    else Gdx.files.internal("localization/langs_en-only.json")
)


abstract class PRManiaLocalizationBase(baseHandle: FileHandle)
    : LocalizationBase(baseHandle, PRManiaLocalePicker)

private object BaseLocalization
    : PRManiaLocalizationBase(Gdx.files.internal("localization/default"))

object Localization : LocalizationGroup(PRManiaLocalePicker, listOf(
    BaseLocalization,
//    EditorHelpLocalization,
//    AchievementsL10N,
//    UpdateNotesL10N,
//    StoryL10N,
))
