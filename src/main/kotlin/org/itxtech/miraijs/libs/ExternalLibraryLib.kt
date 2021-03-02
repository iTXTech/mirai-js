package org.itxtech.miraijs.libs

import org.itxtech.miraijs.MiraiJs
import org.itxtech.miraijs.PluginLib
import org.itxtech.miraijs.PluginScope
import org.mozilla.javascript.*
import org.mozilla.javascript.annotations.JSGetter
import java.io.File
import java.io.FileNotFoundException
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class ExternalLibraryLib(plugin: PluginScope) : PluginLib(plugin) {
    override val nameInJs: String = "external"

    fun load(file: File) = kotlin.run {
        if(!file.exists()) throw FileNotFoundException(file.toString())
        pluginScope.externalLibs[file] ?: ExternalLibraryClassLoaderJVM(file, pluginScope).also {
            pluginScope.externalLibs[file] = it
        }
    }

    fun load(file: File, rootPackage: String) = kotlin.run {
        if(!file.exists()) throw FileNotFoundException(file.toString())
        pluginScope.externalLibs[file] ?: ExternalLibraryClassLoaderJVM(file, pluginScope).run {
            pluginScope.externalLibs[file] = this
            this.`package`(rootPackage)
        }
    }


    @JvmSynthetic
    override fun importTo(scope: Scriptable, context: Context) {
        ScriptableObject.putProperty(scope, nameInJs, Context.javaToJS(this, scope))
    }
}

class ExternalLibraryClassLoaderJVM(
    file: File,
    private val attachedPluginScope: PluginScope
) : URLClassLoader(
    arrayOf(file.toURI().toURL()),
    ContextFactory.getGlobal().applicationClassLoader
) {
    private val cachedClass = ConcurrentHashMap<String, Class<*>>()

    override fun findClass(name: String): Class<*> =
        findClass(name, isLoadFromRhino = true) ?: throw ClassNotFoundException(name)

    private fun findClass(name: String, isLoadFromRhino: Boolean): Class<*>? {
        if(isLoadFromRhino) {
            attachedPluginScope.externalLibs.filterNot {
                it.value == this
            }.forEach {
                it.value.findClass(name, isLoadFromRhino = false).also { loadFromOthers ->
                    if(loadFromOthers != null) return loadFromOthers
                }
            }
        }
        return cachedClass[name] ?: try {
            super.findClass(name).also { cachedClass[name] = it }
        } catch(ex: ClassNotFoundException) { null }
    }

    @JvmOverloads
    fun `package`(rootPackage: String) = NativeJavaPackage(rootPackage, this).also {
        ScriptRuntime.setObjectProtoAndParent(it, attachedPluginScope.topLevelScope)
    }
}

