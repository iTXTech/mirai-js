package org.itxtech.miraijs.plugin.libs

import org.intellij.lang.annotations.Language
import org.itxtech.miraijs.plugin.PluginLib
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

class MiraiLib : PluginLib() {
    override val nameInJs: String = "mirai"

    override fun import(scope: Scriptable, context: Context) {
        context.evaluateString(
            scope, """
            importPackage(net.mamoe.mirai);
            importPackage(net.mamoe.mirai.contract);
            importPackage(net.mamoe.mirai.data);
            importPackage(net.mamoe.mirai.event);
            importPackage(net.mamoe.mirai.event.events);
            importPackage(net.mamoe.mirai.message);
            importPackage(net.mamoe.mirai.message.data);
            importPackage(net.mamoe.mirai.message.action);
            importPackage(net.mamoe.mirai.message.utils);
            const $nameInJs = Packages.net.mamoe.mirai;
        """.trimIndent(), "importMirai", 1, null
        );
    }
}