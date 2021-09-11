package polyrhythmmania.discord.util

import com.badlogic.gdx.graphics.Pixmap
import de.jcm.discordgamesdk.ImageManager
import de.jcm.discordgamesdk.image.ImageDimensions
import de.jcm.discordgamesdk.image.ImageHandle


fun ImageManager.getDataAsPixmap(handle: ImageHandle,
                                 dimensions: ImageDimensions = this.getDimensions(handle)): Pixmap {
    val data: ByteArray = this.getData(handle, dimensions)
    return Pixmap(dimensions.width, dimensions.height, Pixmap.Format.RGBA8888).also { pixmap ->
        val buffer = pixmap.pixels
        buffer.rewind()
        buffer.put(data)
    }
}