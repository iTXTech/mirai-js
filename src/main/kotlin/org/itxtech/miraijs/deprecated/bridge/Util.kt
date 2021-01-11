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

import net.mamoe.mirai.Bot
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

object BotUtil {
    fun getAll() = Bot.instances

    fun get(qq: Long = 0) = if (qq == 0L) Bot.instances.first() else Bot.getInstance(qq)
}

object HttpUtil {
    val client = OkHttpClient.Builder()
        .connectTimeout(1000, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .addInterceptor {
            return@addInterceptor it.proceed(
                it.request().newBuilder()
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"
                    )
                    .build()
            )
        }
        .build()

    fun get(url: String): Response {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute()
    }

    fun newClient(): OkHttpClient.Builder = OkHttpClient.Builder()

    fun newRequest(): Request.Builder = Request.Builder()

    fun newRequestBody(str: String, type: MediaType? = null) = str.toRequestBody(type)
}

object StorageUtil {
    @JvmOverloads
    fun writeText(file: File, str: String, charset: Charset = Charsets.UTF_8) {
        file.writeText(str, charset)
    }

    @JvmOverloads
    fun readText(file: File, charset: Charset = Charsets.UTF_8) = file.readText(charset)

    fun writeBytes(file: File, bytes: ByteArray) = file.writeBytes(bytes)
}
