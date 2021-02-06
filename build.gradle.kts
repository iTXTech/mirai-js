plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
    id("com.jfrog.bintray") version "1.8.5"
    `maven-publish`
    id("net.mamoe.mirai-console") version "2.3.2"
    id("net.mamoe.kotlin-jvm-blocking-bridge") version "1.8.0"
}

group = "org.itxtech"
version = "2.0-M1"
description = "强大的 Mirai JavaScript 插件运行时"

val vcs = "https://github.com/iTXTech/mirai-js"

kotlin {
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("InlineClasses")
            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }
    }
}

repositories {
    maven("https://dl.bintray.com/him188moe/mirai")
    maven("https://dl.bintray.com/mamoe/kotlin-jvm-blocking-bridge")
    maven("https://maven.aliyun.com/repository/public")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.mozilla:rhino:1.7.13")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Name"] = "iTXTech MiraiJs"
        attributes["Revision"] = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            .inputStream.bufferedReader().readText().trim()
    }

    val list = ArrayList<Any>()

    configurations.compileClasspath.get().copyRecursive().forEach { file ->
        arrayOf("rhino", "squareup").forEach {
            if (file.absolutePath.contains(it)) {
                list.add(zipTree(file))
            }
        }
    }

    from(list)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

bintray {
    user = findProperty("buser") as String?
    key = findProperty("bkey") as String?
    setPublications("mavenJava")
    setConfigurations("archives")
    pkg.apply {
        repo = "mirai"
        name = "mirai-js"
        userOrg = "itxtech"
        setLicenses("AGPL-V3")
        publicDownloadNumbers = true
        vcsUrl = vcs
    }
}

@Suppress("DEPRECATION")
val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])

            groupId = rootProject.group.toString()
            this.artifactId = artifactId
            version = version

            pom.withXml {
                val root = asNode()
                root.appendNode("description", description)
                root.appendNode("name", project.name)
                root.appendNode("url", vcs)
                root.children().last()
            }
            artifact(sourcesJar.get())
        }
    }
}
