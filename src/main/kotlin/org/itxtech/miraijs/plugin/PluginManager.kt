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

import org.itxtech.miraijs.MiraiJs
import java.io.File

object PluginManager {
    private val plDir: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "plugins").also { it.mkdirs() } }
    private val plData: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "data").also { it.mkdirs() } }

    private val plugins = arrayListOf<JsPlugin>()

    fun loadPlugins() {
        if (!MiraiJs.dataFolder.isDirectory) {
            MiraiJs.logger.error("数据文件夹不是一个文件夹！" + MiraiJs.dataFolder.absolutePath)
        } else {
            plDir.listFiles()?.forEach { file ->
                if (file.isFile && file.absolutePath.endsWith("js")) {
                    loadPlugin(file)
                }
            }
        }
    }

    fun loadPlugin(file: File) {
        MiraiJs.logger.info("正在加载JS插件：" + file.absolutePath)
        val plugin = JsPlugin(file)
        plugin.load()
        plugins.add(plugin)
    }

    fun enablePlugins() {
        plugins.forEach {
            it.enable()
        }
    }

    fun disablePlugins() {
        plugins.forEach {
            it.disable()
        }
    }
}
