package paintbox

import paintbox.logging.Logger
import paintbox.util.Version
import paintbox.util.WindowSize
import java.io.File


data class PaintboxSettings(val launchArguments: List<String>,
                            val logger: Logger, val logToFile: File?,
                            val version: Version,
                            val emulatedSize: WindowSize, val resizeAction: ResizeAction,
                            val minimumSize: WindowSize)
