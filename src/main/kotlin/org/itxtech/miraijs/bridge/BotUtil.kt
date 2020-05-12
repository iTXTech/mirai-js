package org.itxtech.miraijs.bridge

import net.mamoe.mirai.Bot

object BotUtil {
    fun getAll(): List<Bot> {
        return Bot.botInstances
    }

    fun get(qq: Long = 0): Bot {
        return if (qq == 0L) Bot.botInstances.first() else Bot.getInstance(qq)
    }
}
