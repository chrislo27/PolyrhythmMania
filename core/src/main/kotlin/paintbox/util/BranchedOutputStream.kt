package paintbox.util

import java.io.FilterOutputStream
import java.io.OutputStream


class BranchedOutputStream(out: OutputStream, val branch: OutputStream)
    : FilterOutputStream(out) {

    override fun close() {
        try {
            out.close()
        } finally {
            branch.close()
        }
    }

    override fun flush() {
        out.flush()
        branch.flush()
    }

    @Synchronized override fun write(b: Int) {
        out.write(b)
        branch.write(b)
    }

    @Synchronized override fun write(b: ByteArray) {
        out.write(b)
        branch.write(b)
    }

    @Synchronized override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        branch.write(b, off, len)
    }
}