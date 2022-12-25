package polyrhythmmania.soundsystem.sample

import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean


class ByteArraySeekableByteChannel(val array: ByteArray) : SeekableByteChannel {

    private val closed: AtomicBoolean = AtomicBoolean(false)
    private var position: Int = 0
    
    override fun isOpen(): Boolean {
        return !closed.get()
    }

    override fun read(dst: ByteBuffer?): Int {
        if (dst == null) throw IllegalArgumentException("Destination ByteBuffer is null")
        
        checkIsOpen()

        val available = array.size - position
        if (available <= 0) return -1
        val targetBytes = dst.remaining().coerceAtMost(available)
        
        dst.put(array, position, targetBytes)
        this.position += targetBytes
        
        return targetBytes
    }

    override fun write(src: ByteBuffer?): Int {
        throw NonWritableChannelException()
    }

    override fun position(): Long {
        checkIsOpen()
        return position.toLong()
    }

    override fun position(newPosition: Long): ByteArraySeekableByteChannel {
        checkIsOpen()
        if (newPosition !in 0..Int.MAX_VALUE) 
            throw IllegalArgumentException("newPosition must be a positive Int (got ${newPosition})")
        
        this.position = newPosition.toInt()
        
        return this
    }

    override fun size(): Long {
        return array.size.toLong()
    }

    override fun truncate(size: Long): SeekableByteChannel {
        throw NonWritableChannelException()
    }
    
    override fun close() {
        closed.set(true)
    }

    private fun checkIsOpen() {
        if (closed.get()) throw ClosedChannelException()
    }
}
