pluginInfo = {
    name: "JsPlugin Example",
    version: "1.0.0",
    author: "PeratX"
}

pluginEvent.onLoad = function () {
    logger.info("Plugin Loaded")
}

coreEvent.subscribeAlways(BotOnlineEvent, function (ev) {
    logger.info(ev)
})

coreEvent.subscribeAlways(GroupMessageEvent, function (ev) {
    logger.info(ev)
    ev.group.sendMessage(new PlainText("MiraiJs 收到消息：").plus(ev.message))
})
