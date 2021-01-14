@file:Suppress("unused", "DeferredIsResult")

package org.itxtech.miraijs.plugin.libs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import net.mamoe.kjbb.JvmBlockingBridge
import org.itxtech.miraijs.plugin.PluginLib
import java.util.*

@Suppress("FunctionName", "ClassName", "MemberVisibilityCanBePrivate")
object KotlinCoroutineLib : PluginLib() {
    override val nameInJs = "coroutine"

    @JvmField
    val Dispatchers = kotlinx.coroutines.Dispatchers

    @JvmField
    val CoroutineContext = kotlin.coroutines.CoroutineContext::class.java

    fun createSupervisorJob(parent: kotlinx.coroutines.Job?) =
        SupervisorJobJsImpl(kotlinx.coroutines.SupervisorJob(parent))

    fun createSupervisorJob() = createSupervisorJob(null)
    class SupervisorJobJsImpl(private val supervisorJob: kotlinx.coroutines.CompletableJob) {
        @JvmBlockingBridge
        suspend fun join() = supervisorJob.join()
        fun cancel(reason: String) = supervisorJob.cancel(cause = kotlinx.coroutines.CancellationException(reason))

        @JvmBlockingBridge
        suspend fun cancelAndJoin() = supervisorJob.cancelAndJoin()
        fun isActive() = supervisorJob.isActive
        fun isCanceled() = supervisorJob.isCancelled
        fun isCompleted() = supervisorJob.isCompleted
        fun complete() = supervisorJob.complete()
        fun completeExceptionally(reason: String) = supervisorJob.completeExceptionally(Throwable(reason))
    }

    fun createJob(parent: kotlinx.coroutines.Job?) = JobJsImpl(kotlinx.coroutines.Job(parent))
    class JobJsImpl(private val job: kotlinx.coroutines.Job) {
        @JvmBlockingBridge
        suspend fun join() = job.join()
        fun cancel(reason: String) = job.cancel(cause = kotlinx.coroutines.CancellationException(reason))

        @JvmBlockingBridge
        suspend fun cancelAndJoin() = job.cancelAndJoin()
        fun isActive() = job.isActive
        fun isCanceled() = job.isCancelled
        fun isCompleted() = job.isCompleted
    }

    fun createJob() = createJob(null)

    @OptIn(kotlinx.coroutines.ObsoleteCoroutinesApi::class)
    fun newSingleThreadContext(name: String) = kotlinx.coroutines.newSingleThreadContext(name)

    @OptIn(kotlinx.coroutines.ObsoleteCoroutinesApi::class)
    fun newFixedThreadPoolContext(nThreads: Int, name: String) =
        kotlinx.coroutines.newFixedThreadPoolContext(nThreads, name)

    fun CoroutineScope(coroutineContext: kotlin.coroutines.CoroutineContext) =
        kotlinx.coroutines.CoroutineScope(coroutineContext)

    @JvmBlockingBridge
    suspend fun coroutineScope(samCallback: FunctionCoroutineScopeSAMCallback) =
        kotlinx.coroutines.coroutineScope { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun supervisorScope(samCallback: FunctionCoroutineScopeSAMCallback) =
        kotlinx.coroutines.supervisorScope { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun <T> suspendCoroutine(samCallback: FunctionContinuationSAMCallback<T>): T =
        kotlin.coroutines.suspendCoroutine { samCallback.call(it) }

    @JvmBlockingBridge
    suspend fun delay(timeMills: Long) = kotlinx.coroutines.delay(timeMills)

    @JvmBlockingBridge
    suspend fun yield() = kotlinx.coroutines.yield()
    fun launchFromGlobalScope(samCallback: FunctionCoroutineScopeSAMCallback) =
        kotlinx.coroutines.GlobalScope.launch { samCallback.call(this) }

    fun launch(coroutineScope: kotlinx.coroutines.CoroutineScope, samCallback: FunctionCoroutineScopeSAMCallback) =
        coroutineScope.launch(coroutineScope.coroutineContext) { samCallback.call(this) }

    fun <T> asyncFromGlobalScope(samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>): kotlinx.coroutines.Deferred<T> =
        kotlinx.coroutines.GlobalScope.async { samCallback.call(this) }

    fun <T> async(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = DeferredJsImpl(coroutineScope.async(coroutineScope.coroutineContext) { samCallback.call(this) })

    class DeferredJsImpl<T>(private val deferred: kotlinx.coroutines.Deferred<T>) {
        @JvmBlockingBridge
        suspend fun await(): T = deferred.await()

        @ExperimentalCoroutinesApi
        fun getCompleted() = deferred.getCompleted()
    }


    @JvmBlockingBridge
    suspend fun <T> withContext(
        coroutineContext: kotlin.coroutines.CoroutineContext,
        samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = kotlinx.coroutines.withContext(coroutineContext) { samCallback.call(this) }

    /* kotlin.coroutines.withTimeout doesn't work for JavaScript caller. */
    @ExperimentalCoroutinesApi
    @JvmBlockingBridge
    suspend fun <T> withTimeout(timeMills: Long, samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>) =
        kotlinx.coroutines.coroutineScope functionReturn@{
            val deferred = async { samCallback.call(this) }.also { this.launch { it.await() } }
            delay(timeMills)
            try {
                return@functionReturn deferred.getCompleted()
            } catch (ex: IllegalStateException) {
                deferred.cancel()
                throw Exception("Time out waiting for $timeMills ms.")
            }
        }

    @JvmField
    val channel = object {
        @JvmField
        val BufferOverflow = object {
            @JvmField
            val DROP_LATEST = kotlinx.coroutines.channels.BufferOverflow.DROP_LATEST

            @JvmField
            val DROP_OLDEST = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST

            @JvmField
            val SUSPEND = kotlinx.coroutines.channels.BufferOverflow.SUSPEND
        }

        @JvmField
        val ChannelFactory = kotlinx.coroutines.channels.Channel.Factory
        fun <T> createChannel(): ChannelJsImpl<T> =
            createChannel(ChannelFactory.RENDEZVOUS, BufferOverflow.SUSPEND, null)

        fun <T> createChannel(capacity: Int): ChannelJsImpl<T> =
            createChannel(capacity = capacity, BufferOverflow.SUSPEND, null)

        fun <T> createChannel(samCallback: FunctionChannelOnDeliverElementSAMCallback<T>): ChannelJsImpl<T> =
            createChannel(ChannelFactory.RENDEZVOUS, BufferOverflow.SUSPEND, samCallback)

        fun <T> createChannel(
            capacity: Int = ChannelFactory.RENDEZVOUS,
            onBufferOverflow: kotlinx.coroutines.channels.BufferOverflow,
            samCallback: FunctionChannelOnDeliverElementSAMCallback<T>?
        ): ChannelJsImpl<T> =
            ChannelJsImpl(
                kotlinx.coroutines.channels.Channel(
                    capacity,
                    onBufferOverflow,
                    if (samCallback == null) null else ({ samCallback.call(it) })
                )
            )
    }

    class ChannelJsImpl<T>(private val channel: kotlinx.coroutines.channels.Channel<T>) {
        @OptIn(ExperimentalCoroutinesApi::class)
        fun isClosedForReceive() = channel.isClosedForReceive

        @OptIn(ExperimentalCoroutinesApi::class)
        fun isClosedForSend() = channel.isClosedForSend

        @JvmBlockingBridge
        suspend fun receive(samCallback: FunctionChannelOnDeliverElementSAMCallback<T>) {
            while (!isClosedForReceive()) {
                try {
                    samCallback.call(channel.receive())
                } catch (ex: Exception) {
                    return
                }
            }
        }

        @JvmBlockingBridge
        suspend fun send(value: T) = channel.send(value)
        fun close() = channel.close()
    }

    @JvmField
    val flow = object {
        fun <T> createFlow(samCallback: FunctionFlowActionSAMCallback<T>): FlowJsImpl<T> =
            FlowJsImpl(flow { samCallback.call(FlowCollectorJsImpl(this)) })
    }

    class FlowCollectorJsImpl<T>(val flowCollector: FlowCollector<T>) {
        @JvmBlockingBridge
        suspend fun emit(value: T) = flowCollector.emit(value)
    }

    class FlowJsImpl<T>(val flow: Flow<T>) {
        /* Every transform operation returns a new FlowJsImpl,
        * because internal transform creates a new Flow */
        fun <R> map(samCallback: FunctionFlowTransformSAMCallbackChangeMapType<T, R>) =
            FlowJsImpl(flow.map { samCallback.call(it) })

        fun filter(samCallback: FunctionFlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(flow.filter { samCallback.call(it) })

        fun take(count: Int) = FlowJsImpl(flow.take(count))
        fun takeWhile(samCallback: FunctionFlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(flow.takeWhile { samCallback.call(it) })

        fun buffer(
            capacity: Int,
            onBufferOverflow: kotlinx.coroutines.channels.BufferOverflow
        ) = FlowJsImpl(flow.buffer(capacity, onBufferOverflow))

        fun buffer() = FlowJsImpl(flow.buffer())
        fun flowOn(context: kotlin.coroutines.CoroutineContext) = FlowJsImpl(flow.flowOn(context))
        fun catch(samCallback: FunctionFlowTransformErrorSAMCallback<T>) =
            FlowJsImpl(flow.catch { samCallback.call(FlowCollectorJsImpl(this), it) })

        @JvmBlockingBridge
        suspend fun collect(samCallback: FunctionFlowLastOperationSAMCallback<T>) =
            flow.collect { samCallback.call(it) }
    }

    @JvmField
    val mutex = object {
        fun createMutex() = createMutex(false)
        fun createMutex(boolean: Boolean) = MutexJsImpl(kotlinx.coroutines.sync.Mutex(boolean))
    }

    class MutexJsImpl(private val mutex: kotlinx.coroutines.sync.Mutex) {
        fun isLocked() = mutex.isLocked
        fun holdsLock(objects: Objects) = mutex.holdsLock(objects)

        @JvmBlockingBridge
        suspend fun lock() = mutex.lock(null)

        @JvmBlockingBridge
        suspend fun lock(objects: Objects) = mutex.lock(objects)
        fun tryLock(objects: Objects) = mutex.tryLock(objects)
        fun unlock() = mutex.unlock(null)
        fun unlock(objects: Objects) = mutex.unlock(objects)

        @JvmBlockingBridge
        suspend fun <T> withLock(objects: Objects?, samCallback: FunctionMutexAndSemaphoreSAMCallback<T>): T =
            mutex.withLock(objects) { samCallback.call() }

        @JvmBlockingBridge
        suspend fun <T> withLock(samCallback: FunctionMutexAndSemaphoreSAMCallback<T>): T = withLock(null, samCallback)
    }

    @JvmField
    val semaphore = object {
        fun createSemaphore(permits: Int) = createSemaphore(permits, 0)
        fun createSemaphore(permits: Int, acquiredPermits: Int) =
            SemaphoreJsImpl(kotlinx.coroutines.sync.Semaphore(permits, acquiredPermits))
    }

    class SemaphoreJsImpl(private val semaphore: kotlinx.coroutines.sync.Semaphore) {
        fun availablePermits() = semaphore.availablePermits

        @JvmBlockingBridge
        suspend fun acquire() = semaphore.acquire()
        fun release() = semaphore.release()
        fun tryAcquire() = semaphore.tryAcquire()

        @JvmBlockingBridge
        suspend fun <T> withPermits(samCallback: FunctionMutexAndSemaphoreSAMCallback<T>): T =
            semaphore.withPermit { samCallback.call() }
    }
}

interface FunctionCoroutineScopeSAMCallback {
    fun call(coroutineScope: kotlinx.coroutines.CoroutineScope)
}

interface FunctionCoroutineScopeSAMCallbackWithReturnedValue<T> {
    fun call(coroutineScope: kotlinx.coroutines.CoroutineScope): T
}

interface FunctionContinuationSAMCallback<T> {
    fun call(continuation: kotlin.coroutines.Continuation<T>)
}

interface FunctionChannelOnDeliverElementSAMCallback<T> {
    fun call(value: T)
}

interface FunctionFlowActionSAMCallback<T> {
    fun call(flowCollector: KotlinCoroutineLib.FlowCollectorJsImpl<T>)
}

interface FunctionFlowTransformSAMCallbackChangeMapType<T, R> {
    fun call(value: T): R
}

interface FunctionFlowTransformSAMCallbackChangeJudgeType<T> {
    fun call(value: T): Boolean
}

interface FunctionFlowTransformErrorSAMCallback<T> {
    fun call(flowCollector: KotlinCoroutineLib.FlowCollectorJsImpl<T>, throwable: Throwable)
}

interface FunctionFlowLastOperationSAMCallback<T> {
    fun call(value: T)
}

interface FunctionMutexAndSemaphoreSAMCallback<T> {
    fun call(): T
}
