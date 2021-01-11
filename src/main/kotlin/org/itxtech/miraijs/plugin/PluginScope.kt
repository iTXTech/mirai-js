package org.itxtech.miraijs.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newSingleThreadContext
import org.itxtech.miraijs.plugin.libs.KotlinCoroutineLib
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Script
import kotlin.coroutines.CoroutineContext

class PluginScope(val src: String) : CoroutineScope {

    private val pluginJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = pluginJob

    @OptIn(ObsoleteCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext("TestJsPlugin")

    private lateinit var ctx: Context
    private lateinit var scope: ImporterTopLevel
    private lateinit var script: Script

    init {

    }

    fun load() {
        //set propriety
        ctx = Context.enter()
        ctx.optimizationLevel = -1
        ctx.languageVersion = Context.VERSION_ES6
        //init top level function
        scope = ImporterTopLevel()
        scope.initStandardObjects(ctx, false)
        //init internal object
        scope.insertLib(KotlinCoroutineLib)
        //load script file
        script = ctx.compileString(src, "TestJsPlugin", 1, null)
        script.exec(ctx, scope)
    }
}