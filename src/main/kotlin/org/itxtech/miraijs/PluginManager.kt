package org.itxtech.miraijs

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.error
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
    val pluginFolder: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "plugins").also { it.mkdirs() } }
    val pluginData: File by lazy { File(MiraiJs.dataFolder.absolutePath + File.separatorChar + "data").also { it.mkdirs() } }

    private val plugins: HashSet<PluginInfo> = hashSetOf()

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val loadPluginDispatcher = newSingleThreadContext("JsPluginLoader")
    private val loadPluginsJobs = arrayListOf<Job>()
    private val loadPluginsLock = Mutex()
    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    val resourceDispatcher = newSingleThreadContext("JsResourceDispatcher")

    fun loadPlugins() {
        if (pluginFolder.isDirectory) {
            pluginFolder.listFiles()?.asSequence()?.forEach {
                loadPlugin(it)
            }
            MiraiJs.launch(loadPluginDispatcher) {
                waitLoadPluginsJobs()
                MiraiJs.logger.info("Loaded ${plugins.count()} plugin(s).")
            }
        } else throw RuntimeException("Plugin folder is not a folder.")
    }

    fun executePlugins() {
        plugins.forEach { it.scopeObject.execute() }
    }

    fun loadPlugin(file: File) {
        MiraiJs.launch(loadPluginDispatcher) { loadPluginsLock.withLock {
            try {
                val pkg = PluginPackage(file)
                if (plugins.any { it.identify == pkg.config!!.id }) {
                    MiraiJs.logger.error("Conflict to load ${pkg.config!!.name}(${pkg.config!!.id}): already loaded.")
                    withContext(resourceDispatcher) { pkg.closeAndRelease() }
                } else if(!pkg.hasMainScript()) {
                    MiraiJs.logger.error("Plugin ${pkg.config!!.name}(${pkg.config!!.id}) doesn't have main script.")
                    withContext(resourceDispatcher) { pkg.closeAndRelease() }
                } else {
                    MiraiJs.logger.info("Loading ${pkg.config!!.name}.")
                    val scope = PluginScope(pkg.config!!.id).also {
                        it.init()
                    }
                    withContext(resourceDispatcher) {
                        pkg.extractResources(
                            File(pluginData.absolutePath + File.separatorChar + pkg.config!!.id)
                        )
                        pkg.consumeScriptReaders {
                            scope.attachScript(it, this)
                        }
                    }
                    plugins.add(PluginInfo(pkg.config!!.id, pkg, scope))
                }
            } catch (ex: Exception) {
                MiraiJs.logger.error("Error while loading ${file.name}: $ex")
            }
        } }.also { loadPluginsJobs.add(it) }
    }

    suspend fun unloadPlugin(id: String) = MiraiJs.launch(loadPluginDispatcher) {
        plugins.find { it.identify == id }.run {
            if(this != null) {
                MiraiJs.logger.info("Waiting for plugin ${attachedPackage.config!!.name}($id) unload process...")
                scopeObject.unload()
                plugins.remove(this)
                MiraiJs.logger.info("Successfully unload plugin ${attachedPackage.config!!.name}($id)")
            } else MiraiJs.logger.error("Plugin $id not found.")
        }
    }

    suspend fun reloadPlugin(id: String) = MiraiJs.launch(loadPluginDispatcher) {
        plugins.find { it.identify == id }.run {
            if(this != null) {
                MiraiJs.logger.info("Reloading plugin ${attachedPackage.config!!.name}($id)...")
                scopeObject.run {
                    unload()
                    init()
                    attachedPackage.consumeScriptReaders {
                        attachScript(it, this)
                    }
                    execute()
                }
                MiraiJs.logger.info("Successfully reload plugin ${attachedPackage.config!!.name}($id)")
            } else MiraiJs.logger.error("Plugin $id not found.")
        }
    }

    fun listPlugins() = MiraiJs.logger.info { buildString {
        append("Loaded ${plugins.count()} plugin(s): ")
        plugins.forEach {
            append(it.attachedPackage.config!!.run { "\n\t$name($id) by $author" })
        }
    } }

    suspend fun waitLoadPluginsJobs() = loadPluginsJobs.joinAll()
}

data class PluginInfo(
    val identify: String,
    val attachedPackage: PluginPackage,
    val scopeObject: PluginScope
) {
    init {
        scopeObject.loadingExceptionListener = { _, throwable ->
            MiraiJs.logger.error {
                "Error while loading plugin ${attachedPackage.config!!.name}($identify) by ${
                    attachedPackage.config!!.author
                }.\nDetail: " +
                buildString { throwable.run {
                    append(this)
                    append("\n")
                    append(stackTrace.joinToString("\n") { "\tat $it" })
                } }
            }
        }
        scopeObject.runtimeExceptionListener = { _, throwable ->
            MiraiJs.logger.error {
                "Exception in MiraiJs plugin ${attachedPackage.config!!.name}($identify) by ${
                    attachedPackage.config!!.author
                }.\nDetail: " +
                    buildString { throwable.run {
                        append(this)
                        append("\n")
                        append(stackTrace.joinToString("\n") { "\tat $it" })
                    } }
            }
            scopeObject.launch(PluginManager.resourceDispatcher) {
                attachedPackage.closeAndRelease()
            }
        }
    }
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

    @SubCommand
    @Description("Load plugin from filesystem.")
    suspend fun CommandSender.load(@Name("File name") name: String) {
        PluginManager.loadPlugin(File(PluginManager.pluginFolder.toString() + File.separator + name))
    }
}