package org.itxtech.miraijs

import org.itxtech.miraijs.libs.*
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

abstract class PluginLib(plugin: PluginScope) {
    abstract val nameInJs: String

    open val pluginScope: PluginScope = plugin

    @JvmSynthetic
    abstract fun importTo(scope: Scriptable, context: Context)
}

val knownPluginLibrary = listOf(
    KotlinCoroutineLib::class,
    MiraiLib::class,
    OkHttpLib::class,
    StorageLib::class,
    LoggerLib::class,
    ExternalLibraryLib::class,
    ConsoleCommandLib::class
)