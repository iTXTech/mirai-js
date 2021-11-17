# MiraiJs 2.0-M1 插件示例

注：仅为 js 示例，插件包见下文。

### 插件包结构：

```
myplugin.zip
├─── scripts  <---- 存放 JavaScript 插件主体。
│   ├─── main.js  <---- 主脚本，若存在则先加载 main.js，再加载其他脚本。
│   ├─── other1.js  <---- 其他脚本。
│   └─── other2.js  <---- 其他脚本。
├─── resources  <---- 资源文件，首次加载时解压到 
│   │               └ <base>/data/org.itxtech.miraijs.MiraiJs/data/<plugin id>
│   ├─── a.jpg
│   ├─── b.jar
│   ├─── c.jar
│   ├─── d.json
│   └─── ...
└─── config.json  <---- 插件信息。
```

### `config.json` 插件配置结构：

```javascript
{
    // 插件ID，必须是独一无二的，否则会冲突。
    "id": "me.myname.testplugin",
    // 插件名称，可以为空。
    "name": "TestPlugin",
    // 插件作者，可以为空。
    "author": "PluginAuthor",
    // 插件描述，可以为空。
    "description": "This is a test mirai-js plugin."
}
```

## 如何使用：

1. 按照上述插件包结构创建一个插件包。
2. 将示例的 js 直接（或改名为 `main.js` 后）放入插件包中的 `plugins` 文件夹。
3. 打包成 `zip` 压缩包，放入 `<mirai>/data/org.itxtech.miraijs.MiraiJs/plugins` 文件夹中。
4. 启动 mirai-console。
