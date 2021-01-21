package org.itxtech.miraijs

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CompositeCommand
import org.itxtech.miraijs.`package`.PluginConfigInfo
import org.itxtech.miraijs.`package`.PluginPackage
import java.io.File
import java.lang.Exception

object PluginManager {
    var optimizationLevel = try {
        Class.forName("android.os.Build"); 0
    } catch (e: Throwable) {
        -1
    }
    private val pluginFolder: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "plugins").also { it.mkdirs() } }
    private val pluginData: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "data").also { it.mkdirs() } }

    private val loadedPlugins: HashMap<PluginConfigInfo, PluginScope> = hashMapOf()

    lateinit var loadPluginsJob: Job

    fun loadPlugins() = MiraiJs.launch {
        if (pluginFolder.isDirectory) {
            pluginFolder.listFiles()?.forEach {
                try {
                    val `package` = PluginPackage(it)
                    MiraiJs.logger.info("Loading ${`package`.config!!.name}.")
                    `package`.extractResources(
                        File(
                            pluginData.absolutePath + File.separatorChar + `package`.config!!.id
                        )
                    )
                    val pluginScope = PluginScope(`package`.config!!)
                    pluginScope.init()
                    `package`.consumeScriptReader { scriptName ->
                        pluginScope.attachScript(scriptName, this)
                    }
                    loadedPlugins[`package`.config!!] = pluginScope
                } catch (ex: Exception) {
                    MiraiJs.logger.error("Error while loading ${it.name}: $ex")
                }
            }
            MiraiJs.logger.info("Loaded ${loadedPlugins.count()} plugins.")
        } else throw RuntimeException("Plugin folder is not a folder.")
    }.also { loadPluginsJob = it }

    fun executePlugins() {
        loadedPlugins.forEach { (_, pluginScope) ->
            pluginScope.execute()
        }
    }

    suspend fun waitLoadPluginsJob() = loadPluginsJob.join()
}

object JpmCommand : CompositeCommand(
    MiraiJs, "jpm", "MiraiJs插件管理器"
) {

}