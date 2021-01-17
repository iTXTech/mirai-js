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

    class CoroutineContextJsImpl(@JvmField val self: kotlin.coroutines.CoroutineContext) {
        fun plus(other: CoroutineContextJsImpl) = CoroutineContextJsImpl(self.plus(other.self))
        fun isActive() = self.isActive
        fun getJob() = JobJsImpl(self.job)
        fun cancel(cause: String) = self.cancel(CancellationException(cause))
        fun cancelChildren(cause: String) = self.cancelChildren(CancellationException(cause))
        fun ensureActive() = self.ensureActive()
    }

    @JvmField
    val continuation = object {
        fun <T> create(samCallback: KtCoroutineLambdaInterface.ContinuationResumeWithSAMCallback<T>): ContinuationJsImpl<T> =
            ContinuationJsImpl(kotlin.coroutines.Continuation(kotlin.coroutines.EmptyCoroutineContext) {
                samCallback.call(it)
            })
    }

    class ContinuationJsImpl<T>(@JvmField val self: kotlin.coroutines.Continuation<T>) {
        fun resumeWithSuccess(value: T) = self.resumeWith(Result.success(value))
        fun resumeWithFailure(reason: String) = self.resumeWith(Result.failure(Throwable(reason)))
        fun resume(value: T) = self.resume(value)
        fun getContext() = CoroutineContextJsImpl(self.context)
    }

    fun createSupervisorJob(parent: kotlinx.coroutines.Job?) =
        SupervisorJobJsImpl(kotlinx.coroutines.SupervisorJob(parent))

    fun createSupervisorJob() = createSupervisorJob(null)
    class SupervisorJobJsImpl(@JvmField val self: kotlinx.coroutines.CompletableJob) {
        @JvmBlockingBridge
        suspend fun join() = self.join()
        fun cancel(reason: String) = self.cancel(cause = kotlinx.coroutines.CancellationException(reason))

        @JvmBlockingBridge
        suspend fun cancelAndJoin() = self.cancelAndJoin()
        fun isActive() = self.isActive
        fun isCanceled() = self.isCancelled
        fun isCompleted() = self.isCompleted
        fun complete() = self.complete()
        fun completeExceptionally(reason: String) = self.completeExceptionally(Throwable(reason))
    }

    fun createJob(parent: kotlinx.coroutines.Job?) = JobJsImpl(kotlinx.coroutines.Job(parent))

    class JobJsImpl(@JvmField val self: kotlinx.coroutines.Job) {
        @JvmBlockingBridge
        suspend fun join() = self.join()
        fun cancel(reason: String) = self.cancel(cause = kotlinx.coroutines.CancellationException(reason))

        @JvmBlockingBridge
        suspend fun cancelAndJoin() = self.cancelAndJoin()
        fun isActive() = self.isActive
        fun isCanceled() = self.isCancelled
        fun isCompleted() = self.isCompleted
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
    suspend fun coroutineScope(samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallback) =
        kotlinx.coroutines.coroutineScope { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun supervisorScope(samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallback) =
        kotlinx.coroutines.supervisorScope { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun <T> suspendCoroutine(samCallback: KtCoroutineLambdaInterface.ContinuationSAMCallback<T>): T =
        kotlin.coroutines.suspendCoroutine { samCallback.call(ContinuationJsImpl(it)) }

    @JvmBlockingBridge
    suspend fun delay(timeMills: Long) = kotlinx.coroutines.delay(timeMills)

    @JvmBlockingBridge
    suspend fun yield() = kotlinx.coroutines.yield()
    fun launchFromGlobalScope(samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallback) =
        kotlinx.coroutines.GlobalScope.launch { samCallback.call(this) }

    fun launch(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallback
    ) =
        coroutineScope.launch(coroutineScope.coroutineContext) { samCallback.call(this) }

    fun <T> asyncFromGlobalScope(
        samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = DeferredJsImpl(kotlinx.coroutines.GlobalScope.async { samCallback.call(this) })


    fun <T> async(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallbackWithReturnedValue<T>
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
        samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = kotlinx.coroutines.withContext(coroutineContext) { samCallback.call(this) }

    /* kotlin.coroutines.withTimeout doesn't work for Rhino JavaScript caller. */
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    @JvmBlockingBridge
    suspend fun <T> withTimeout(
        timeMills: Long,
        samCallback: KtCoroutineLambdaInterface.CoroutineScopeSAMCallbackWithReturnedValue<T>
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

        fun <T> createChannel(samCallback: KtCoroutineLambdaInterface.ChannelOnDeliverElementSAMCallback<T>): ChannelJsImpl<T> =
            createChannel(ChannelFactory.RENDEZVOUS, BufferOverflow.SUSPEND, samCallback)

        fun <T> createChannel(
            capacity: Int = ChannelFactory.RENDEZVOUS,
            onBufferOverflow: kotlinx.coroutines.channels.BufferOverflow,
            samCallback: KtCoroutineLambdaInterface.ChannelOnDeliverElementSAMCallback<T>?
        ): ChannelJsImpl<T> =
            ChannelJsImpl(
                kotlinx.coroutines.channels.Channel(
                    capacity,
                    onBufferOverflow,
                    if (samCallback == null) null else ({ samCallback.call(it) })
                )
            )
    }

    class ChannelJsImpl<T>(@JvmField val self: kotlinx.coroutines.channels.Channel<T>) {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        fun isClosedForReceive() = self.isClosedForReceive

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        fun isClosedForSend() = self.isClosedForSend

        @JvmBlockingBridge
        suspend fun receive(samCallback: KtCoroutineLambdaInterface.ChannelOnDeliverElementSAMCallback<T>) {
            while (!isClosedForReceive()) {
                try {
                    samCallback.call(self.receive())
                } catch (ex: Exception) {
                    return
                }
            }
        }

        @JvmBlockingBridge
        suspend fun send(value: T) = self.send(value)
        fun close() = self.close()
    }

    @JvmField
    val flow = object {
        fun <T> createFlow(samCallback: KtCoroutineLambdaInterface.FlowActionSAMCallback<T>): FlowJsImpl<T> =
            FlowJsImpl(flow { samCallback.call(FlowCollectorJsImpl(this)) })

        fun <T> flowOf(elements: Array<T>) = FlowJsImpl(flow { repeat(elements.count()) { emit(elements[it]) } })
    }

    class FlowCollectorJsImpl<T>(val flowCollector: FlowCollector<T>) {
        @JvmBlockingBridge
        suspend fun emit(value: T) = flowCollector.emit(value)
    }

    class FlowJsImpl<T>(@JvmField val self: Flow<T>) {
        fun onEach(samCallback: KtCoroutineLambdaInterface.FlowUpstreamOnEachSAMCallback<T>) =
            FlowJsImpl(self.onEach { samCallback.call(it) })

        /* Every transform operation returns a new FlowJsImpl,
        * because internal transform creates a new Flow */
        fun <R> map(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeMapType<T, R>) =
            FlowJsImpl(self.map { samCallback.call(it) })

        fun filter(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(self.filter { samCallback.call(it) })

        fun filterNot(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(self.filterNot { samCallback.call(it) })

        fun take(count: Int) = FlowJsImpl(self.take(count))
        fun takeWhile(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(self.takeWhile { samCallback.call(it) })

        fun drop(count: Int) = FlowJsImpl(self.drop(count))
        fun dropWhile(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeJudgeType<T>) =
            FlowJsImpl(self.dropWhile { samCallback.call(it) })

        fun buffer(
            capacity: Int,
            onBufferOverflow: kotlinx.coroutines.channels.BufferOverflow
        ) = FlowJsImpl(self.buffer(capacity, onBufferOverflow))

        fun buffer() = FlowJsImpl(self.buffer())
        fun conflate() = FlowJsImpl(self.conflate())

        fun flowOn(context: CoroutineContextJsImpl) = FlowJsImpl(self.flowOn(context.self))
        fun launchIn(scope: kotlinx.coroutines.CoroutineScope) = JobJsImpl(self.launchIn(scope))
        fun catch(samCallback: KtCoroutineLambdaInterface.FlowTransformErrorSAMCallback<T>) =
            FlowJsImpl(self.catch { samCallback.call(FlowCollectorJsImpl(this), it) })

        @kotlinx.coroutines.FlowPreview
        fun debounce(timeoutMills: Long) = FlowJsImpl(self.debounce(timeoutMills))

        @kotlinx.coroutines.FlowPreview
        fun debounce(samCallback: KtCoroutineLambdaInterface.FlowTransformDebounceSAMCallback<T>) =
            FlowJsImpl(self.debounce { samCallback.call(it) })

        fun cancellable() = FlowJsImpl(self.cancellable())

        fun <TT, R> combine(
            other: FlowJsImpl<TT>,
            samCallback: KtCoroutineLambdaInterface.FlowTransformCombineAndZipSAMCallback<T, TT, R>
        ) = FlowJsImpl(self.combine(other.self) { a: T, b: TT -> samCallback.call(a, b) })

        fun <TT, R> zip(
            other: FlowJsImpl<TT>,
            samCallback: KtCoroutineLambdaInterface.FlowTransformCombineAndZipSAMCallback<T, TT, R>
        ) = FlowJsImpl(self.zip(other.self) { a: T, b: TT -> samCallback.call(a, b) })

        @JvmBlockingBridge
        suspend fun collect(samCallback: KtCoroutineLambdaInterface.FlowLastOperationSAMCallback<T>) =
            self.collect { samCallback.call(it) }

        @JvmBlockingBridge
        suspend fun collectIndexed(samCallback: KtCoroutineLambdaInterface.FlowLastOperationSAMCallbackIndexed<T>) =
            self.collectIndexed { index, value -> samCallback.call(index, value) }

        @JvmBlockingBridge
        suspend fun first() = self.first()

        @JvmBlockingBridge
        suspend fun first(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeJudgeType<T>) =
            self.first { samCallback.call(it) }

        @JvmBlockingBridge
        suspend fun count() = self.count()

        @JvmBlockingBridge
        suspend fun count(samCallback: KtCoroutineLambdaInterface.FlowTransformSAMCallbackChangeJudgeType<T>) =
            self.count { samCallback.call(it) }
    }

    @JvmField
    val mutex = object {
        fun createMutex() = createMutex(false)
        fun createMutex(boolean: Boolean) = MutexJsImpl(kotlinx.coroutines.sync.Mutex(boolean))
    }

    class MutexJsImpl(@JvmField val self: kotlinx.coroutines.sync.Mutex) {
        fun isLocked() = self.isLocked
        fun holdsLock(objects: Objects) = self.holdsLock(objects)

        @JvmBlockingBridge
        suspend fun lock() = self.lock(null)

        @JvmBlockingBridge
        suspend fun lock(objects: Objects) = self.lock(objects)
        fun tryLock(objects: Objects) = self.tryLock(objects)
        fun unlock() = self.unlock(null)
        fun unlock(objects: Objects) = self.unlock(objects)

        @JvmBlockingBridge
        suspend fun <T> withLock(
            objects: Objects?,
            samCallback: KtCoroutineLambdaInterface.MutexAndSemaphoreSAMCallback<T>
        ): T =
            self.withLock(objects) { samCallback.call() }

        @JvmBlockingBridge
        suspend fun <T> withLock(samCallback: KtCoroutineLambdaInterface.MutexAndSemaphoreSAMCallback<T>): T =
            withLock(null, samCallback)
    }

    @JvmField
    val semaphore = object {
        fun createSemaphore(permits: Int) = createSemaphore(permits, 0)
        fun createSemaphore(permits: Int, acquiredPermits: Int) =
            SemaphoreJsImpl(kotlinx.coroutines.sync.Semaphore(permits, acquiredPermits))
    }

    class SemaphoreJsImpl(@JvmField val self: kotlinx.coroutines.sync.Semaphore) {
        fun availablePermits() = self.availablePermits

        @JvmBlockingBridge
        suspend fun acquire() = self.acquire()
        fun release() = self.release()
        fun tryAcquire() = self.tryAcquire()

        @JvmBlockingBridge
        suspend fun <T> withPermits(samCallback: KtCoroutineLambdaInterface.MutexAndSemaphoreSAMCallback<T>): T =
            self.withPermit { samCallback.call() }
    }

    //wrapper
    fun wrapCoroutineContext(context: kotlin.coroutines.CoroutineContext) = CoroutineContextJsImpl(context)
    fun <T> wrapContinuation(continuation: kotlin.coroutines.Continuation<T>) = ContinuationJsImpl(continuation)
    fun wrapSupervisorJob(job: kotlinx.coroutines.CompletableJob) = SupervisorJobJsImpl(job)
    fun wrapJob(job: kotlinx.coroutines.Job) = JobJsImpl(job)
    fun <T> wrapDeferred(deferred: kotlinx.coroutines.Deferred<T>) = DeferredJsImpl(deferred)
    fun <E> wrapChannel(channel: kotlinx.coroutines.channels.Channel<E>) = ChannelJsImpl(channel)
    fun <T> wrapFlow(flow: Flow<T>) = FlowJsImpl(flow)
    fun <T> wrapFlowCollector(flowCollector: FlowCollector<T>) =
        FlowCollectorJsImpl(flowCollector)
}

class KtCoroutineLambdaInterface {
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
