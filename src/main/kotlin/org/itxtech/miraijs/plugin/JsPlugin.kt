/*
 *
 * Mirai Js
 *
 * Copyright (C) 2020 iTX Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author PeratX
 * @website https://github.com/iTXTech/mirai-js
 *
 */

package org.itxtech.miraijs.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.bridge.*
import org.mozilla.javascript.*
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.ContinuationInterceptor

data class JsPlugin(val file: File) {
    lateinit var dispatcher: PluginDispatcher
    lateinit var cx: Context
    lateinit var script: Script
    lateinit var scope: ImporterTopLevel
    lateinit var pluginInfo: PluginInfo
    private val pluginEvent = PluginEvent()
    private val coreEvent = CoreEvent(this)

    fun launch(b: suspend CoroutineScope.() -> Unit) {
        MiraiJs.launch(context = dispatcher, block = b)
    }

    private fun loadLibs() {
        ScriptableObject.putProperty(scope, "logger", Context.javaToJS(PluginLogger(this), scope))
        ScriptableObject.putProperty(scope, "pluginEvent", Context.javaToJS(pluginEvent, scope))
        ScriptableObject.putProperty(scope, "coreEvent", Context.javaToJS(coreEvent, scope))
        ScriptableObject.putProperty(scope, "bots", Context.javaToJS(BotUtil, scope))
        ScriptableObject.putProperty(scope, "co", Context.javaToJS(CoroutineUtil, scope))
        cx.evaluateString(
            scope, """
            importPackage(net.mamoe.mirai.event.events)
            importPackage(net.mamoe.mirai.message)
            importPackage(net.mamoe.mirai.message.data)
        """.trimIndent(), "importMirai", 1, null
        )
    }

    fun load() {
        dispatcher = PluginDispatcher()
        launch {
            cx = Context.enter()
            // See https://mozilla.github.io/rhino/compat/engines.html
            cx.languageVersion = Context.VERSION_ES6
            scope = ImporterTopLevel()
            scope.initStandardObjects(cx, false)
            loadLibs()

            script = cx.compileString(file.readText(), file.name, 1, null)
            script.exec(cx, scope)

            var info = scope["pluginInfo"]
            pluginInfo = if (info == ScriptableObject.NOT_FOUND) {
                MiraiJs.logger.error("未找到插件信息：" + file.absolutePath)
                PluginInfo(file.name)
            } else {
                info = info as NativeObject
                PluginInfo(
                    info["name"] as String,
                    info.getOrDefault("version", "") as String,
                    info.getOrDefault("author", "") as String,
                    info.getOrDefault("website", "") as String
                )
            }

            pluginEvent.onLoad?.run()
        }
    }

    fun enable() = launch {
        pluginEvent.onEnable?.run()
    }

    fun unload() = launch {
        Context.exit()
    }
}

data class PluginInfo(
    val name: String,
    val version: String = "未知",
    val author: String = "未知",
    val website: String = ""
)

class PluginDispatcher : ContinuationInterceptor by Executors.newFixedThreadPool(1).asCoroutineDispatcher()
