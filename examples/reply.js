// 插件信息
pluginInfo = {
    name: "JsPlugin Example",
    version: "1.0.0",
    author: "PeratX",
    website: "https://github.com/iTXTech/mirai-js/blob/master/examples/reply.js"
};

let verbose = true;

// onLoad 事件
plugin.ev.onLoad = () => {
    logger.info("插件已加载");
    let v = 0;
    // 启动协程
    core.launch(() => {
        v++;
        logger.info("正在等待：" + v);
        if (verbose) {
            // 100ms执行一次
            return 100;
        }
        // 停止协程，返回 -1
        return -1;
    });
    // 延时1000ms执行一次
    core.launch(() => {
        verbose = false
        return -1;
    }, 1000);
    // 命令名称，描述，帮助，别名，回调
    core.registerCommand("test", "测试命令", "test", null, (sender, args) => {
        logger.info("发送者：" + sender)
        logger.info("参数：" + args)
        return true
    });
};

plugin.ev.onEnable = () => {
    logger.info("插件已启用。" + (plugin.enabled ? "是真的" : "是假的"));
    try {
        // Http 基于 OkHttp，可使用 OkHttp 的 API 自行构造
        let result = http.get("https://github.com/mamoe/mirai");
        if (result.isSuccessful()) {
            logger.info("Mirai GitHub主页长度：" + result.body().string().length());
        } else {
            logger.error("无法访问Mirai GitHub主页");
        }
        // 手动调用 OkHttp
        let client = http.newClient()
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .build()
        let response = client.newCall(
            http.newRequest()
                .url("https://im.qq.com")
                .header("User-Agent", "NMSL Browser 1.0")
                .build()
        ).execute();
        if (response.isSuccessful()) {
            logger.info("QQ主页长度：" + response.body().string().length());
        } else {
            logger.error("无法访问QQ主页");
        }
    } catch (e) {
        logger.error("无法获取网页", e)
    }
};

plugin.ev.onDisable = () => {
    logger.info("插件已禁用。");
};

plugin.ev.onUnload = () => {
    logger.info("插件已卸载。");
};

core.subscribeAlways(BotOnlineEvent, ev => {
    logger.info(ev);
    core.subscribeBotAlways(ev.bot, GroupMessageEvent, ev => {
        logger.info(ev);
        ev.group.sendMessage(new PlainText("MiraiJs 收到消息：").plus(ev.message));
    })
});
