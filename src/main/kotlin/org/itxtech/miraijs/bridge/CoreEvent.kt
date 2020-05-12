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

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.subscribeAlways
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.plugin.JsPlugin

class CoreEvent(private val plugin: JsPlugin) {
    private val events = hashMapOf<Class<Event>, ArrayList<JsCallback>>()
    private val botEvents = hashMapOf<Bot, HashMap<Class<Event>, ArrayList<JsCallback>>>()
    private val listeners = arrayListOf<Listener<Event>>()

    init {
        listeners.add(MiraiJs.subscribeAlways {
            if (plugin.enabled) {
                events[javaClass]?.forEach {
                    it.call(this)
                }
            }
        })
    }

    private fun subscribe(clz: Class<Event>, callback: JsCallback, map: HashMap<Class<Event>, ArrayList<JsCallback>>) {
        if (map.containsKey(clz)) {
            map[clz]!!.add(callback)
        } else {
            map[clz] = arrayListOf(callback)
        }
    }

    fun subscribeAlways(clz: Class<Event>, callback: JsCallback) {
        subscribe(clz, callback, events)
    }

    fun subscribeBotAlways(bot: Bot, clz: Class<Event>, callback: JsCallback) {
        if (!botEvents.containsKey(bot)) {
            botEvents[bot] = hashMapOf()
            listeners.add(bot.subscribeAlways {
                if (plugin.enabled) {
                    botEvents[bot]?.get(javaClass)?.forEach {
                        it.call(this)
                    }
                }
            })
        }
        subscribe(clz, callback, botEvents[bot]!!)
    }

    fun clear() {
        botEvents.clear()
        events.clear()
        listeners.forEach {
            it.cancel()
        }
        listeners.clear()
    }
}

interface JsCallback {
    fun call(event: Event)
}
