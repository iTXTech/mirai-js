package org.itxtech.miraijs

import org.itxtech.miraijs.libs.KotlinCoroutineLib
import org.itxtech.miraijs.libs.MiraiLib
import org.itxtech.miraijs.libs.OkHttpLib
import org.itxtech.miraijs.libs.StorageLib
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

abstract class PluginLib(plugin: PluginScope) {
    abstract val nameInJs: String

    open val pluginScope: PluginScope = plugin

    @JvmSynthetic
    abstract fun importTo(scope: Scriptable, context: Context)
}

val knownPluginLibrary = listOf(
    KotlinCoroutineLib::class.java,
    MiraiLib::class.java,
    OkHttpLib::class.java,
    StorageLib::class.java
)