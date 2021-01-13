@file:Suppress("unused", "DeferredIsResult")

package org.itxtech.miraijs.plugin.libs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import net.mamoe.kjbb.JvmBlockingBridge
import org.itxtech.miraijs.plugin.PluginLib

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
    ): kotlinx.coroutines.Deferred<T> = coroutineScope.async(coroutineScope.coroutineContext) { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun <T> await(deferred: kotlinx.coroutines.Deferred<T>): T = deferred.await()

    @JvmBlockingBridge
    suspend fun <T> withContext(
        coroutineContext: kotlin.coroutines.CoroutineContext,
        samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>
    ) = kotlinx.coroutines.withContext(coroutineContext) { samCallback.call(this) }

    @JvmBlockingBridge
    suspend fun <T> withTimeout(timeMills: Long, samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>) =
        kotlinx.coroutines.withTimeout(timeMills) { samCallback.call(this) }

    @JvmField
    val channel = ChannelField

    object ChannelField {
        @JvmField
        val BufferOverflow = BufferOverflowField

        object BufferOverflowField {
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