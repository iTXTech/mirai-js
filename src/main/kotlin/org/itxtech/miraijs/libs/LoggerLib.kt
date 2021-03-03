package org.itxtech.miraijs.libs

import net.mamoe.mirai.utils.*
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

@Suppress("unused")
class LoggerLib(plugin: PluginScope) : PluginLib(plugin) {
    override val nameInJs: String = "logger"

    fun verbose(any: Any) = MiraiJs.logger.verbose { "[${pluginScope.name}] $any" }
    fun info(any: Any) = MiraiJs.logger.info { "[${pluginScope.name}] $any" }
    fun debug(any: Any) = MiraiJs.logger.debug { "[${pluginScope.name}] $any" }
    fun error(any: Any) = MiraiJs.logger.error { "[${pluginScope.name}] $any" }
    fun warning(any: Any) = MiraiJs.logger.warning { "[${pluginScope.name}] $any" }

    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }
}