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

import kotlinx.atomicfu.atomic
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalAPI
import org.itxtech.miraijs.MiraiJs
import java.io.File

open class PluginManager {
    protected val plDir: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "plugins").also { it.mkdirs() } }
    protected val plData: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "data").also { it.mkdirs() } }

    protected val pluginId = atomic(0)
    protected val plugins = hashMapOf<Int, JsPlugin>()
    protected val command = JpmCommand(this)
    var optimizationLevel = detectOptLvl()

    private fun detectOptLvl() =
        try {
            Class.forName("android.os.Build")
            0
        } catch (e: Throwable) {
            -1
        }

    open fun getPluginDataDir(name: String) =
        File(plData.absolutePath + File.separatorChar + name).apply { mkdirs() }

    open fun loadPlugins() {
        if (!MiraiJs.dataFolder.isDirectory) {
            MiraiJs.logger.error("数据文件夹不是一个文件夹！" + MiraiJs.dataFolder.absolutePath)
        } else {
            plDir.listFiles()?.forEach { file ->
                loadPlugin(file)
            }
        }
    }

    open fun loadPlugin(file: File): Boolean {
        if (file.exists() && file.isFile && file.name.endsWith(".js")) {
            MiraiJs.logger.info("正在加载 MiraiJs 插件：" + file.name)
            plugins.values.forEach {
                if (it.file == file) {
                    return false
                }
            }
            val plugin = JsPlugin(this, pluginId.value, file)
            plugins.values.forEach {
                if (it.pluginInfo.name == plugin.pluginInfo.name) {
                    MiraiJs.logger.error("插件 \"${file.name}\" 与已加载的插件 \"${it.file.name}\" 冲突：相同的插件")
                    plugin.unload()
                    return false
                }
            }
            plugin.load()
            plugins[pluginId.getAndIncrement()] = plugin
            return true
        }
        return false
    }

    open fun enablePlugins() {
        plugins.values.forEach {
            it.enable()
        }
    }

    open fun disablePlugins() {
        plugins.values.forEach {
            it.disable()
        }
    }

    @OptIn(ConsoleExperimentalAPI::class)
    class JpmCommand(private val manager: PluginManager) : CompositeCommand(
        MiraiJs, "jpm",
        description = "Mirai Js 插件管理器"
    ) {
        private fun StringBuilder.getCommonPluginInfo(p: JsPlugin) {
            appendLine("Id：${p.id} 文件：${p.file.name} 名称：${p.pluginInfo.name} 状态：${if (p.enabled) "启用" else "停用"} 版本：${p.pluginInfo.version} 作者：${p.pluginInfo.author}")
            if (p.pluginInfo.website != "") {
                appendLine("主页：${p.pluginInfo.website}")
            }
        }

        @SubCommand
        suspend fun CommandSender.list() {
            sendMessage(buildString {
                appendLine("共加载了 ${manager.plugins.size} 个 MiraiJs 插件。")
                appendLine("")
                manager.plugins.values.forEach { p ->
                    getCommonPluginInfo(p)
                    appendLine("")
                }
            })
        }

        @SubCommand
        suspend fun CommandSender.load(@Name("Js文件名") file: String) {
            sendMessage(buildString {
                if (!manager.loadPlugin(File(manager.plDir.absolutePath + File.separatorChar + file))) {
                    appendLine("文件 \"${file}\" 非法。")
                }
            })
        }

        @SubCommand
        suspend fun CommandSender.unload(@Name("插件Id") id: Int) {
            sendMessage(buildString {
                if (manager.plugins.containsKey(id)) {
                    manager.plugins[id]!!.unload()
                    manager.plugins.remove(id)
                } else {
                    appendLine("Id $id 不存在。")
                }
            })
        }

        @SubCommand
        suspend fun CommandSender.enable(@Name("插件Id") id: Int) {
            sendMessage(buildString {
                if (manager.plugins.containsKey(id)) {
                    manager.plugins[id]!!.enable()
                } else {
                    appendLine("Id $id 不存在。")
                }
            })
        }

        @SubCommand
        suspend fun CommandSender.disable(@Name("插件Id") id: Int) {
            sendMessage(buildString {
                if (manager.plugins.containsKey(id)) {
                    manager.plugins[id]!!.disable()
                } else {
                    appendLine("Id $id 不存在。")
                }
            })
        }
    }

    open fun registerCommand() {
        command.register()
    }
}
