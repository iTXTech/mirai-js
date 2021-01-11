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

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandOwner
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.MessageChain
import org.itxtech.miraijs.deprecated.plugin.JsPlugin

class Core(private val plugin: JsPlugin) {
    private val events = hashMapOf<Class<Event>, ArrayList<JsCallback>>()
    private val commands = arrayListOf<RawCommand>()

    fun clear() {
        commands.forEach { it.unregister() }
        commands.clear()
    }

    fun detach() {
        events.clear()
    }

    fun attach() {
        plugin.globalEventChannel().subscribeAlways<Event> {
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

    object JsPluginCommandOwner : CommandOwner {
        override val parentPermission: Permission by lazy {
            PermissionService.INSTANCE.register(
                permissionId("*"),
                "The parent of Mirai Js commands"
            )
        }

        override fun permissionId(name: String): PermissionId = PermissionId("miraijs", "command.$name")
    }

    @JvmOverloads
    fun registerCommand(
        cmd: CommandCallback,
        cmdName: String,
        cmdDescription: String = "",
        cmdUsage: String = "",
        prefixOptional: Boolean = false,
        cmdAlias: List<String> = listOf(),
        cmdPermission: Permission = JsPluginCommandOwner.parentPermission
    ) {
        object : RawCommand(
            owner = JsPluginCommandOwner,
            primaryName = cmdName,
            secondaryNames = cmdAlias.toTypedArray(),
            description = cmdDescription,
            usage = cmdUsage,
            prefixOptional = prefixOptional,
            parentPermission = cmdPermission
        ) {
            override suspend fun CommandSender.onCommand(args: MessageChain) {
                if (!cmd.call(this, args.contentToString().split(" "))) {
                    sendMessage(usage)
                }
            }
        }.apply {
            register()
            commands.add(this)
        }
    }

    interface CommandCallback {
        fun call(sender: CommandSender, args: List<String>): Boolean
    }
}
