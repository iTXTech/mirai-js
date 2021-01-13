package org.itxtech.miraijs.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

abstract class PluginLib {
    abstract val nameInJs: String
}


@Suppress("NOTHING_TO_INLINE")
inline fun <T : PluginLib> Scriptable.importLib(lib: T) {
    ScriptableObject.putProperty(this, lib.nameInJs, Context.javaToJS(lib, this))
}