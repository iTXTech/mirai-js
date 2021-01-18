package org.itxtech.miraijs.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

abstract class PluginLib {
    abstract val nameInJs: String

    abstract fun import(scope: Scriptable, context: Context)
}