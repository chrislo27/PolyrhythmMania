package polyrhythmmania.storymode.music

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


object StemLoader {
    
    private const val MAX_POOL_SIZE: Int = 4
    private const val MAX_KEEP_ALIVE_TIME_SEC: Long = 10L
    
    private val threadID: AtomicInteger = AtomicInteger(0)
    // Executor scales between 0 and MAX_POOL_SIZE threads, with threads shutting down after MAX_KEEP_ALIVE_TIME_SEC. Core thread timeouts enabled
    private val executor: ExecutorService = ThreadPoolExecutor(MAX_POOL_SIZE, MAX_POOL_SIZE, MAX_KEEP_ALIVE_TIME_SEC, TimeUnit.SECONDS, LinkedBlockingQueue()) { runnable ->
        Thread(runnable).apply {
            this.priority = 6
            this.isDaemon = true
            this.name = "StemLoader-${threadID.getAndIncrement()}"
        }
    }.apply { 
        this.allowCoreThreadTimeOut(true)
    }
    
    fun enqueue(runnable: Runnable) {
        executor.submit(runnable)
    }
}