plugins {
    kotlin("jvm") version "2.0.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.cludivers"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // https://mvnrepository.com/artifact/com.sk89q.worldedit/worldedit-bukkit
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.16")
    // https://mvnrepository.com/artifact/com.fastasyncworldedit/FastAsyncWorldEdit-Bukkit
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.13.2")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    runServer {
        downloadPlugins {
//            url("https://dev.bukkit.org/projects/worldedit/files/6786280/download")
            url("https://ci.athion.net/job/FastAsyncWorldEdit/1174/artifact/artifacts/FastAsyncWorldEdit-Paper-2.13.3-SNAPSHOT-1174.jar")
        }
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.8")
    }
}



val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
