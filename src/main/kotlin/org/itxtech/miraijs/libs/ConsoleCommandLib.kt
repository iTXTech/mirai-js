package org.itxtech.miraijs.libs

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandOwner
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.message.data.MessageChain
import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginScope
import org.itxtech.miraijs.utils.KtLambdaInterfaceBridge
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

@Suppress("unused")
class ConsoleCommandLib(plugin: PluginScope) : PluginLib(plugin) {
    override val nameInJs: String = "command"

    @JvmOverloads
    fun register(
        name: String, desc: String = "MiraiJS plugin ${pluginScope.name} registered command: $name",
        usage: String = "<No usage description>", prefixOptional: Boolean = false,
        alias: List<String> = listOf(), permission: Permission = RootCommandOwner.parentPermission,
        override: Boolean = false, callback: KtLambdaInterfaceBridge.DoubleArgument<CommandSender, MessageChain, Unit>
    ) = JSCommand(name, alias, desc, usage, prefixOptional, permission, callback).also {
        pluginScope.registeredCommands.add(it)
        it.register(override)
    }

    fun unregister(command: JSCommand) {
        command.unregister()
    }

    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }
}


class JSCommand(
    name: String,
    alias: List<String>,
    desc: String,
    usage: String,
    prefixOptional: Boolean,
    permission: Permission,
    private val callback: KtLambdaInterfaceBridge.DoubleArgument<CommandSender, MessageChain, Unit>
) : RawCommand(
    owner = RootCommandOwner,
    primaryName = name,
    secondaryNames = alias.toTypedArray(),
    description = desc,
    usage = usage,
    prefixOptional = prefixOptional,
    parentPermission = permission
) {
    override suspend fun CommandSender.onCommand(args: MessageChain) {
        callback.call(this, args)
    }
}


object RootCommandOwner : CommandOwner {
    override val parentPermission: Permission
        get() = PermissionService.INSTANCE.register(permissionId("*"), "MiraiJS root command.")

    override fun permissionId(name: String): PermissionId = PermissionId("miraijs", "command.$name")
}