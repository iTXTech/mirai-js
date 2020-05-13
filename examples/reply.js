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

// 插件信息
pluginInfo = {
    name: "JsPluginExample",
    version: "1.0.0",
    author: "PeratX",
    website: "https://github.com/iTXTech/mirai-js/blob/master/examples/reply.js"
};

let verbose = true;

// onLoad 事件
plugin.ev.onLoad = () => {
    logger.info("插件已加载：" + plugin.dataDir);

    // 插件数据读写
    let file = plugin.getDataFile("test.txt")
    // 第三个编码参数默认为 UTF-8，可空，同readText第二个参数
    stor.writeText(file, "真的很强。", Charset.forName("GBK"));
    logger.info("读取文件：" + file + " 内容：" + stor.readText(file, Charset.forName("GBK")));

    let config = new JsonConfig(plugin.getDataFile("test.json"));
    config.put("wow", "Hello World!");
    config.save();

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
