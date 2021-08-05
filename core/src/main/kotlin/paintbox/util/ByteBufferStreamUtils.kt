package paintbox.util

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.min

// https://stackoverflow.com/questions/4332264/wrapping-a-bytebuffer-with-an-inputstream/6603018#6603018

class ByteBufferBackedInputStream(buf: ByteBuffer) : InputStream() {
    var buf: ByteBuffer
    
    override fun read(): Int {
        return if (!buf.hasRemaining()) {
            -1
        } else buf.get().toInt() and 0xFF
    }

    override fun read(bytes: ByteArray?, off: Int, len: Int): Int {
        var len = len
        if (!buf.hasRemaining()) {
            return -1
        }
        len = min(len, buf.remaining())
        buf.get(bytes, off, len)
        return len
    }

    init {
        this.buf = buf
    }
}

class ByteBufferBackedOutputStream(var buf: ByteBuffer) : OutputStream() {
    override fun write(b: Int) {
        buf.put(b.toByte())
    }

    override fun write(bytes: ByteArray?, off: Int, len: Int) {
        buf.put(bytes, off, len)
    }
}
