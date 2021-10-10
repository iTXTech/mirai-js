# Mirai Js 插件开发总览

`Mirai Js` 是使用 [Mozilla Rhino](https://github.com/mozilla/rhino) 作为引擎的，运行于 `JVM` 上的 `Mirai console` 插件，支持热加载/卸载，支持完整的 Mirai 面向 Java 的 API 和 部分 Kotlin DSL。

## 插件结构

### 目录树

```tree
myplugin.zip
├─── scripts  <---- 存放 JavaScript 插件主体。
│   ├─── main.js  <---- 主脚本。
│   ├─── other1.js <---- 其他脚本模块。
│   └─── other2.js   <---- 其他脚本模块。
├─── resources  <---- 资源文件，会被在首次加载时解压到 data/org...MiraiJs/data/<插件 ID>/ 中。
│   ├─── a.jpg
│   ├─── b.jar
│   ├─── c.jar
│   ├─── d.json
│   └─── ...
└─── config.json <---- 插件信息。
```

请注意：`scripts` 一定存在 `main.js` 脚本，否则这个插件将无法被加载。

### `config.json` 结构

```javascript
{
    // 插件ID，必须是独一无二的，否则会与其他插件冲突。
    "id": "me.myname.testplugin",
    // 插件名称，可以为空。
    "name": "TestPlugin",
    // 插件作者，可以为空。
    "author": "PluginAuthor",
    // 插件描述，可以为空。
    "description": "This is a test mirai-js plugin."
}
```

## 编写插件

#### 1. 与 mirai 面向 Java 的 API 互动

`main.js`：

```javascript
importClass(net.mamoe.mirai.event.GlobalEventChannel);
importClass(net.mamoe.mirai.event.events.BotOnlineEvent);
importPackage(net.mamoe.mirai)

let bot = null;
let listener = GlobalEventChannel.INSTANCE.subscribeAlways(BotOnlineEvent, (event) => {
    logger.info("Bot " + event.getBot() + " is now online!");
    bot = event.getBot();
});
```

控制台：

```
> login 123456789 password
I/Bot.123456789: Logging in...
I/Bot.123456789: Online OtherClients: mirai
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] Bot Bot(123456789) is now online!
I/Bot.123456789: Login successful
```

得益于 Rhino 对 Java 的转换，你可以看到，Mirai Js 与 mirai 的交互和直接使用 Java 开发 mirai 是非常相似的。

上述 JS 的等价 Java 代码：

```java
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.BotOnlineEvent;

public class MiraiJava {
    public static Bot bot = null;
    public static void main() {
        Listener<BotOnlineEvent> listener = GlobalEventChannel.INSTANCE.subscribeAlways(
            BotOnlineEvent.class, (event) -> {
            	System.out.println("Bot " + event.getBot() + " is now online!");
            	bot = event.getBot();
        	}
        );
    }
}
```

你可以在 [Scripting Java - Mozilla | MDN](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scripting_Java) 查看更多关于在 Rhino JavaScript 中调用 Java 的信息。

### 2. 插件生命周期

Mirai Js 支持热加载/卸载，在控制台输入 `/jpm unload <插件 ID>` 来卸载这个插件。

调用这个命令只是将当插件的 `isUnloadFlag` 设置为 `true`，插件的卸载工作实际上是由插件开发者来完成的。

插件开发者需要配合用户的卸载操作，尽最大可能地在卸载后不残留资源。

* 使用 `coroutine.isCancelled()` 来获取 `isUnloadFlag` 状态，当为 `true` 时，插件**应该执行卸载动作**或**使插件自然完成执行**。

*  在调用卸载命令后，所有在插件中启动的 Kotlin 协程会取消执行并等待执行结束。

  > 在插件中启动的协程默认都是 [`pluginParentJob`](https://github.com/iTXTech/mirai-js/blob/master/src/main/kotlin/org/itxtech/miraijs/PluginScope.kt#L19) 的子协程。

下面将列举几个正确地配合卸载的操作

* **事件订阅**：减少 `EventChannel.subscribeAlways` 的使用，因为 `subscribeAlways` 不可被阻断，请尽量使用 `subscribeOnce` 和 `subscribe`。

  ```javascript
  importPackage(net.mamoe.mirai.event)
  importClass(net.mamoe.mirai.event.events.GroupMessageEvent);
  
  GlobalEventChannel.subscribe(GroupMessageEvent, (event) => {
  	if(!coroutine.isCancelled()) {
  		doSomething();
  		return ListeningStatus.LISTENING;
  	} else return ListeningStatus.STOPPED
  });
  ```

* 线程：为线程或根命名域中执行的循环操作添加插件终止判断。

  ```javascript
  importClass(java.lang.Thread);
  importClass(java.lang.Runnable);
  
  let thread = new Thread(new Runnable({
      run: function() {
          while(otherConditions() && coroutine.isCancelled()) {
              doSomething();
          }
          cleanup();
      }
  }));
  thread.start();
  ```

* 协程：为协程执行的循环操作添加协程终止判断。

  ```javascript
  let job = coroutine.launch((coroutineScope) => {
      while(otherConditions() && job.isCancelled()) {
          doSomething();
      }
      cleanup();
  });
  ```

### 3. 多文件

在插件目录中的 `scripts` 文件夹下，允许存在多个文件。加载插件时，会先编译所有的 JS 文件，然后加载 `main.js`。

除了 `main.js`，其他 JS 文件会被当做 **模块**(module)，只有在使用它们时才会加载运行。

通过 `module(filename)` 来加载并使用模块，在模块中通过为 `module.exports` 赋值来导出模块中的对象。

> 仿照了 NodeJS 中的 `require` 和 `module.exports` 的写法。

#### 示例

`main.js`

```javascript
let sayhello = module("hello");
sayhello.greet("LiHua");
```

`hello.js`

```javascript
module.exports = {
    template: "Hello {name}. Nice to meet you!",
    matcher: new RegExp("\\{name\\}", "gm"),
    greet: function(name) {
        logger.info(this.template.replace(this.matcher, name));
    }
}
```

控制台：

  ```
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] Hello LiHua. Nice to meet you!
  ```

模块嵌套也是允许的，你可以在一个模块中引用其他模块。

#### 模块引用

使用 `module()` 获取模块对象实际上是获得了它的引用，并不是拷贝或者新建了一个对象。

对 `module()` 获取的模块对象的修改实际上就是对模块本身的修改。

例如：

`main.js`

```javascript
let repeatModule = module("repeat");

command.register("stopRepeat", (sender, args) => {
    repeatModule.status = false;
});
```

`repeat.js`

```javascript
let coroutineStatus = {
    counter: 0,
    status: true
}

let job = coroutine.launch((coroutineScope) => {
    while(!job.isCancelled() && coroutineStatus.status) {
        logger.info("work in module's coroutine, now " + (++coroutineStatus.counter));
        coroutine.delay(2000);
    }
    logger.info("coroutine stopped!");
});

module.exports = coroutineStatus;
```


控制台：

```
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] work in module's coroutine, now 1
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] work in module's coroutine, now 2
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] work in module's coroutine, now 3
> stopRepeat
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] coroutine stopped!
```

#### 唯一运行

只有在首次调用 `module()` 时会加载运行对应的 JS 文件，再次调用相同的模块时会使用先前的缓存。

`main.js`

```javascript
let mod = module("module1");
mod.count = 114514;
logger.info(mod.count);
let refMod = module("module1");
logger.info(refMod.count);
```

`module1.js`

```javascript
module.exports = {
    count: 0
}
```

控制台

```
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] 114514.0
I/org.itxtech.miraijs.MiraiJs: [TestPlugin] 114514.0
```

#### 不可阻塞

模块 JS 文件的根命名域不可无限阻塞(如循环执行)，否则模块调用者将也会阻塞。

