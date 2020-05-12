plugins {
    kotlin("jvm") version "1.3.71"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.71"
}

group = "org.itxtech"
version = "1.0.0"

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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")

    implementation("net.mamoe:mirai-core:1.0-RC2-1")
    implementation("net.mamoe:mirai-console:0.5.1")

    implementation("org.mozilla:rhino:1.7.12")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Name"] = "iTXTech MiraiJs"
        attributes["Revision"] = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            .inputStream.bufferedReader().readText().trim()
    }

    val list = ArrayList<Any>()
    configurations.compileClasspath.get().forEach {
        if (it.absolutePath.contains("rhino")) {
            list.add(zipTree(it))
        }
    }

    from(list)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
