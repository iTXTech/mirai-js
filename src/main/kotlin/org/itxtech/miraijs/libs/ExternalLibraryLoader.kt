package org.itxtech.miraijs.libs

import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.File


class ExternalLibraryLoader(plugin: PluginScope) : PluginLib(plugin) {
    @JvmSynthetic
    override val nameInJs: String = "external"

    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }

}

//Default classloader for jvm
//For Android, users need to implement IClassLoader
/*
object DefaultJVMClassLoader : IClassLoader {

}

interface IClassLoader {
    val clsFile: File

}*/
