package org.itxtech.miraijs.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

abstract class PluginLib {
    abstract val nameInJs: String

    @Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")
    open fun import(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }
}