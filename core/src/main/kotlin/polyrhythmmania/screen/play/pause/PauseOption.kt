package polyrhythmmania.screen.play.pause


data class PauseOption(val localizationKey: String, val enabled: Boolean, val action: () -> Unit)
