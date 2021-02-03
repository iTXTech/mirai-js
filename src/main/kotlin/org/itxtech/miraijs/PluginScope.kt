package org.itxtech.miraijs

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.utils.error
import org.itxtech.miraijs.`package`.PluginPackage
import org.itxtech.miraijs.libs.JSCommand
import org.itxtech.miraijs.libs.PrimitivePluginData
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Script
import java.io.File
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
class PluginScope(private val pluginPackage: PluginPackage) : CoroutineScope {
    val name = pluginPackage.config!!.name
    val id = pluginPackage.config!!.id
    val author = pluginPackage.config!!.author

    //Parent of all jobs created by plugin
    val pluginParentJob = Job()
    //Plugin processing(loading, unloading, etc...)
    override val coroutineContext: CoroutineContext
        get() = MiraiJs.coroutineContext + pluginParentJob

    @OptIn(ObsoleteCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext(name)
    
    val runtimeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        MiraiJs.logger.error {
            "Exception in MiraiJs plugin $name($id) by $author.\nDetail: " +
            buildString { throwable.run {
                append(this)
                append("\n")
                append(stackTrace.joinToString("\n") { "  at $it" })
            } }
        }
        cancelPluginAndItsChildrenJob()
    }

    private lateinit var ctx: Context
    private lateinit var scope: ImporterTopLevel
    private val scripts: HashMap<String, Script> = hashMapOf()

    val dataFolder = File(PluginManager.pluginData.absolutePath + File.separatorChar + id)

    /*
     * library scope
     */
    val registeredCommands = mutableListOf<JSCommand>()
    val data = PrimitivePluginData(name)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun init() = withContext(dispatcher) { //set propriety
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
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun compileScripts() = withContext(dispatcher + runtimeExceptionHandler) {
        pluginPackage.consumeScriptReaders {
            scripts[it] =
                ctx.compileReader(this@consumeScriptReaders, "${name}#$it", 1, null)
        }
    }

    fun load() = launch(dispatcher + runtimeExceptionHandler) {
        val mainScript = scripts.filterKeys { it == "main" }
        if (mainScript.count() == 0) {
            scripts.values.forEach {
                it.exec(ctx, scope)
            }
        } else {
            mainScript["main"]?.exec(ctx, scope)
            scripts.asSequence().filterNot { it.key == "main" }.forEach {
                it.value.exec(ctx, scope)
            }
        }
    }

    suspend fun unload() = withContext(dispatcher) {
        Context.exit()
        registeredCommands.forEach { it.unregister() }
        pluginPackage.closeAndRelease()
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

    override fun toString() = "MiraiJs plugin $name($id) by $author"
}