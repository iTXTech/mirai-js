# Mirai Js

**运行于 `JVM` 的强大的 `Mirai JavaScript` 插件运行时**

使用 JavaScript 编写 [Mirai](https://github.com/mamoe/mirai) 插件，支持与所有 Mirai 面向 Java 的API 和部分 Kotlin DSL，[暂不支持]()`Android`环境。

## 使用须知

**所有基于`Mirai Js`的`JavaScript`插件必须遵循`AGPL-v3`协议开放源代码，详见 [协议文本](LICENSE) 。**

**API可能随时变动，请保持更新！**

## 特性

* 完整的 Mirai 面向 Java 的支持。
* 支持大部分 Kotlin Coroutine。
* 不完整的 [ES6 支持](https://mozilla.github.io/rhino/compat/engines.html)。
* 灵活地加载外部库。

## Mirai Js 插件管理器 `jpm`(`JavaScript Plugin Manager`)

1. 在 `mirai-console` 中键入 `/jpm` 获得帮助
1. `/jpm` 可 列出、加载、重载和卸载 插件。

`/jpm [list|load|reload|unload] <插件ID/文件名>`

## 使用

* 从 [Releases](https://github.com/iTXTech/mirai-js/releases) 中下载最新的版本，放入 mirai console 的插件文件夹，启动 mirai console。
* 在 `data/org.itxtech.miraijs.MiraiJs/plugins` 下放入 `zip` 格式的 Mirai Js 插件。
* 在 mirai console 输入 `/jpm load <文件名>` (不需要加 `.zip`) 来加载这个插件。

## 开源协议

    Copyright (C) 2020 iTX Technologies
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
    
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.xxxxxxxxxx Copyright (C) 2020 iTX TechnologiesThis program is free software: you can redistribute it and/or modifyit under the terms of the GNU Affero General Public License aspublished by the Free Software Foundation, either version 3 of theLicense, or (at your option) any later version.This program is distributed in the hope that it will be useful,but WITHOUT ANY WARRANTY; without even the implied warranty ofMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theGNU Affero General Public License for more details.You should have received a copy of the GNU Affero General Public Licensealong with this program.  If not, see <http://www.gnu.org/licenses/>.