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

package org.itxtech.miraijs.deprecated.bridge

import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.deprecated.plugin.JsPlugin
import org.mozilla.javascript.IdScriptableObject
import org.mozilla.javascript.NativeJavaObject

class PluginLogger(private val plugin: JsPlugin) {
    private fun prefix() = plugin.pluginInfo.name

    fun info(str: String) {
        MiraiJs.logger.info("[${prefix()}] $str")
    }

    fun debug(str: String) {
        MiraiJs.logger.debug("[${prefix()}] $str")
    }

    @JvmOverloads
    fun error(str: String, e: Any? = null) {
        var t = e
        if (e is IdScriptableObject) {
            val obj = (e.get("javaException", e) as NativeJavaObject).unwrap()
            if (obj is Throwable) {
                t = obj
            }
        }
        if (t is Throwable) {
            MiraiJs.logger.error("[${prefix()}] $str", t)
        } else {
            MiraiJs.logger.error("[${prefix()}] $str")
        }
    }

    fun warning(str: String) {
        MiraiJs.logger.warning("[${prefix()}] $str")
    }

    fun verbose(str: String) {
        MiraiJs.logger.verbose("[${prefix()}] $str")
    }
}
