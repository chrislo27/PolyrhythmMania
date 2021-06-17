package polyrhythmmania.editor

import polyrhythmmania.Localization


interface EditorSetting {
    val persistValueID: String
}
enum class CameraPanningSetting(override val persistValueID: String, val localization: String): EditorSetting {
    PAN("pan", "editorSettings.cameraPanningSetting.pan"),
    FOLLOW("follow", "editorSettings.cameraPanningSetting.follow");

    override fun toString(): String {
        return Localization.getValue(localization)
    }

    companion object {
        val VALUES: List<CameraPanningSetting> = values().toList()
        val MAP: Map<String, CameraPanningSetting> = VALUES.associateBy { it.persistValueID }
    }
}
