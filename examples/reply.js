// 插件信息
pluginInfo = {
    name: "JsPlugin Example",
    version: "1.0.0",
    author: "PeratX"
};

let verbose = true;

// onLoad 事件
pluginEv.onLoad = () => {
    logger.info("插件已加载");
    let v = 0;
    // 启动协程
    co.launch(function () {
        v++;
        logger.info("正在等待：" + v);
        if (verbose) {
            // 100ms执行一次
            return 100;
        }
        // 停止协程
        return -1;
    });
    co.launchDelay(1000, () => {
        verbose = false
        return -1;
    });
};

pluginEv.onEnable = () => {
    logger.info("插件已启用。" + (plugin.enabled ? "是真的" : "是假的"));
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
};

coreEv.subscribeAlways(BotOnlineEvent, ev => {
    logger.info(ev);
});

coreEv.subscribeAlways(GroupMessageEvent, ev => {
    logger.info(ev);
    ev.group.sendMessage(new PlainText("MiraiJs 收到消息：").plus(ev.message));
});
