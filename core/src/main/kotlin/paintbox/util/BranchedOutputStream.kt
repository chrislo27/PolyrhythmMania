package paintbox.util

import java.io.FilterOutputStream
import java.io.OutputStream


class BranchedOutputStream(out: OutputStream, val branch: OutputStream)
    : FilterOutputStream(out) {

    override fun close() {
        try {
            super.close()
        } finally {
            branch.close()
        }
    }

    override fun flush() {
        super.flush()
        branch.close()
    }

    @Synchronized override fun write(b: Int) {
        super.write(b)
        branch.write(b)
    }

    @Synchronized override fun write(b: ByteArray) {
        super.write(b)
        branch.write(b)
    }

    @Synchronized override fun write(b: ByteArray, off: Int, len: Int) {
        super.write(b, off, len)
        branch.write(b, off, len)
    }
}