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
import org.itxtech.miraijs.MiraiJs

object BotUtil {
    fun getAll(): List<Bot> {
        return Bot.botInstances
    }

    fun get(qq: Long = 0): Bot {
        return if (qq == 0L) Bot.botInstances.first() else Bot.getInstance(qq)
    }
}

object CoroutineUtil {
    fun launch(call: Co) {
        MiraiJs.launch {
            var d = 0L
            while (isActive && d != -1L) {
                delay(d)
                d = call.exec()
            }
        }
    }

    interface Co {
        fun exec(): Long
    }
}
