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

package org.itxtech.miraijs

import net.mamoe.mirai.console.plugins.PluginBase
import java.io.File

object MiraiJs : PluginBase() {
    private val plDir: File by lazy { File(dataFolder.absolutePath + File.separatorChar + "plugins").also { it.mkdirs() } }
    private val plData: File by lazy { File(dataFolder.absolutePath + File.separatorChar + "data").also { it.mkdirs() } }

    override fun onLoad() {
        super.onLoad()
    }

    override fun onEnable() {
        super.onEnable()
    }

    override fun onDisable() {
        super.onDisable()
    }
}
