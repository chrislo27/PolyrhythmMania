package paintbox.util

import com.badlogic.gdx.graphics.Color
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.util.tinyfd.TinyFileDialogs.*
import java.io.File
import java.nio.ByteBuffer


object TinyFDWrapper {

    /**
     * A file extension filter. The [extensions] should be strings in a format like `*.png`, `*.ogg`, etc.
     */
    data class FileExtFilter(val description: String, val extensions: List<String>) {
        fun copyWithExtensionsInDesc(): FileExtFilter =
                this.copy(description = "$description (${
                    extensions.joinToString(separator = ", ") {
                        if (it.startsWith("*.")) it.substring(1) else it
                    }
                })", extensions = extensions)
    }

    private fun File?.toProperPath(): String? {
        if (this == null) return null
        if (this.isDirectory) return this.absolutePath + "/"
        return this.absolutePath
    }

    private fun openFile(title: String, defaultFile: String?, filter: FileExtFilter?): File? {
        return if (filter == null) {
            val path = tinyfd_openFileDialog(title, defaultFile, null, null, false) ?: return null
            File(path)
        } else {
            val stack: MemoryStack = MemoryStack.stackPush()
            stack.use {
                val filterPatterns: PointerBuffer = stack.mallocPointer(filter.extensions.size)
                filter.extensions.forEach {
                    filterPatterns.put(memAddress(stack.UTF8(it)))
                }
                filterPatterns.flip()

                val path = tinyfd_openFileDialog(title, defaultFile, filterPatterns, filter.description, false)
                        ?: return null
                File(path)
            }
        }
    }

    /**
     * Opens an open file chooser dialog that can only select a single file.
     * [function] is called when the dialog is closed/a file is selected.
     */
    fun openFile(title: String, defaultFile: File?, filter: FileExtFilter?, function: (File?) -> Unit) {
        openFile(title, defaultFile.toProperPath(), filter).let(function)
    }

    private fun openMultipleFiles(title: String, defaultFile: String?, filter: FileExtFilter?): List<File>? {
        return if (filter == null) {
            val path = tinyfd_openFileDialog(title, defaultFile, null, null, true) ?: return null
            path.split('|').map { File(it) }
        } else {
            val stack: MemoryStack = MemoryStack.stackPush()
            stack.use {
                val filterPatterns: PointerBuffer = stack.mallocPointer(filter.extensions.size)
                filter.extensions.forEach {
                    filterPatterns.put(memAddress(stack.UTF8(it)))
                }
                filterPatterns.flip()

                val path = tinyfd_openFileDialog(title, defaultFile, filterPatterns, filter.description, true)
                        ?: return null
                path.split('|').map { File(it) }
            }
        }
    }

    /**
     * Opens an open file chooser dialog that can select multiple files.
     * [function] is called when the dialog is closed/file(s) is selected.
     */
    fun openMultipleFiles(title: String, defaultFile: File?, filter: FileExtFilter?, function: (List<File>?) -> Unit) {
        openMultipleFiles(title, defaultFile.toProperPath(), filter).let(function)
    }

    private fun saveFile(title: String, defaultFile: String?, filter: FileExtFilter?): File? {
        return if (filter == null) {
            val path = tinyfd_saveFileDialog(title, defaultFile, null, null) ?: return null
            File(path)
        } else {
            val stack: MemoryStack = MemoryStack.stackPush()
            stack.use {
                val filterPatterns: PointerBuffer = stack.mallocPointer(filter.extensions.size)
                filter.extensions.forEach {
                    filterPatterns.put(memAddress(stack.UTF8(it)))
                }
                filterPatterns.flip()

                val path = tinyfd_saveFileDialog(title, defaultFile, filterPatterns, filter.description) ?: return null
                File(path)
            }
        }
    }

    /**
     * Opens a save file chooser dialog.
     * [function] is called when the dialog is closed/a file is selected.
     */
    fun saveFile(title: String, defaultFile: File?, filter: FileExtFilter?, function: (File?) -> Unit) {
        saveFile(title, defaultFile.toProperPath(), filter).let(function)
    }

    private fun selectFolder(title: String, defaultFolder: String): File? {
        val path = tinyfd_selectFolderDialog(title, defaultFolder) ?: return null
        return File(path)
    }

    /**
     * Opens a select folder/directory chooser dialog.
     * [function] is called when the dialog is closed/a directory is selected.
     */
    fun selectFolder(title: String, defaultFolder: File, function: (File?) -> Unit) {
        selectFolder(title, defaultFolder.toProperPath()!!).let(function)
    }

    /**
     * Opens a colour selection dialog. Returns null if the chooser was cancelled.
     */
    fun selectColor(title: String, defaultHexColor: String): Color? {
        val stack: MemoryStack = MemoryStack.stackPush()
        stack.use {
            val color: ByteBuffer = stack.malloc(3)
            val hex: String? = tinyfd_colorChooser(title, defaultHexColor, null, color) ?: return null
            return Color.valueOf(hex).apply { a = 1f }
        }
    }

    /**
     * Opens a colour selection dialog. Returns null if the chooser was cancelled.
     */
    fun selectColor(title: String, defaultColor: Color?): Color? {
        val stack: MemoryStack = MemoryStack.stackPush()
        stack.use {
            val color: ByteBuffer = stack.malloc(3)
            val def = if (defaultColor == null) "#FFFFFF" else "#${defaultColor.toString().take(6)}"
            val hex: String? = tinyfd_colorChooser(title, def, null, color) ?: return null
            return Color.valueOf(hex).apply { a = 1f }
        }
    }

}
