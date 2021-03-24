package io.github.chrislo27.paintbox

import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.util.Version
import io.github.chrislo27.paintbox.util.WindowSize
import java.io.File


data class PaintboxSettings(val launchArguments: List<String>,
                            val logger: Logger, val logToFile: File?,
                            val version: Version,
                            val emulatedSize: WindowSize, val resizeAction: ResizeAction,
                            val minimumSize: WindowSize)
