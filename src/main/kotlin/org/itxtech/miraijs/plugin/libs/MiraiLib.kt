@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package org.itxtech.miraijs.plugin.libs

import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import org.itxtech.miraijs.plugin.PluginLib
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object MiraiLib : PluginLib() {
    override val nameInJs: String = "mirai"

    override fun import(scope: Scriptable, context: Context) {
        context.evaluateString(
            scope, """
            importPackage(net.mamoe.mirai);
            importPackage(net.mamoe.mirai.contract);
            importPackage(net.mamoe.mirai.data);
            importPackage(net.mamoe.mirai.event);
            importPackage(net.mamoe.mirai.event.events);
            importPackage(net.mamoe.mirai.message);
            importPackage(net.mamoe.mirai.message.data);
            importPackage(net.mamoe.mirai.message.action);
            importPackage(net.mamoe.mirai.utils);
            const $nameInJs = Packages.net.mamoe.mirai;
        """.trimIndent(), "importMirai", 1, null
        )
        ScriptableObject.putProperty(scope, nameInJs + "Kt", Context.javaToJS(this, scope))
    }

    fun <E : Event> wrapEventChannel(eventChannel: EventChannel<E>) =
        EventChannelKtWrapper(eventChannel)

    class EventChannelKtWrapper<E : Event>(val self: EventChannel<E>) {
        //GlobalEventChannel.filter is currently not available for Java
        fun filter(samCallback: MiraiLambdaInterface.EventChannelFilterSAMCallback<E>) =
            EventChannelKtWrapper(self.filter { samCallback.call(it) })

        //EventChannel.subscribeMessages is kotlin-only function.
        fun <R> subscribeMessages(
            coroutineContext: CoroutineContext,
            concurrencyKind: ConcurrencyKind,
            priority: EventPriority,
            samCallBack: MiraiLambdaInterface.EventChannelSubscribeMessagesSAMCallback<R>
        ): EventChannelKtWrapper<E> {
            self.subscribeMessages(coroutineContext, concurrencyKind, priority) {
                samCallBack.call(MessageEventSubscriberBuilderJsImpl(this))
            }
            return this
        }

        fun <R> subscribeMessages(
            samCallBack: MiraiLambdaInterface.EventChannelSubscribeMessagesSAMCallback<R>
        ) = subscribeMessages(EmptyCoroutineContext, ConcurrencyKind.CONCURRENT, EventPriority.MONITOR, samCallBack)

        fun unwrap() = self

        class MessageEventSubscriberBuilderJsImpl(val self: MessageEventSubscribersBuilder) {
            //always subscribe
            fun always(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Unit>
            ): Listener<MessageEvent> = self.always { samCallback.call(this, Unit) }

            //filter from message
            fun case(
                equals: String, ignoreCase: Boolean, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.case(equals, ignoreCase, trim) { samCallback.call(this, it) }

            fun case(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ) = case(equals, ignoreCase = false, trim = true, samCallback)

            fun match(
                regex: org.mozilla.javascript.regexp.NativeRegExp,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, MatchResult, Unit>
            ) = self.matching(Regex(regex.toString())) { samCallback.call(this, it) }

            fun contains(
                equals: String, ignoreCase: Boolean, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.contains(equals, ignoreCase, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.contains(equals) }.content
                )
            }

            fun contains(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ) = contains(equals, ignoreCase = false, trim = true, samCallback)

            fun startWith(
                equals: String, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.startsWith(equals, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.startsWith(equals) }.content
                )
            }

            fun startWith(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ) = startWith(equals, trim = true, samCallback)

            fun endsWith(
                suffix: String, removeSuffix: Boolean, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.endsWith(suffix, removeSuffix, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>()
                        .first { p -> p.content.endsWith(suffix, ignoreCase = false) }.content
                )
            }

            fun endsWith(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ) = endsWith(equals, removeSuffix = true, trim = true, samCallback)

            //filter from subject
            fun sentBy(
                qq: Long,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<FriendMessageEvent, Unit, Unit>
            ) = self.sentBy(qq) { samCallback.call(this as FriendMessageEvent, Unit) }

            fun sentByFriend(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<FriendMessageEvent, Unit, Unit>
            ) = self.sentByFriend { samCallback.call(this, Unit) }

            fun sentByStranger(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<StrangerMessageEvent, Unit, Unit>
            ) = self.sentByStranger { samCallback.call(this, Unit) }

            fun sentByGroupAdmin(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, Unit>
            ) = self.sentByAdministrator().invoke { samCallback.call(this.cast(), Unit) }

            fun sentByGroupOwner(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, Unit>
            ) = self.sentByOwner().invoke { samCallback.call(this.cast(), Unit) }

            fun sentByGroupTemp(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupTempMessageEvent, Unit, Unit>
            ) = self.sentByGroupTemp().invoke { samCallback.call(this.cast(), Unit) }

            //TODO: seems doesn't work
            fun sentFrom(
                group: Long,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, Unit>
            ) = self.sentFrom(group).invoke { samCallback.call(this.cast(), Unit) }

            fun atBot(samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Unit>) =
                self.atBot().invoke { samCallback.call(this.cast(), Unit) }

            fun atAll(samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, Unit>) =
                self.atAll().invoke { samCallback.call(this.cast(), Unit) }

            fun at(qq: Long, samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, At, Unit>) =
                self.at(qq).invoke {
                    samCallback.call(this.cast(),
                        this.message.filterIsInstance<At>().first { at -> at.target == qq }
                    )
                }

            fun <T : SingleMessage> has(
                type: Class<T>,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, SingleMessage, Unit>
            ) = self.content { message.any { it.javaClass == type } }.invoke {
                samCallback.call(this.cast(),
                    this.message.first { m -> m.javaClass == type }
                )
            }

            fun content(
                samCallbackJudge: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Boolean>,
                samCallbackExecute: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Unit>
            ) = self
                .content { samCallbackJudge.call(this, Unit) }
                .invoke { samCallbackExecute.call(this.cast(), Unit) }
        }
    }

    fun <T : MessageEvent> wrapMessageEvent(messageEvent: T) =
        MessageEventKtWrapper(messageEvent)

    class MessageEventKtWrapper(val self: MessageEvent) {
        @JvmBlockingBridge
        suspend fun <R> selectMessages(
            timeMillis: Long, filterContext: Boolean, priority: EventPriority,
            samCallback: MiraiLambdaInterface.MessageEventSelectMessageSAMCallback<R>
        ) = self.selectMessages(timeMillis, filterContext, priority) {
            samCallback.call(MessageEventSelectBuilderJsImpl(this))
        }

        @JvmBlockingBridge
        suspend fun <R> selectMessages(
            samCallback: MiraiLambdaInterface.MessageEventSelectMessageSAMCallback<R>
        ) = selectMessages(-1, true, EventPriority.MONITOR, samCallback)

        fun unwrap() = self

        class MessageEventSelectBuilderJsImpl<R>(val self: MessageSelectBuilder<MessageEvent, R>) {
            //selectMessages-only function
            fun default(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, R>
            ) = self.default { samCallback.call(this, Unit) }

            fun timeoutException(
                timeMillis: Long,
                samCallback: MiraiLambdaInterface.MessageEventSelectTimeoutSAMCallback<Unit>
            ) = self.timeoutException(timeMillis) { MessageSelectionTimeoutException().also { samCallback.call() } }

            fun timeout(
                timeMillis: Long,
                samCallback: MiraiLambdaInterface.MessageEventSelectTimeoutSAMCallback<R>
            ) = self.timeout(timeMillis) { samCallback.call() }

            //copy from [MessageEventSubscriberBuilderJsImpl]
            //filter from message
            fun case(
                equals: String, ignoreCase: Boolean, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.case(equals, ignoreCase, trim) { samCallback.call(this, it) }

            fun case(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = case(equals, ignoreCase = false, trim = true, samCallback)

            fun match(
                regex: org.mozilla.javascript.regexp.NativeRegExp,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, MatchResult, R>
            ) = self.matching(Regex(regex.toString())) { samCallback.call(this, it) }

            fun contains(
                equals: String, ignoreCase: Boolean, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.contains(equals, ignoreCase, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.contains(equals) }.content
                )
            }

            fun contains(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = contains(equals, ignoreCase = false, trim = true, samCallback)

            fun startWith(
                equals: String, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.startsWith(equals, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.startsWith(equals) }.content
                )
            }

            fun startWith(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = startWith(equals, trim = true, samCallback)

            fun endsWith(
                suffix: String, removeSuffix: Boolean, trim: Boolean,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.endsWith(suffix, removeSuffix, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>()
                        .first { p -> p.content.endsWith(suffix, ignoreCase = false) }.content
                )
            }

            fun endsWith(
                equals: String,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = endsWith(equals, removeSuffix = true, trim = true, samCallback)

            //filter from subject
            fun sentBy(
                qq: Long,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<FriendMessageEvent, Unit, R>
            ) = self.sentBy(qq) { samCallback.call(this as FriendMessageEvent, Unit) }

            fun sentByFriend(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<FriendMessageEvent, Unit, R>
            ) = self.sentByFriend { samCallback.call(this, Unit) }

            fun sentByStranger(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<StrangerMessageEvent, Unit, R>
            ) = self.sentByStranger { samCallback.call(this, Unit) }

            fun sentByGroupAdmin(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, R>
            ) = self.sentByAdministrator().invoke { samCallback.call(this.cast(), Unit) }

            fun sentByGroupOwner(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, R>
            ) = self.sentByOwner().invoke { samCallback.call(this.cast(), Unit) }

            fun sentByGroupTemp(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupTempMessageEvent, Unit, R>
            ) = self.sentByGroupTemp().invoke { samCallback.call(this.cast(), Unit) }

            //TODO: seems doesn't work
            fun sentFrom(
                group: Long,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, R>
            ) = self.sentFrom(group).invoke { samCallback.call(this.cast(), Unit) }

            fun atBot(samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, R>) =
                self.atBot().invoke { samCallback.call(this.cast(), Unit) }

            fun atAll(samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Unit, R>) =
                self.atAll().invoke { samCallback.call(this.cast(), Unit) }

            fun at(qq: Long, samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, At, R>) =
                self.at(qq).invoke {
                    samCallback.call(this.cast(),
                        this.message.filterIsInstance<At>().first { at -> at.target == qq }
                    )
                }

            fun <T : SingleMessage> has(
                type: Class<T>,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, SingleMessage, R>
            ) = self.content { message.any { it.javaClass == type } }.invoke {
                samCallback.call(this.cast(),
                    this.message.first { m -> m.javaClass == type }
                )
            }

            fun content(
                samCallbackJudge: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Boolean>,
                samCallbackExecute: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, R>
            ) = self
                .content { samCallbackJudge.call(this, Unit) }
                .invoke { samCallbackExecute.call(this.cast(), Unit) }
        }
    }
}

object MiraiLambdaInterface {
    interface EventChannelFilterSAMCallback<T> {
        fun call(value: T): Boolean
    }

    interface MessageListenerSAMInterface<T : MessageEvent, V, R> {
        fun call(event: T, value: V): R
    }

    interface EventChannelSubscribeMessagesSAMCallback<R> {
        fun call(msgSubscribersBuilder: MiraiLib.EventChannelKtWrapper.MessageEventSubscriberBuilderJsImpl): R
    }

    interface MessageEventSelectMessageSAMCallback<R> {
        fun call(msgSelectBuilder: MiraiLib.MessageEventKtWrapper.MessageEventSelectBuilderJsImpl<R>): R
    }

    interface MessageEventSelectTimeoutSAMCallback<R> {
        fun call(): R
    }
}