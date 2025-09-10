pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "SnowLib"

include("snowlib-core")
include("snowlib-kotlin")
