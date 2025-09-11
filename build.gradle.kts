import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("io.github.goooler.shadow") version "8.1.8"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("java-library")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.snow1026"
version = "1.0.0"

val pluginVersion = version.toString()

allprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            addBooleanOption("Xdoclint:none", true)
            addStringOption("charset", "UTF-8")
            encoding = "UTF-8"
        }
        isFailOnError = false
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("SnowLib-$pluginVersion.jar")
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
        relocate("org.bstats", "io.snow1026.shadowed.bstats")
    }

    build {
        dependsOn(shadowJar)
        finalizedBy("copyJarToServer")
    }

    compileJava.get().dependsOn(clean)
}

val serverPluginsDir = file("C:/Users/user/Desktop/._LumPq_/DarkForest/.server/plugins")
tasks.register<Copy>("copyJarToServer") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)
    into(serverPluginsDir)
    rename { "SnowLib-$pluginVersion.jar" }
}

dependencies {
    implementation(project(":snowlib-core"))
    implementation(project(":snowlib-kotlin"))
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.snow1026", "SnowLib", version.toString())
    configure(JavaLibrary( javadocJar = JavadocJar.Javadoc(), sourcesJar = true ))

    pom {
        name.set("SnowLib")
        description.set("snowlib")
        url.set("https://github.com/snow1026/SnowLib")

        licenses {
            license {
                name.set("GNU General Public License version 3")
                url.set("https://opensource.org/licenses/GPL-3.0")
            }
        }

        developers {
            developer {
                id.set("snow1026")
                name.set("snow1026")
            }
        }

        scm {
            url.set("https://github.com/snow1026/SnowLib")
            connection.set("scm:git:https://github.com/snow1026/SnowLib.git")
        }
    }
}

signing {
    useGpgCmd()
}
