package polyrhythmmania.container.manifest


data class SaveOptions(val isProject: Boolean, val isAutosave: Boolean, val exportStatistics: ExportStatistics?) {
    
    companion object {
        val EDITOR_AUTOSAVE: SaveOptions = SaveOptions(isProject = true, isAutosave = true, exportStatistics = null)
        val EDITOR_SAVE_AS_PROJECT: SaveOptions = SaveOptions(isProject = true, isAutosave = false, exportStatistics = null)
        
        val SHUTDOWN_RECOVERY: SaveOptions = SaveOptions(isProject = true, isAutosave = true, exportStatistics = null)
        val CRASH_RECOVERY: SaveOptions = SHUTDOWN_RECOVERY
        
        fun editorExportAsLevel(statistics: ExportStatistics): SaveOptions {
            return SaveOptions(isProject = false, isAutosave = false, exportStatistics = statistics)
        }
    }
}
