pluginManagement {
    repositories {
        maven("https://maven.minecraftforge.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.parchmentmc.org")
        mavenCentral()
        gradlePluginPortal()
    }
}

val minecraft_version: String by settings

rootProject.name = "beddium-${minecraft_version}"
