package org.itxtech.miraijs

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info
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
    val pluginData: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "data").also { it.mkdirs() } }

    private val plugins: HashMap<String, PluginScope> = hashMapOf()

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val loadPluginDispatcher = newSingleThreadContext("JsPluginLoader")
    private val loadPluginsJobs = arrayListOf<Job>()
    private val loadPluginsLock = Mutex()

    fun loadPlugins() {
        if (pluginFolder.isDirectory) {
            pluginFolder.listFiles()?.asSequence()?.forEach {
                MiraiJs.launch(loadPluginDispatcher) { loadPluginsLock.withLock {
                    try {
                        val `package` = PluginPackage(it)
                        if (plugins.filter { it.key == `package`.config!!.id }.count() != 0) {
                            MiraiJs.logger.error("Conflict to load ${`package`.config!!.name}(${`package`.config!!.id}): already loaded.")
                        } else {
                            MiraiJs.logger.info("Loading ${`package`.config!!.name}.")
                            `package`.extractResources(
                                File(pluginData.absolutePath + File.separatorChar + `package`.config!!.id)
                            )
                            val pluginScope = PluginScope(`package`)
                            pluginScope.init()
                            pluginScope.compileScripts()
                            plugins[`package`.config!!.id] = pluginScope
                        }
                    } catch (ex: Exception) {
                        MiraiJs.logger.error("Error while loading ${it.name}: $ex")
                    }
                } }.also { loadPluginsJobs.add(it) }
            }
            MiraiJs.launch(loadPluginDispatcher) {
                waitLoadPluginsJobs()
                MiraiJs.logger.info("Loaded ${plugins.count()} plugin(s).")
            }

        } else throw RuntimeException("Plugin folder is not a folder.")
    }

    fun executePlugins() {
        plugins.forEach { (_, pluginScope) ->
            pluginScope.load()
        }
    }


    suspend fun unloadPlugin(id: String) = MiraiJs.launch(loadPluginDispatcher) {
        plugins[id].run {
            if(this != null) {
                MiraiJs.logger.info("Waiting for plugin $name($id) unload process...")
                unload()
                plugins.remove(id)
                MiraiJs.logger.info("Successfully unload plugin $name($id)")
            } else MiraiJs.logger.error("Plugin $id not found.")
        }
    }

    suspend fun reloadPlugin(id: String) = MiraiJs.launch(loadPluginDispatcher) {
        plugins[id].run {
            if(this != null) {
                MiraiJs.logger.info("Reloading plugin $name($id)...")
                reload()
                MiraiJs.logger.info("Successfully reload plugin $name($id)")
            } else MiraiJs.logger.error("Plugin $id not found.")
        }
    }

    suspend fun listPlugins() = MiraiJs.logger.info { buildString {
        append("Loaded ${plugins.count()} plugin(s): ")
        plugins.forEach {
            append("\n\t${it.value}")
        }
    } }

    suspend fun waitLoadPluginsJobs() = loadPluginsJobs.joinAll()
}

@ConsoleExperimentalApi
@Suppress("unused")
object JpmCommand : CompositeCommand(
    MiraiJs, "jpm", "MiraiJs插件管理器"
) {
    @SubCommand
    @Description("Reload a plugin.")
    suspend fun CommandSender.reload(@Name("Plugin Id") id: String) {
        PluginManager.reloadPlugin(id)
    }

    @SubCommand
    @Description("Unload a plugin and disable it.")
    suspend fun CommandSender.unload(@Name("Plugin Id") id: String) {
        PluginManager.unloadPlugin(id)
    }

    @SubCommand
    @Description("List all loaded plugin.")
    suspend fun CommandSender.list() {
        PluginManager.listPlugins()
    }
}