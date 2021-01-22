package org.itxtech.miraijs.libs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.kjbb.JvmBlockingBridge
import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginManager
import org.itxtech.miraijs.PluginScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.nio.charset.Charset

@Suppress("unused", "MemberVisibilityCanBePrivate")
class StorageLib(plugin: PluginScope) : PluginLib(plugin) {
    override val nameInJs: String = "storage"

    @JvmBlockingBridge
    suspend fun getResourceStream(path: String) =
        withContext(Dispatchers.IO + pluginScope.coroutineContext) {
            val resFile = getResourceFile(path)
            resFile.inputStream()
        }

    @JvmOverloads
    @JvmBlockingBridge
    suspend fun getResourceStreamReader(path: String, charset: Charset = Charset.defaultCharset()) =
        InputStreamReader(getResourceStream(path), charset)

    @JvmOverloads
    @JvmBlockingBridge
    suspend fun getResourceString(path: String, charset: Charset = Charset.defaultCharset()) = buildString {
        getResourceStreamReader(path, charset).readLines().also {
            append(it)
        }
    }

    fun getResourceFile(path: String) = File(pluginScope.dataFolder.absolutePath + path)

    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }
}