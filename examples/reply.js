// 插件信息
pluginInfo = {
    name: "JsPlugin Example",
    version: "1.0.0",
    author: "PeratX"
};

let verbose = true;

// onLoad 事件
pluginEvent.onLoad = () => {
    logger.info("插件已加载");
    let v = 0;
    // 启动协程
    co.launch(function () {
        v++;
        logger.info("正在等待：" + v);
        if (verbose) {
            // 500ms执行一次
            return 500;
        }
        // 停止协程
        return -1;
    });
};

pluginEvent.onEnable = () => {
    logger.info("插件已启用。");
};

coreEvent.subscribeAlways(BotOnlineEvent, ev => {
    logger.info(ev);
    // Bot上线后关闭
    verbose = false;
});

coreEvent.subscribeAlways(GroupMessageEvent, ev => {
    logger.info(ev);
    ev.group.sendMessage(new PlainText("MiraiJs 收到消息：").plus(ev.message));
});
