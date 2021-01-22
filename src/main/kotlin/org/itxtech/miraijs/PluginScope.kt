package org.itxtech.miraijs

import kotlinx.coroutines.*
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import org.itxtech.miraijs.`package`.PluginPackage
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


    private val pluginJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = pluginJob + MiraiJs.coroutineContext + dispatcher

    @OptIn(ObsoleteCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext(name)

    private lateinit var ctx: Context
    private lateinit var scope: ImporterTopLevel
    private val scripts: HashMap<String, Script> = hashMapOf()

    val dataFolder = File(PluginManager.pluginData.absolutePath + File.separatorChar + id)

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
        dataFolder.mkdirs()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun compileScripts() = withContext(dispatcher) {
        try {
            pluginPackage.consumeScriptReaders {
                scripts[it] = ctx.compileReader(
                    this,
                    "${name}#$it",
                    1,
                    null
                )
            }
        } catch (ex: Exception) {
            unload()
            throw Context.throwAsScriptRuntimeEx(ex)
        }
    }

    fun load() = launch(dispatcher) {
        try {
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
        } catch (ex: Exception) {
            MiraiJs.logger.error("Plugin occurred an error : ${Context.throwAsScriptRuntimeEx(ex)}")
        }
    }

    fun unload() = launch(dispatcher) {
        Context.exit()
        pluginJob.cancelChildren()
        pluginJob.cancelAndJoin()
    }

    fun reload() = launch(dispatcher) {
        Context.exit()
        init()
        compileScripts()
        load()
    }

    override fun toString(): String {
        return "$name($id)"
    }
}