# Mirai Js

**运行于 `JVM` 的强大的 `Mirai JavaScript` 插件运行时**

使用 JavaScript 编写 [Mirai](https://github.com/mamoe/mirai) 插件，支持与所有 Mirai 面向 Java 的API 和部分 Kotlin DSL，[暂不支持](#Android-兼容性) `Android` 环境。

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

## 开发

查看开发文档：[docs/general.md](docs/general.md)

查看 API 文档：<未完成>

## Android 兼容性

插件的 `ExternalLibraryLib` 中提供了加载外部 jar 功能，使用了 Android 不支持的 `URLClassLoader`。

后续将会兼容 Android ： 使用 [D8](https://developer.android.google.cn/studio/command-line/d8) 将 Java 字节码编译为 Dex 字节码后使用 `DexClassLoader` 加载。

## [mirai-console-loader](https://github.com/iTXTech/mirai-console-loader) 兼容性

使用 mirai-console-loader 启动 mirai 时，脚本环境无法找到 `net.mamoe.mirai` 包，请使用原始方式启动：
```
java -cp "./libs/*" net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
```

详见：[#10](https://github.com/iTXTech/mirai-js/issues/10)

## 开源协议

    Copyright (C) 2020-2021 iTX Technologies
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
    
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.