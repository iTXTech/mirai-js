package org.itxtech.miraijs

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import org.itxtech.miraijs.libs.JSCommand
import org.itxtech.miraijs.libs.PrimitivePluginData
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Script
import java.io.File
import java.io.Reader
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
class PluginScope(val name: String) : CoroutineScope {

    //Parent of all jobs created by plugin
    lateinit var pluginParentJob: CompletableJob
    //Plugin processing(loading, unloading, etc...)
    override val coroutineContext: CoroutineContext
        get() = MiraiJs.coroutineContext + pluginParentJob + runtimeExceptionHandler

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext(name)

    private lateinit var ctx: Context
    private lateinit var scope: ImporterTopLevel
    private val scripts: HashMap<String, Script> = hashMapOf()

    val dataFolder = File(PluginManager.pluginData.absolutePath + File.separatorChar + name)
    //represent if unload() is called by jpm
    var isUnloadFlag: Boolean = false
    //represent if execute is called
    private var isExecuted: Boolean = false

    //catch exceptions while loading plugins
    var loadingExceptionListener: ((CoroutineContext, Throwable) -> Unit)? = null
    private val loadingExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        launch { unload() }
        loadingExceptionListener?.invoke(coroutineContext, throwable)
    }
    //catch exceptions caused by plugin in runtime
    var runtimeExceptionListener: ((CoroutineContext, Throwable) -> Unit)? = null
    private val runtimeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        runtimeExceptionListener?.invoke(coroutineContext, throwable)
    }

    //library scope
    val registeredCommands = mutableListOf<JSCommand>()
    val data = PrimitivePluginData(name)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun init() = withContext(dispatcher + loadingExceptionHandler) { //set propriety
        ctx = Context.enter()
        ctx.optimizationLevel = PluginManager.optimizationLevel
        ctx.languageVersion = Context.VERSION_ES6
        scope = ImporterTopLevel(ctx)
        knownPluginLibrary.forEach {
            it.constructors.first().call(this@PluginScope).importTo(scope, ctx)
        }
        MiraiJs.withConsolePluginContext { data.reload() }
        pluginParentJob = SupervisorJob()
        isUnloadFlag = false
        isExecuted = false
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun attachScript(srcName: String, reader: Reader) : Unit
            = withContext(dispatcher + loadingExceptionHandler) {
        ctx.compileReader(reader, "${name}#$srcName", 1, null).also {
            scripts[srcName] = it
            if (isExecuted) it.exec(ctx, scope)
        }
    }

    fun execute() = launch(dispatcher + loadingExceptionHandler) {
        scripts.forEach { it.value.exec(ctx, scope) }
        isExecuted = true
    }

    suspend fun unload() {
        isUnloadFlag = true
        withContext(dispatcher) {
            Context.exit()
            registeredCommands.forEach { it.unregister() }
            registeredCommands.clear()
        }
        scripts.clear()
        cancelPluginAndItsChildrenJobAndJoin()
    }

    private suspend fun cancelPluginAndItsChildrenJobAndJoin() {
        pluginParentJob.runCatching {
            cancelChildren()
            cancelAndJoin()
        }
    }

    private fun cancelPluginAndItsChildrenJob() {
        pluginParentJob.runCatching {
            cancelChildren()
            cancel()
        }
    }

    override fun toString() = "$name(${super.toString()})"
}