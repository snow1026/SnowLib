pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "SnowLib"

include("mappings:v1_17_R1")
include("mappings:v1_18_R1")
include("mappings:v1_18_R2")
include("mappings:v1_19_R1")
include("mappings:v1_19_R2")
include("mappings:v1_19_R3")
include("mappings:v1_20_R1")
include("mappings:v1_20_R2")
include("mappings:v1_20_R3")
include("mappings:v1_20_R4")
include("mappings:v1_21_R1")
include("mappings:v1_21_R2")
include("mappings:v1_21_R3")
include("mappings:v1_21_R4")
include("mappings:v1_21_R5")
include("snowlib-core")
include("snowlib-kotlin")
