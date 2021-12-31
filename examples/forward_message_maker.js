/*
 *
 * Mirai Js Example plugin - Forward Message Maker
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

//这是一个自定义转发消息记录较笨。
//当在群聊中检测到 @xxx 说abc 时，
//bot 会发送一段不存在的消息记录，其中包含 xxx 说的 abc 这一条消息。

importClass(net.mamoe.mirai.event.GlobalEventChannel);
importClass(net.mamoe.mirai.event.ListeningStatus);
importClass(net.mamoe.mirai.event.events.GroupMessageEvent);
importClass(net.mamoe.mirai.message.code.MiraiCode);
importPackage(net.mamoe.mirai.message.data);

const matcher = /\u8bf4(.+)/gi;

GlobalEventChannel.INSTANCE.subscribe(GroupMessageEvent, event => {
    if (!coroutine.isCancelled()) {
        let message = event.getMessage();
        let group = event.getGroup();

        let ata = message.stream().filter(sm => sm instanceof At).findFirst().orElse(null);

        if (ata != null) {
            let messageCode = String(message.serializeToMiraiCode());
            let result = matcher.exec(messageCode);
            if (result != null) {
                let target = group.get(ata.getTarget());
                let chain = MiraiCode.deserializeMiraiCode(result[1]);


                let ataName = target.getNameCard();
                if (ataName.isEmpty()) ataName = target.getNick();

                let fwd = new ForwardMessageBuilder(group).add(target.getId(), ataName, chain).build();
                group.sendMessage(fwd);

                logger.info("Forward message has sent.");
                matcher.test(messageCode);
            }
        }
        
        return ListeningStatus.LISTENING;
    } else return ListeningStatus.STOPPED;
});