package org.itxtech.miraijs.libs

import net.mamoe.mirai.utils.*
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

@Suppress("unused")
class ExternalJarLoaderLib(plugin: PluginScope) : PluginLib(plugin) {
    override val nameInJs: String = "external"


    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }
}