@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package org.itxtech.miraijs.plugin.libs

import kotlinx.coroutines.*
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.*
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

    @JvmSynthetic
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
        //EventChannel.filter is currently not available for Java
        fun filter(samCallback: MiraiLambdaInterface.EventChannelFilterSAMCallback<E>) =
            EventChannelKtWrapper(self.filter { samCallback.call(it) })

        //EventChannel.subscribeMessages is kotlin-only function.
        @JvmOverloads
        fun <R> subscribeMessages(
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
            concurrencyKind: ConcurrencyKind = ConcurrencyKind.CONCURRENT,
            priority: EventPriority = EventPriority.MONITOR,
            samCallBack: MiraiLambdaInterface.EventChannelSubscribeMessagesSAMCallback<R>
        ): EventChannelKtWrapper<E> {
            self.subscribeMessages(coroutineContext, concurrencyKind, priority) {
                samCallBack.call(MessageEventSubscriberBuilderJsImpl(this))
            }
            return this
        }

        fun unwrap() = self

        class MessageEventSubscriberBuilderJsImpl(val self: MessageEventSubscribersBuilder) {
            //always subscribe
            fun always(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Unit>
            ): Listener<MessageEvent> = self.always { samCallback.call(this, Unit) }

            //filter from message
            @JvmOverloads
            fun case(
                equals: String, ignoreCase: Boolean = false, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.case(equals, ignoreCase, trim) { samCallback.call(this, it) }

            fun match(
                regex: org.mozilla.javascript.regexp.NativeRegExp,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, MatchResult, Unit>
            ) = self.matching(Regex(regex.toString())) { samCallback.call(this, it) }

            @JvmOverloads
            fun contains(
                equals: String, ignoreCase: Boolean = false, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.contains(equals, ignoreCase, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.contains(equals) }.content
                )
            }

            @JvmOverloads
            fun startWith(
                equals: String, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.startsWith(equals, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.startsWith(equals) }.content
                )
            }

            @JvmOverloads
            fun endsWith(
                suffix: String, removeSuffix: Boolean = true, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, Unit>
            ): Listener<MessageEvent> = self.endsWith(suffix, removeSuffix, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>()
                        .first { p -> p.content.endsWith(suffix, ignoreCase = false) }.content
                )
            }

            //filter from subject
            fun sentBy(
                qq: Long,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<FriendMessageEvent, Friend, Unit>
            ) = self.sentBy(qq) { samCallback.call(this as FriendMessageEvent, subject) }

            fun sentByFriend(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<FriendMessageEvent, Friend, Unit>
            ) = self.sentByFriend { samCallback.call(this, subject) }

            fun sentByStranger(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<StrangerMessageEvent, Stranger, Unit>
            ) = self.sentByStranger { samCallback.call(this, subject) }

            fun sentByGroupAdmin(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Member, Unit>
            ) = self.sentByAdministrator().invoke { samCallback.call(this.cast(), this.cast()) }

            fun sentByGroupOwner(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Member, Unit>
            ) = self.sentByOwner().invoke { samCallback.call(this.cast(), this.cast()) }

            fun sentByGroupTemp(
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupTempMessageEvent, NormalMember, Unit>
            ) = self.sentByGroupTemp().invoke { samCallback.call(this.cast(), this.cast()) }

            //TODO: seems doesn't work
            fun sentFrom(
                group: Long,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Group, Unit>
            ) = self.sentFrom(group).invoke { samCallback.call(this.cast(), this.cast()) }

            fun atBot(samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, Unit, Unit>) =
                self.atBot().invoke { samCallback.call(this.cast(), Unit) }

            fun atAll(samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<GroupMessageEvent, Group, Unit>) =
                self.atAll().invoke { samCallback.call(this.cast(), this.cast()) }

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
        @JvmOverloads
        @JvmBlockingBridge
        suspend fun <R> selectMessages(
            timeMillis: Long = -1, filterContext: Boolean = true, priority: EventPriority = EventPriority.MONITOR,
            samCallback: MiraiLambdaInterface.MessageEventSelectMessageSAMCallback<R>
        ) = self.selectMessages(timeMillis, filterContext, priority) {
            samCallback.call(MessageEventSelectBuilderJsImpl(this))
        }

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
            @JvmOverloads
            fun case(
                equals: String, ignoreCase: Boolean = false, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.case(equals, ignoreCase, trim) { samCallback.call(this, it) }

            fun match(
                regex: org.mozilla.javascript.regexp.NativeRegExp,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, MatchResult, R>
            ) = self.matching(Regex(regex.toString())) { samCallback.call(this, it) }

            @JvmOverloads
            fun contains(
                equals: String, ignoreCase: Boolean = true, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.contains(equals, ignoreCase, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.contains(equals) }.content
                )
            }

            @JvmOverloads
            fun startWith(
                equals: String, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.startsWith(equals, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>().first { p -> p.content.startsWith(equals) }.content
                )
            }

            @JvmOverloads
            fun endsWith(
                suffix: String, removeSuffix: Boolean = true, trim: Boolean = true,
                samCallback: MiraiLambdaInterface.MessageListenerSAMInterface<MessageEvent, String, R>
            ) = self.endsWith(suffix, removeSuffix, trim) {
                samCallback.call(
                    this,
                    this.message.filterIsInstance<PlainText>()
                        .first { p -> p.content.endsWith(suffix, ignoreCase = false) }.content
                )
            }

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

    @JvmField
    val utils = LinearSyncHelperKtWrapper

    //mostly copy from: net.mamoe.mirai.event.syncFromEvent and nextMessage
    //modify to make js caller comfortable!
    @Suppress("DeferredIsResult")
    object LinearSyncHelperKtWrapper {
        @JvmOverloads
        @JvmBlockingBridge
        suspend fun <E : Event, R : Any> syncFromEvent(
            clazz: Class<E>,
            timeoutMillis: Long = -1,
            priority: EventPriority = EventPriority.MONITOR,
            samCallback: MiraiLambdaInterface.SyncEventSAMCallback<E, R>
        ): R? {
            require(timeoutMillis == -1L || timeoutMillis > 0) { "timeoutMillis must be -1 or > 0" }
            return withTimeoutOrNullOrCoroutineScope(timeoutMillis) {
                this@LinearSyncHelperKtWrapper.syncFromEventImpl(clazz, this, priority) {
                    samCallback.call(it)
                }
            }
        }

        //TODO: add a asyncFromEvent that uses plugin coroutine scope
        @JvmOverloads
        fun <E : Event, R : Any> asyncFromEvent(
            coroutineScope: CoroutineScope,
            clazz: Class<E>,
            timeoutMillis: Long = -1,
            priority: EventPriority = EventPriority.MONITOR,
            samCallback: MiraiLambdaInterface.SyncEventSAMCallback<E, R>
        ): KotlinCoroutineLib.DeferredJsImpl<R?> {
            require(timeoutMillis == -1L || timeoutMillis > 0) { "timeoutMillis must be -1 or > 0" }
            return KotlinCoroutineLib.DeferredJsImpl(
                coroutineScope.async(context = coroutineScope.coroutineContext) {
                    syncFromEvent(clazz, timeoutMillis, priority, samCallback)
                }
            )
        }

        @JvmOverloads
        @JvmBlockingBridge
        suspend fun <E : Event> nextEvent(
            clazz: Class<E>,
            timeoutMillis: Long = -1,
            priority: EventPriority = EventPriority.MONITOR,
            samCallback: MiraiLambdaInterface.SyncEventSAMCallback<E, Boolean> =
                object : MiraiLambdaInterface.SyncEventSAMCallback<E, Boolean> {
                    override fun call(event: E): Boolean {
                        return true
                    }
                }
        ): E? {
            require(timeoutMillis == -1L || timeoutMillis > 0) { "timeoutMillis must be -1 or > 0" }
            return withTimeoutOrNullOrCoroutineScope(timeoutMillis) {
                nextEventImpl(clazz, this, priority) { samCallback.call(it) }
            }
        }

        private suspend fun <E : Event, R> syncFromEventImpl(
            clazz: Class<E>,
            coroutineScope: CoroutineScope,
            priority: EventPriority,
            mapper: (E) -> R?
        ): R = suspendCancellableCoroutine { continuation ->
            coroutineScope.globalEventChannel().subscribe(clazz.kotlin, priority = priority) {
                runCatching {
                    continuation.resumeWith(
                        Result.success(
                            mapper.invoke(this) ?: return@subscribe ListeningStatus.LISTENING
                        )
                    )
                }
                return@subscribe ListeningStatus.STOPPED
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private suspend fun <E : Event> nextEventImpl(
            clazz: Class<E>,
            coroutineScope: CoroutineScope,
            priority: EventPriority,
            filter: (E) -> Boolean
        ): E = suspendCancellableCoroutine { continuation ->
            coroutineScope.globalEventChannel().subscribe(clazz.kotlin, priority = priority) {
                if (!filter(this)) return@subscribe ListeningStatus.LISTENING
                runCatching {
                    continuation.resumeWith(Result.success(this))
                }
                return@subscribe ListeningStatus.STOPPED
            }
        }

        private suspend inline fun <R> withTimeoutOrNullOrCoroutineScope(
            timeoutMillis: Long,
            noinline block: suspend CoroutineScope.() -> R
        ): R? {
            require(timeoutMillis == -1L || timeoutMillis > 0) { "timeoutMillis must be -1 or > 0 " }
            return if (timeoutMillis == -1L) {
                coroutineScope(block)
            } else {
                withTimeoutOrNull(timeoutMillis, block)
            }
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

    interface SyncEventSAMCallback<E : Event, R> {
        fun call(event: E): R
    }
}