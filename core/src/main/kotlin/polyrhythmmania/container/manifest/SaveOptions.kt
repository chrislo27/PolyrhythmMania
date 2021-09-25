package polyrhythmmania.container.manifest


data class SaveOptions(val isProject: Boolean, val isAutosave: Boolean) {
    companion object {
        val EDITOR_AUTOSAVE: SaveOptions = SaveOptions(isProject = true, isAutosave = true)
        val EDITOR_SAVE_AS_PROJECT: SaveOptions = SaveOptions(isProject = true, isAutosave = false)
        
        val SHUTDOWN_RECOVERY: SaveOptions = SaveOptions(isProject = true, isAutosave = true)
        val CRASH_RECOVERY: SaveOptions = SHUTDOWN_RECOVERY
    }
}
