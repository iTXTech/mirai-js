@file:Suppress("unused", "DeferredIsResult")

package org.itxtech.miraijs.plugin.libs

import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import net.mamoe.kjbb.JvmBlockingBridge
import org.itxtech.miraijs.plugin.PluginLib
import java.util.*
import kotlin.coroutines.resume

@Suppress("FunctionName", "ClassName", "MemberVisibilityCanBePrivate")
object KotlinCoroutineLib : PluginLib() {
    override val nameInJs = "coroutine"

    @JvmField
    val Dispatchers = kotlinx.coroutines.Dispatchers

    class CoroutineContextJsImpl(val context: kotlin.coroutines.CoroutineContext) {
        fun plus(other: CoroutineContextJsImpl) = CoroutineContextJsImpl(context.plus(other.context))
        fun isActive() = context.isActive
        fun getJob() = JobJsImpl(context.job)
        fun cancel(cause: String) = context.cancel(CancellationException(cause))
        fun cancelChildren(cause: String) = context.cancelChildren(CancellationException(cause))
        fun ensureActive() = context.ensureActive()
    }

    @JvmField
    val continuation = object {
        fun <T> create(samCallback: FunctionInterfaces.ContinuationResumeWithSAMCallback<T>): ContinuationJsImpl<T> =
            ContinuationJsImpl(kotlin.coroutines.Continuation(kotlin.coroutines.EmptyCoroutineContext) {
                samCallback.call(it)
            })
    }

    class ContinuationJsImpl<T>(val continuation: kotlin.coroutines.Continuation<T>) {
        fun resumeWithSuccess(value: T) = continuation.resumeWith(Result.success(value))
        fun resumeWithFailure(reason: String) = continuation.resumeWith(Result.failure(Throwable(reason)))
        fun resume(value: T) = continuation.resume(value)
        fun getContext() = CoroutineContextJsImpl(continuation.context)
    }

    fun createSupervisorJob(parent: kotlinx.coroutines.Job?) =
        SupervisorJobJsImpl(kotlinx.coroutines.SupervisorJob(parent))

    fun createSupervisorJob() = createSupervisorJob(null)
    class SupervisorJobJsImpl(val supervisorJob: kotlinx.coroutines.CompletableJob) {
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

    class JobJsImpl(val job: kotlinx.coroutines.Job) {
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
    suspend fun coroutineScope(samCallback: FunctionInterfaces.CoroutineScopeSAMCallback) =
        kotlinx.coroutines.coroutineScope { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun supervisorScope(samCallback: FunctionInterfaces.CoroutineScopeSAMCallback) =
        kotlinx.coroutines.supervisorScope { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun <T> suspendCoroutine(samCallback: FunctionInterfaces.ContinuationSAMCallback<T>): T =
        kotlin.coroutines.suspendCoroutine { samCallback.call(ContinuationJsImpl(it)) }

    @JvmBlockingBridge
    suspend fun delay(timeMills: Long) = kotlinx.coroutines.delay(timeMills)

    @JvmBlockingBridge
    suspend fun yield() = kotlinx.coroutines.yield()
    fun launchFromGlobalScope(samCallback: FunctionInterfaces.CoroutineScopeSAMCallback) =
        kotlinx.coroutines.GlobalScope.launch { samCallback.call(this) }

    fun launch(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        samCallback: FunctionInterfaces.CoroutineScopeSAMCallback
    ) =
        coroutineScope.launch(coroutineScope.coroutineContext) { samCallback.call(this) }

    fun <T> asyncFromGlobalScope(
        samCallback: FunctionInterfaces.CoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = DeferredJsImpl(kotlinx.coroutines.GlobalScope.async { samCallback.call(this) })


    fun <T> async(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        samCallback: FunctionInterfaces.CoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = DeferredJsImpl(coroutineScope.async(coroutineScope.coroutineContext) { samCallback.call(this) })

    class DeferredJsImpl<T>(private val deferred: kotlinx.coroutines.Deferred<T>) {
        @JvmBlockingBridge
        suspend fun await(): T = deferred.await()

        @kotlinx.coroutines.ExperimentalCoroutinesApi
        fun getCompleted() = deferred.getCompleted()
    }


    @JvmBlockingBridge
    suspend fun <T> withContext(
        coroutineContext: kotlin.coroutines.CoroutineContext,
        samCallback: FunctionInterfaces.CoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = kotlinx.coroutines.withContext(coroutineContext) { samCallback.call(this) }

    /* kotlin.coroutines.withTimeout doesn't work for Rhino JavaScript caller. */
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    @JvmBlockingBridge
    suspend fun <T> withTimeout(
        timeMills: Long,
        samCallback: FunctionInterfaces.CoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = kotlinx.coroutines.coroutineScope functionReturn@{
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

        fun <T> createChannel(samCallback: FunctionInterfaces.ChannelOnDeliverElementSAMCallback<T>): ChannelJsImpl<T> =
            createChannel(ChannelFactory.RENDEZVOUS, BufferOverflow.SUSPEND, samCallback)

        fun <T> createChannel(
            capacity: Int = ChannelFactory.RENDEZVOUS,
            onBufferOverflow: kotlinx.coroutines.channels.BufferOverflow,
            samCallback: FunctionInterfaces.ChannelOnDeliverElementSAMCallback<T>?
        ): ChannelJsImpl<T> =
            ChannelJsImpl(
                kotlinx.coroutines.channels.Channel(
                    capacity,
                    onBufferOverflow,
                    if (samCallback == null) null else ({ samCallback.call(it) })
                )
            )
    }

    class ChannelJsImpl<T>(val channel: kotlinx.coroutines.channels.Channel<T>) {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        fun isClosedForReceive() = channel.isClosedForReceive

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        fun isClosedForSend() = channel.isClosedForSend

        @JvmBlockingBridge
        suspend fun receive(samCallback: FunctionInterfaces.ChannelOnDeliverElementSAMCallback<T>) {
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
        fun <T> createFlow(samCallback: FunctionInterfaces.FlowActionSAMCallback<T>): FlowJsImpl<T> =
            FlowJsImpl(flow { samCallback.call(FlowCollectorJsImpl(this)) })

        fun <T> flowOf(elements: Array<T>) = FlowJsImpl(flow { repeat(elements.count()) { emit(elements[it]) } })
    }

    class FlowCollectorJsImpl<T>(val flowCollector: FlowCollector<T>) {
        @JvmBlockingBridge
        suspend fun emit(value: T) = flowCollector.emit(value)
    }

    class FlowJsImpl<T>(private val flow: Flow<T>) {
        fun onEach(samCallback: FunctionInterfaces.FlowUpstreamOnEachSAMCallback<T>) =
            FlowJsImpl(flow.onEach { samCallback.call(it) })

        /* Every transform operation returns a new FlowJsImpl,
        * because internal transform creates a new Flow */
        fun <R> map(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeMapType<T, R>) =
            FlowJsImpl(flow.map { samCallback.call(it) })

        fun filter(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(flow.filter { samCallback.call(it) })

        fun filterNot(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(flow.filterNot { samCallback.call(it) })

        fun take(count: Int) = FlowJsImpl(flow.take(count))
        fun takeWhile(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(flow.takeWhile { samCallback.call(it) })

        fun drop(count: Int) = FlowJsImpl(flow.drop(count))
        fun dropWhile(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(flow.dropWhile { samCallback.call(it) })

        fun buffer(
            capacity: Int,
            onBufferOverflow: kotlinx.coroutines.channels.BufferOverflow
        ) = FlowJsImpl(flow.buffer(capacity, onBufferOverflow))

        fun buffer() = FlowJsImpl(flow.buffer())
        fun conflate() = FlowJsImpl(flow.conflate())

        fun flowOn(context: CoroutineContextJsImpl) = FlowJsImpl(flow.flowOn(context.context))
        fun launchIn(scope: kotlinx.coroutines.CoroutineScope) = JobJsImpl(flow.launchIn(scope))
        fun catch(samCallback: FunctionInterfaces.FlowTransformErrorSAMCallback<T>) =
            FlowJsImpl(flow.catch { samCallback.call(FlowCollectorJsImpl(this), it) })

        @kotlinx.coroutines.FlowPreview
        fun debounce(timeoutMills: Long) = FlowJsImpl(flow.debounce(timeoutMills))

        @kotlinx.coroutines.FlowPreview
        fun debounce(samCallback: FunctionInterfaces.FlowTransformDebounceSAMCallback<T>) =
            FlowJsImpl(flow.debounce { samCallback.call(it) })

        fun cancellable() = FlowJsImpl(flow.cancellable())

        fun <TT, R> combine(
            other: FlowJsImpl<TT>,
            samCallback: FunctionInterfaces.FlowTransformCombineAndZipSAMCallback<T, TT, R>
        ) = FlowJsImpl(flow.combine(other.flow) { a: T, b: TT -> samCallback.call(a, b) })

        fun <TT, R> zip(
            other: FlowJsImpl<TT>,
            samCallback: FunctionInterfaces.FlowTransformCombineAndZipSAMCallback<T, TT, R>
        ) = FlowJsImpl(flow.zip(other.flow) { a: T, b: TT -> samCallback.call(a, b) })

        @JvmBlockingBridge
        suspend fun collect(samCallback: FunctionInterfaces.FlowLastOperationSAMCallback<T>) =
            flow.collect { samCallback.call(it) }

        @JvmBlockingBridge
        suspend fun collectIndexed(samCallback: FunctionInterfaces.FlowLastOperationSAMCallbackIndexed<T>) =
            flow.collectIndexed { index, value -> samCallback.call(index, value) }

        @JvmBlockingBridge
        suspend fun first() = flow.first()

        @JvmBlockingBridge
        suspend fun first(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeJudgeType<T>) =
            flow.first { samCallback.call(it) }

        @JvmBlockingBridge
        suspend fun count() = flow.count()

        @JvmBlockingBridge
        suspend fun count(samCallback: FunctionInterfaces.FlowTransformSAMCallbackChangeJudgeType<T>) =
            flow.count { samCallback.call(it) }
    }

    @JvmField
    val mutex = object {
        fun createMutex() = createMutex(false)
        fun createMutex(boolean: Boolean) = MutexJsImpl(kotlinx.coroutines.sync.Mutex(boolean))
    }

    class MutexJsImpl(val mutex: kotlinx.coroutines.sync.Mutex) {
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
        suspend fun <T> withLock(
            objects: Objects?,
            samCallback: FunctionInterfaces.MutexAndSemaphoreSAMCallback<T>
        ): T =
            mutex.withLock(objects) { samCallback.call() }

        @JvmBlockingBridge
        suspend fun <T> withLock(samCallback: FunctionInterfaces.MutexAndSemaphoreSAMCallback<T>): T =
            withLock(null, samCallback)
    }

    @JvmField
    val semaphore = object {
        fun createSemaphore(permits: Int) = createSemaphore(permits, 0)
        fun createSemaphore(permits: Int, acquiredPermits: Int) =
            SemaphoreJsImpl(kotlinx.coroutines.sync.Semaphore(permits, acquiredPermits))
    }

    class SemaphoreJsImpl(val semaphore: kotlinx.coroutines.sync.Semaphore) {
        fun availablePermits() = semaphore.availablePermits

        @JvmBlockingBridge
        suspend fun acquire() = semaphore.acquire()
        fun release() = semaphore.release()
        fun tryAcquire() = semaphore.tryAcquire()

        @JvmBlockingBridge
        suspend fun <T> withPermits(samCallback: FunctionInterfaces.MutexAndSemaphoreSAMCallback<T>): T =
            semaphore.withPermit { samCallback.call() }
    }
}

class FunctionInterfaces {
    interface CoroutineScopeSAMCallback {
        fun call(coroutineScope: kotlinx.coroutines.CoroutineScope)
    }


    interface CoroutineScopeSAMCallbackWithReturnedValue<T> {
        fun call(coroutineScope: kotlinx.coroutines.CoroutineScope): T
    }


    interface ContinuationResumeWithSAMCallback<T> {
        fun call(result: Result<T>)
    }

    interface ContinuationSAMCallback<T> {
        fun call(continuation: KotlinCoroutineLib.ContinuationJsImpl<T>)
    }

    interface ChannelOnDeliverElementSAMCallback<T> {
        fun call(value: T)
    }

    interface FlowActionSAMCallback<T> {
        fun call(flowCollector: KotlinCoroutineLib.FlowCollectorJsImpl<T>)
    }

    interface FlowUpstreamOnEachSAMCallback<T> {
        fun call(value: T)
    }

    interface FlowTransformSAMCallbackChangeMapType<T, R> {
        fun call(value: T): R
    }

    interface FlowTransformSAMCallbackChangeJudgeType<T> {
        fun call(value: T): Boolean
    }

    interface FlowTransformDebounceSAMCallback<T> {
        fun call(value: T): Long
    }

    interface FlowTransformErrorSAMCallback<T> {
        fun call(flowCollector: KotlinCoroutineLib.FlowCollectorJsImpl<T>, throwable: Throwable)
    }

    interface FlowTransformCombineAndZipSAMCallback<T, TT, R> {
        fun call(value1: T, value2: TT): R
    }

    interface FlowLastOperationSAMCallback<T> {
        fun call(value: T)
    }

    interface FlowLastOperationSAMCallbackIndexed<T> {
        fun call(index: Int, value: T)
    }

    interface MutexAndSemaphoreSAMCallback<T> {
        fun call(): T
    }
}
