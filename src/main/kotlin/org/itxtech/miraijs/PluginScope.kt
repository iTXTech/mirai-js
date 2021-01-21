package org.itxtech.miraijs

import kotlinx.coroutines.*
import org.itxtech.miraijs.`package`.PluginConfigInfo
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Script
import java.io.InputStreamReader
import kotlin.coroutines.CoroutineContext

class PluginScope(private val pluginInfo: PluginConfigInfo) : CoroutineScope {

    private val pluginJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = pluginJob + MiraiJs.coroutineContext + dispatcher

    @OptIn(ObsoleteCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext(pluginInfo.name)

    private lateinit var ctx: Context
    private lateinit var scope: ImporterTopLevel
    private val scripts: HashMap<String, Script> = hashMapOf()

    suspend fun init() = withContext(dispatcher) { //set propriety
        ctx = Context.enter()
        ctx.optimizationLevel = PluginManager.optimizationLevel
        ctx.languageVersion = Context.VERSION_ES6
        //init top level function
        scope = ImporterTopLevel()
        scope.initStandardObjects(ctx, false)
        //init known libraries
        knownPluginLibrary.forEach {
            (it.constructors[0].newInstance(this@PluginScope) as PluginLib).importTo(scope, ctx)
        }
    }

    suspend fun attachScript(scriptName: String, reader: InputStreamReader) = withContext(dispatcher) {
        scripts[scriptName] =
            ctx.compileReader(reader, "${pluginInfo.name}#$scriptName", 1, null)
    }

    fun execute() = launch(dispatcher) {
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
}