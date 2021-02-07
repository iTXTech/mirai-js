package org.itxtech.miraijs

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.warning
import org.itxtech.miraijs.`package`.PluginPackage
import org.itxtech.miraijs.libs.JSCommand
import org.itxtech.miraijs.libs.PrimitivePluginData
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Script
import java.io.File
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
class PluginScope(private val pluginPackage: PluginPackage) : CoroutineScope {
    val name = pluginPackage.config!!.name
    val id = pluginPackage.config!!.id
    val author = pluginPackage.config!!.author

    //Parent of all jobs created by plugin
    lateinit var pluginParentJob: CompletableJob
    //Plugin processing(loading, unloading, etc...)
    override val coroutineContext: CoroutineContext
        get() = MiraiJs.coroutineContext + pluginParentJob + runtimeExceptionHandler

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext(name)

    //catch exceptions while loading plugins
    val loadingExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        MiraiJs.logger.error {
            "Error while loading plugin $name($id) by $author.\nDetail: " +
            buildString { throwable.run {
                append(this)
                append("\n")
                append(stackTrace.joinToString("\n") { "\tat $it" })
            } }
        }
        launch { unload() }
    }
    //catch exceptions caused by plugin in runtime
    val runtimeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        MiraiJs.logger.error {
            "Exception in MiraiJs plugin $name($id) by $author.\nDetail: " +
            buildString { throwable.run {
                append(this)
                append("\n")
                append(stackTrace.joinToString("\n") { "\tat $it" })
            } }
        }
    }

    private lateinit var ctx: Context
    private lateinit var scope: ImporterTopLevel
    private val scripts: HashMap<String, Script> = hashMapOf()

    val dataFolder = File(PluginManager.pluginData.absolutePath + File.separatorChar + id)
    //represent if unload() is called by jpm
    var isUnloadFlag: Boolean = false

    /*
     * library scope
     */
    val registeredCommands = mutableListOf<JSCommand>()
    val data = PrimitivePluginData(name)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun init() = withContext(dispatcher + loadingExceptionHandler) { //set propriety
        ctx = Context.enter()
        ctx.optimizationLevel = PluginManager.optimizationLevel
        ctx.languageVersion = Context.VERSION_ES6
        //init top level function
        scope = ImporterTopLevel()
        scope.initStandardObjects(ctx, false)
        //init libraries
        knownPluginLibrary.forEach {
            it.constructors.first().call(this@PluginScope).importTo(scope, ctx)
        }
        MiraiJs.withConsolePluginContext { data.reload() }
        pluginParentJob = SupervisorJob()
        isUnloadFlag = false
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun compileScripts() = withContext(dispatcher + loadingExceptionHandler) {
        pluginPackage.consumeScriptReaders {
            scripts[it] =
                ctx.compileReader(this@consumeScriptReaders, "${name}#$it", 1, null)
        }
    }

    fun load() = launch(dispatcher + loadingExceptionHandler) {
        val order = mutableListOf<Pair<String, Script>>()
        pluginPackage.config!!.order.run {
            filterNot { it == "..." }.forEach {
                if(scripts[it] != null) {
                    order.add(it to scripts[it]!!)
                } else MiraiJs.logger.warning { "Script $it in plugin $name($id) is not found!" }
            }
            when(filter { it == "..." }.count()) {
                0 -> { /* stub */ }
                1 -> {
                    val idx = indexOf("...")
                    scripts.map { it.key }.subtract(order.map { it.first }).toList().asReversed().forEach {
                        if(scripts[it] != null) {
                            order.add(idx, it to scripts[it]!!)
                        } else MiraiJs.logger.warning { "Script $it in plugin $name($id) is not found!" }
                    }
                }
                else -> {
                    MiraiJs.logger.error { """
                        Error occurred while loading plugin $name($id): 
	                    ${'\t'}Count of generic load symbol "..." is larger than 1.
                    """.trimIndent() }
                }
            }
        }
        try {
            order.forEach { it.second.exec(ctx, scope) }
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun unload() {
        isUnloadFlag = true
        withContext(dispatcher) {
            Context.exit()
            registeredCommands.forEach { it.unregister() }
            pluginPackage.closeAndRelease()
        }
        cancelPluginAndItsChildrenJobAndJoin()
    }

    suspend fun reload() {
        isUnloadFlag = true
        withContext(dispatcher) {
            Context.exit()
            registeredCommands.run {
                forEach { it.unregister() }
                clear()
            }
        }
        cancelPluginAndItsChildrenJobAndJoin()
        init()
        compileScripts()
        load()
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

    override fun toString() = "$name($id) by $author"
}