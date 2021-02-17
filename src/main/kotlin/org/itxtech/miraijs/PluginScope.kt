package org.itxtech.miraijs

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import org.itxtech.miraijs.libs.JSCommand
import org.itxtech.miraijs.libs.PrimitivePluginData
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import java.io.File
import java.io.Reader
import java.io.Serializable
import java.lang.reflect.Member
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
class PluginScope(val name: String) : CoroutineScope, ScriptableObject() {

    //Parent of all jobs created by plugin
    lateinit var pluginParentJob: CompletableJob
    //Plugin processing(loading, unloading, etc...)
    override val coroutineContext: CoroutineContext
        get() = MiraiJs.coroutineContext + pluginParentJob + runtimeExceptionHandler

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext(name)

    private lateinit var ctx: Context
    private lateinit var topLevelScope: ImporterTopLevel
    private val moduleExports: HashMap<String, Any> = hashMapOf()
    private val scripts: HashMap<String, Script> = hashMapOf()

    val dataFolder = File(PluginManager.pluginData.absolutePath + File.separatorChar + name)
    //represent if unload() is called by jpm
    var isUnloadFlag: Boolean = false

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
    private val libInstances: HashSet<PluginLib> by lazy {
        knownPluginLibrary.map {
            it.constructors.first().call(this@PluginScope)
        }.toHashSet()
    }
    val registeredCommands = mutableListOf<JSCommand>()
    val data = PrimitivePluginData(name)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun init() = withContext(dispatcher + loadingExceptionHandler) { //set propriety
        ctx = Context.enter()
        ctx.optimizationLevel = PluginManager.optimizationLevel
        ctx.languageVersion = Context.VERSION_ES6
        topLevelScope = generateModuleScope()
        MiraiJs.withConsolePluginContext { data.reload() }
        pluginParentJob = SupervisorJob()
        isUnloadFlag = false
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun attachScript(srcName: String, reader: Reader) : Unit
            = withContext(dispatcher + loadingExceptionHandler) {
        ctx.compileReader(reader, "${name}#$srcName", 1, null).also {
            scripts[srcName] = it
        }
    }

    fun execute() = launch(dispatcher + loadingExceptionHandler) {
        scripts["main"]!!.exec(ctx, topLevelScope)
    }

    fun generateModuleScope() : ImporterTopLevel {
        val scriptable = ImporterTopLevel(ctx)
        parentScope = scriptable
        val importModuleInstance = javaClass.getMethod("js_importModule", String::class.java)
        scriptable.put("module", scriptable,
            ModuleFunctionObject("module", importModuleInstance, this))
        libInstances.forEach { it.importTo(scriptable, ctx) }
        return scriptable
    }

    @Suppress("unused")
    fun js_importModule(moduleName: String) : Any {
        if(moduleExports.containsKey(moduleName)) {
            return moduleExports[moduleName]!!
        } else if(scripts.containsKey(moduleName)) {
            val moduleScope = generateModuleScope()
            scripts[moduleName]!!.exec(ctx, moduleScope)
            return ((moduleScope["module"] as ScriptableObject)["exports"] ?: Undefined.instance).also {
                moduleExports.put(moduleName, it)
            }
        } else throw NoSuchFileException(File(moduleName))
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

    override fun getClassName(): String = javaClass.name

    override fun toString() = "$name(${super.toString()})"

    private class ModuleFunctionObject(
        name: String, methodOrConstructor: Member, parentScope: Scriptable
    ) : FunctionObject(name, methodOrConstructor, parentScope) {
        override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
            return super.call(cx, scope, parentScope, args)
        }
    }
}