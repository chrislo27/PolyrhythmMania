package polyrhythmmania.container

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StreamUtils
import java.io.File
import java.io.InputStream


/**
 * An [ExternalResource] is effectively a non-system file that has to be kept with a [Container].
 *
 * This could be things such as the (compressed) music used.
 *
 * This class is [Disposable]. All open streams, file channels, etc should be closed.
 * The [shouldFileBeDeletedWhenDisposed] property indicates if the file will also be deleted when disposed (for temp files).
 */
class ExternalResource(
        val key: String,
        val file: File,
        val shouldFileBeDeletedWhenDisposed: Boolean
) : Disposable {

    private val reader: InputStream = file.inputStream()
    private val disposalListeners: MutableSet<ExtResDisposalListener> = mutableSetOf()

    fun addDisposalListener(listener: ExtResDisposalListener) {
        synchronized(disposalListeners) {
            disposalListeners.add(listener)
        }
    }

    fun removeDisposalListener(listener: ExtResDisposalListener) {
        synchronized(disposalListeners) {
            disposalListeners.remove(listener)
        }
    }

    override fun dispose() {
        disposalListeners.toList().forEach { it.dispose(this) }
        StreamUtils.closeQuietly(reader)
        if (shouldFileBeDeletedWhenDisposed) {
            file.delete()
        }
    }
}

fun interface ExtResDisposalListener {

    fun dispose(res: ExternalResource)

}
