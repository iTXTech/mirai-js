plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "org.itxtech"
version = "1.1.0-rc.2"

kotlin {
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("InlineClasses")
            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }
    }
}

repositories {
    maven("https://mirrors.huaweicloud.com/repository/maven")
    maven("https://dl.bintray.com/him188moe/mirai")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
    api("org.jetbrains.kotlinx:atomicfu:0.14.4")

    implementation("net.mamoe:mirai-core:1.3.0")
    implementation("net.mamoe:mirai-console:1.0-M4")

    implementation("org.mozilla:rhino:1.7.13")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Name"] = "iTXTech MiraiJs"
        attributes["Revision"] = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            .inputStream.bufferedReader().readText().trim()
    }

    val list = ArrayList<Any>()
    configurations.compileClasspath.get().forEach { file ->
        arrayOf("rhino", "squareup").forEach {
            if (file.absolutePath.contains(it)) {
                list.add(zipTree(file))
            }
        }
    }

    from(list)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
    kotlinOptions.jvmTarget = "1.8"
}
