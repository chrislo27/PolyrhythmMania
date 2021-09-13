package polyrhythmmania.container

import com.badlogic.gdx.utils.Disposable
import java.io.File


/**
 * An [ExternalResource] is effectively a non-system file that has to be kept with a [Container].
 *
 * This could be things such as the (compressed) music used.
 *
 * This class is [Disposable]. All open streams, file channels, etc should be closed.
 * The [shouldBeDeletedWhenDisposed] indicates if the file will also be deleted when disposed (for temp files).
 */
class ExternalResource(
        val key: String,
        val file: File,
        val shouldBeDeletedWhenDisposed: Boolean
) : Disposable {

    private val disposalListeners: MutableSet<DisposalListener> = mutableSetOf()

    fun addDisposalListener(listener: DisposalListener) {
        synchronized(disposalListeners) {
            disposalListeners.add(listener)
        }
    }

    fun removeDisposalListener(listener: DisposalListener) {
        synchronized(disposalListeners) {
            disposalListeners.remove(listener)
        }
    }

    override fun dispose() {
        disposalListeners.toList().forEach { it.dispose(this) }
        if (shouldBeDeletedWhenDisposed) {
            file.delete()
        }
    }
}

fun interface DisposalListener {

    fun dispose(res: ExternalResource)

}
