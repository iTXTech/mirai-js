@file:Suppress("unused", "DeferredIsResult")

package org.itxtech.miraijs.plugin.libs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.kjbb.JvmBlockingBridge
import org.itxtech.miraijs.plugin.PluginLib
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

object KotlinCoroutineLib : PluginLib() {
    override val nameInJs: String = "coroutine"

    val dispatchers = kotlinx.coroutines.Dispatchers

    @JvmBlockingBridge
    suspend fun coroutineScope(
        samCallback: FunctionCoroutineScopeSAMCallback
    ) = kotlinx.coroutines.coroutineScope {
        samCallback.call(this)
    }

    @JvmBlockingBridge
    suspend fun <T> suspendCoroutine(
        samCallback: FunctionContinuationSAMCallback<T>
    ): T = kotlin.coroutines.suspendCoroutine {
        samCallback.call(it)
    }

    @JvmBlockingBridge
    suspend fun delay(timeMills: Long) {
        kotlinx.coroutines.delay(timeMills)
    }

    fun launchFromGlobalScope(
        samCallback: FunctionCoroutineScopeSAMCallback
    ) = kotlinx.coroutines.GlobalScope.launch {
        samCallback.call(this)
    }

    fun launch(
        coroutineScope: CoroutineScope,
        samCallback: FunctionCoroutineScopeSAMCallback
    ) = coroutineScope.launch(coroutineScope.coroutineContext) {
        samCallback.call(this)
    }

    fun <T> asyncFromGlobalScope(
        samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>
    ): kotlinx.coroutines.Deferred<T> = kotlinx.coroutines.GlobalScope.async {
        samCallback.call(this)
    }

    fun <T> async(
        coroutineScope: CoroutineScope,
        samCallback: FunctionCoroutineScopeSAMCallbackWithReturnedValue<T>
    ): kotlinx.coroutines.Deferred<T> = coroutineScope.async(coroutineScope.coroutineContext) {
        samCallback.call(this)
    }

    @JvmBlockingBridge
    suspend fun <T> await(
        deferred: kotlinx.coroutines.Deferred<T>
    ): T = deferred.await()

    @JvmBlockingBridge
    suspend fun withContext(
        coroutineContext: CoroutineContext,
        samCallback: FunctionCoroutineScopeSAMCallback
    ) = withContext(coroutineContext) {
        samCallback.call(this)
    }

}

interface FunctionCoroutineScopeSAMCallback {
    fun call(coroutineScope: CoroutineScope)
}

interface FunctionCoroutineScopeSAMCallbackWithReturnedValue<T> {
    fun call(coroutineScope: CoroutineScope): T
}

interface FunctionContinuationSAMCallback<T> {
    fun call(continuation: Continuation<T>)
}