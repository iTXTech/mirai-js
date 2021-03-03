/*
 *
 * Mirai Js Example plugin - Repeater
 *
 * Copyright (C) 2021 iTX Technologies
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
 * @author StageGuard
 * @website https://github.com/iTXTech/mirai-js
 *
 */

//这是一个复读机，当同一条文本消息被发送 repeatWhen 次时它就会复读一遍。
//复读过后将不再复读这条消息，即使下一条消息还是这个消息。
//直到下一条消息内容不同时才重新记录。
var repeatWhen = 3;

//记录的消息
//{
//  repCount: number,
//  repeated: t/f,
//  content: "message conent",
//  group: 123456789
//}
var records = [];

var listener = mirai.event.GlobalEventChannel.INSTANCE.subscribeAlways(
    mirai.event.events.GroupMessageEvent, (event) => {
        var chain = event.getMessage();
        var groupId = event.getGroup().getId();
        //当只有文本消息时
        if (chain.size() == 2 &&
            chain.stream().anyMatch((sm) => {
                return sm instanceof mirai.message.data.PlainText;
            })
        ) {
            //获取文本消息字符串
            var content = chain.stream().filter((sm) => {
                return sm instanceof mirai.message.data.PlainText;
            }).findFirst().orElse(null).getContent().toString();

            for (var i in records) {
                if (records[i].group == groupId) {
                    if (records[i].content == content) {
                        if (
                            ++records[i].repCount == repeatWhen &&
                            !records[i].repeated
                        ) {
                            event.getGroup().sendMessage(content);
                            records[i].repCount = 1;
                            records[i].repeated = true;
                        }
                    } else {
                        records[i].content = content;
                        records[i].repeated = false;
                        records[i].repCount = 1;
                    }
                    return;
                }
            }
            records.push({
                repCount: 1,
                repeated: false,
                content: content,
                group: groupId
            });
        }
    }
);
logger.info("Repeater has started!");