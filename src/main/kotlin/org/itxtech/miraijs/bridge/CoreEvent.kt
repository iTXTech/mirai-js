package org.itxtech.miraijs.bridge

import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.subscribeAlways
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.plugin.JsPlugin

class CoreEvent(private val plugin: JsPlugin) {
    private val events = hashMapOf<Class<Event>, JsCallback>()

    init {
        MiraiJs.subscribeAlways<Event> {
            events[javaClass]?.call(this)
        }
    }

    fun subscribeAlways(clz: Class<Event>, callback: JsCallback) {
        events[clz] = callback
    }
}

interface JsCallback {
    fun call(event: Event)
}
