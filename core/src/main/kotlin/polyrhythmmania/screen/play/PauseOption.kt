package polyrhythmmania.screen.play


data class PauseOption(val localizationKey: String, val enabled: Boolean, val action: () -> Unit)
