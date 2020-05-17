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

package org.itxtech.miraijs.bridge

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.Command
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.registerCommand
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.subscribeAlways
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.plugin.JsPlugin

class Core(private val plugin: JsPlugin) {
    private val events = hashMapOf<Class<Event>, ArrayList<JsCallback>>()
    private val botEvents = hashMapOf<Bot, HashMap<Class<Event>, ArrayList<JsCallback>>>()
    private val commands = arrayListOf<Command>()

    fun clear() {
        commands.forEach {
            CommandManager.unregister(it)
        }
        commands.clear()
    }

    fun detach() {
        botEvents.clear()
        events.clear()
    }

    fun attach() {
        plugin.subscribeAlways<Event> {
            if (plugin.enabled) {
                events[javaClass]?.forEach {
                    it.call(this)
                }
            }
        }
    }

    interface JsCallback {
        fun call(event: Event)
    }

    private fun subscribe(
        clz: Class<Event>, callback: JsCallback,
        map: HashMap<Class<Event>, ArrayList<JsCallback>>
    ) {
        if (map.containsKey(clz)) {
            map[clz]!!.add(callback)
        } else {
            map[clz] = arrayListOf(callback)
        }
    }

    fun subscribeAlways(clz: Class<Event>, callback: JsCallback) {
        subscribe(clz, callback, events)
    }

    @JvmOverloads
    fun launch(call: Co, sleep: Long = 0) = plugin.launch {
        delay(sleep)
        var d = 0L
        while (isActive && d != -1L) {
            delay(d)
            d = call.exec()
        }
    }

    interface Co {
        fun exec(): Long
    }

    @JvmOverloads
    fun registerCommand(
        cmdName: String,
        cmdDescription: String = "",
        cmdUsage: String = "",
        cmdAlias: List<String>? = null,
        cmd: CommandCallback
    ) = MiraiJs.registerCommand {
        name = cmdName
        alias = cmdAlias
        description = cmdDescription
        usage = cmdUsage
        onCommand {
            return@onCommand cmd.call(this, it)
        }
    }.apply { commands.add(this) }

    interface CommandCallback {
        fun call(sender: CommandSender, args: List<String>): Boolean
    }
}
