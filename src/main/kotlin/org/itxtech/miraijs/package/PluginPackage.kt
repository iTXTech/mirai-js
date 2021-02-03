package org.itxtech.miraijs.`package`

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.File
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader


class PluginPackage(file: File) {
    private val zipFile: ZipFile = ZipFile(file)
    private val resourceList: MutableList<ZipEntry> = mutableListOf()
    private val scriptList: HashMap<String, ZipEntry> = hashMapOf()
    var config: PluginConfigInfo? = null

    init {
        var isReader: InputStreamReader? = null
        var zipInputStream: ZipInputStream? = null
        try {
            if (file.isFile && file.canRead()) {
                zipInputStream = ZipInputStream(file.inputStream())
                var zipEntry: ZipEntry?
                while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                    val entryPath = zipEntry.toString()
                    when {
                        entryPath.startsWith("resources/") -> resourceList.add(zipEntry!!)
                        entryPath.run { startsWith("scripts/") and endsWith(".js") } ->
                            scriptList[entryPath.run {
                                substringAfterLast("/").substringBeforeLast(".")
                            }] = zipEntry!!
                        entryPath == "config.json" -> {
                            isReader = InputStreamReader(zipFile.getInputStream(zipEntry!!))
                            config = Json.decodeFromString(readString(isReader, true))
                            isReader.close()
                        }
                    }
                }
                zipInputStream.close()
                if (config == null) throw Exception("No plugin config found.")
            }
        } catch (ex: Exception) {
            isReader?.close()
            zipInputStream?.closeEntry()
            zipInputStream?.close()
            throw ex
        }
    }

    suspend fun consumeScriptReaders(block: suspend InputStreamReader.(String) -> Unit) {
        scriptList.forEach { (s, zipEntry) ->
            val inputStreamReader = InputStreamReader(zipFile.getInputStream(zipEntry))
            block(inputStreamReader, s)
            inputStreamReader.close()
        }
    }

    suspend fun extractResources(pluginDataPath: File, override: Boolean = false) = withContext(Dispatchers.IO) {
        resourceList.forEach {
            val entryPath = it.toString().replace('/', File.separatorChar)
            val resFile = File(pluginDataPath.path + File.separatorChar + entryPath.substringAfter("/"))
            if (resFile.exists()) {
                if (override) extractFile(it, resFile)
            } else {
                resFile.createNewFile()
                extractFile(it, resFile)
            }
        }
    }

    private fun extractFile(zipEntry: ZipEntry, toPath: File) {
        val inputStream = zipFile.getInputStream(zipEntry)
        val fileOutputStream = FileOutputStream(toPath)
        val buffer = ByteArray(4096)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            fileOutputStream.write(buffer, 0, length)
        }
        inputStream.close()
        fileOutputStream.flush()
        fileOutputStream.close()
    }

    private fun readString(isReader: InputStreamReader, close: Boolean = false) = buildString {
        isReader.readLines().forEach { append(it) }
        if (close) isReader.close()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun closeAndRelease() = withContext(Dispatchers.IO) {
        zipFile.close()
    }

}