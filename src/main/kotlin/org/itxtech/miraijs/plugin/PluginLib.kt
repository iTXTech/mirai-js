package org.itxtech.miraijs.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

abstract class PluginLib {
    abstract val nameInJs: String
}

fun <L : PluginLib> Scriptable.insertLib(lib: L) {
    ScriptableObject.putProperty(this, lib.nameInJs, Context.javaToJS(lib, this))
}