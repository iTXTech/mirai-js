package org.itxtech.miraijs.libs

import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset

@Suppress("unused", "MemberVisibilityCanBePrivate", "FunctionName")
class StorageLib(plugin: PluginScope) : PluginLib(plugin) {
    override val nameInJs: String = "storage"
    lateinit var typeMapper: StorageGetTypeMapper<*>

    /*
     * Storage related.
     */

    fun `$internalSetTypeMapper$`(mapper: StorageGetTypeMapper<*>) {
        typeMapper = mapper
    }

    @JvmOverloads
    fun get(key: String, default: Any? = null) = typeMapper.call(pluginScope.data.run {
        numbers[key] ?: strings[key] ?: booleans[key] ?: default
        ?: throw NoSuchElementException("No default value passed for the value of $key which is non-exist.")
    })

    fun put(key: String, value: Number) {
        pluginScope.data.numbers[key] = value.toDouble()
    }

    fun put(key: String, value: Boolean) {
        pluginScope.data.booleans[key] = value
    }

    fun put(key: String, value: String) {
        pluginScope.data.strings[key] = value
    }

    fun remove(key: String) = pluginScope.data.run {
        numbers.remove(key)
        booleans.remove(key)
        strings.remove(key)
        Unit
    }

    fun clear() = pluginScope.data.run {
        numbers.clear()
        booleans.clear()
        strings.clear()
    }

    fun contains(key: String) = pluginScope.data.run {
        numbers.containsKey(key) || booleans.containsKey(key) || strings.containsKey(key)
    }

    fun reload() = MiraiJs.withConsolePluginContext { pluginScope.data.reload() }

    /*
     * Resource related.
     */
    @JvmBlockingBridge
    suspend fun getResourceStream(path: String) =
        withContext(Dispatchers.IO + pluginScope.coroutineContext) {
            val resFile = getResourceFile(path)
            resFile.inputStream()
        }

    @JvmOverloads
    @JvmBlockingBridge
    suspend fun getResourceStreamReader(
        path: String,
        charset: Charset = Charset.forName("UTF-8")
    ) = InputStreamReader(getResourceStream(path), charset)

    @JvmOverloads
    @JvmBlockingBridge
    suspend fun getResourceString(
        path: String,
        charset: Charset = Charset.defaultCharset()
    ) = buildString {
        getResourceStreamReader(path, charset).readLines().also {
            append(it)
        }
    }

    fun getResourceFile(path: String) = File(pluginScope.dataFolder.absolutePath + path)

    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
        context.evaluateString(scope, """
            storage.${'$'}internalSetTypeMapper${'$'}((value) => { try { 
                return eval(value.toString());
            } catch(ex) {
                if(/ReferenceError/.test(ex.toString())) {
                    return value.toString();
                } else throw new Error("Cannot resolve value: " + value + ", reason:" + ex);
            } });
        """.trimIndent(), "importStorageLib", 1, null)
    }
}

interface StorageGetTypeMapper<T> {
    fun call(value: Any) : T?
}

class PrimitivePluginData(name: String): AutoSavePluginData(name) {
    val numbers: MutableMap<String, Double> by value(mutableMapOf())
    val booleans: MutableMap<String, Boolean> by value(mutableMapOf())
    val strings: MutableMap<String, String> by value(mutableMapOf())
}