package polyrhythmmania.soundsystem.javasound

import java.lang.StringBuilder
import javax.sound.sampled.*


object DumpAudioDebugInfo {
    
    fun dump() {
        val strBuilder = StringBuilder()
        var indent = 0
        fun addIndent() {
            indent++
        }
        fun removeIndent() {
            indent = (indent - 1).coerceAtLeast(0)
        }
        fun StringBuilder.println(string: String = ""): StringBuilder = this.append("  ".repeat(indent)).append(string).append("\n")
        val separator = "=".repeat(80)
        
        strBuilder.println(separator)
        strBuilder.println("DumpAudioDebugInfo")
        strBuilder.println(separator)
        strBuilder.println("\n\n")

        fun outputInfoForMixerInfo(mixerInfo: Mixer.Info?) {
            if (mixerInfo != null) {
                strBuilder.println("Name: ${mixerInfo.name}")
                strBuilder.println("Desc: ${mixerInfo.description}")
                strBuilder.println("Vendor: ${mixerInfo.vendor}")
                strBuilder.println("Version: ${mixerInfo.version}")
            } else {
                strBuilder.println("(mixerInfo is null)")
            }
        }
        
        val mixerInfos = AudioSystem.getMixerInfo().toList()
        try {
            mixerInfos.forEachIndexed { index, mixerInfo ->
                strBuilder.println("Mixer Info $index:")
                addIndent()
                outputInfoForMixerInfo(mixerInfo)
                removeIndent()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        strBuilder.println()
        
        fun outputInfoForMixer(mixer: Mixer) {
            strBuilder.println("toString(): $mixer")
            strBuilder.println("Mixer.Info:")
            addIndent()
            try {
                outputInfoForMixerInfo(mixer.mixerInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            removeIndent()
            
            fun printLineInfos(lines: List<Line.Info?>) {
                for (lineInfo: Line.Info? in lines) {
                    if (lineInfo == null) {
                        strBuilder.println("(lineInfo was null)")
                        continue
                    }
                    try {
                        strBuilder.println(lineInfo.toString())
                        addIndent()
                        strBuilder.println(lineInfo::class.java.name)
                        strBuilder.println("Is supported: ${mixer.isLineSupported(lineInfo)}")
                        strBuilder.println("Max lines open: ${mixer.getMaxLines(lineInfo)}")
                        
                        if (lineInfo is DataLine.Info) {
                            strBuilder.println("Extra DataLine.Info info:")
                            addIndent()
                            
                            strBuilder.println("Min/max buffer sizes: min ${lineInfo.minBufferSize}, max ${lineInfo.maxBufferSize}")
                            val audioFormats = lineInfo.formats.toList()
                            strBuilder.println("Audio formats: ${audioFormats.size}")
                            addIndent()
                            audioFormats.forEachIndexed { index, audioFormat: AudioFormat? ->  
                                strBuilder.println("$index: ${audioFormat?.toString() ?: "(null)"}")
                            }
                            removeIndent()
                            
                            removeIndent()
                        }
                        removeIndent()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            val wasOpen = mixer.isOpen
            if (!wasOpen) {
                try {
                    mixer.open()
                } catch (ignored: LineUnavailableException) {
                } catch (e: Exception) {
                    strBuilder.println("Exception when trying to open mixer")
                    e.printStackTrace()
                }
            }
            strBuilder.println("Was open: $wasOpen")
            strBuilder.println("Is open: ${mixer.isOpen}")
            
            
            val sourceLines = mixer.sourceLineInfo.toList()
            strBuilder.println("Supported source line infos: ${sourceLines.size}")
            addIndent()
            try {
                printLineInfos(sourceLines)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            removeIndent()
            
            val targetLines = mixer.targetLineInfo.toList()
            strBuilder.println("Supported target line infos: ${targetLines.size}")
            addIndent()
            try {
                printLineInfos(targetLines)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            removeIndent()
            
            if (mixer.isOpen && !wasOpen) {
                try {
                    mixer.close()
                } catch (ignored: Exception) {
                }
            }
            
            strBuilder.println()
        }


        strBuilder.println("Mixer for system default:")
        addIndent()
        try {
            val mixer = AudioSystem.getMixer(null)
            outputInfoForMixer(mixer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        removeIndent()
        mixerInfos.filterNotNull().forEachIndexed { index, info ->
            strBuilder.println("Mixer for info $index:")
            addIndent()
            try {
                val mixer = AudioSystem.getMixer(info)
                outputInfoForMixer(mixer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            removeIndent()
        }
        
        strBuilder.println(separator)
        
        println(strBuilder.toString())
    }
    
}