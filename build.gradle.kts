import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("java") // java-library가 java를 포함하지만, 기존 구조 유지
    id("io.github.goooler.shadow") version "8.1.8"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("java-library")
    // id("maven-publish") // vanniktech가 내부에서 처리하므로 불필요, 충돌 소지 줄이기 위해 제거
    id("signing")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.snow1026"
version = "1.0.0-SNAPSHOT"

val pluginVersion = version.toString()

val nmsProjects by lazy {
    subprojects.filter {
        it.path.startsWith(":mappings:") &&
                it.parent?.name == "mappings" &&
                it.name != "build" &&
                it.name.isNotEmpty()
    }
}

allprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        // ❌ 중복 원인 제거: withSourcesJar(), withJavadocJar() 삭제
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    // Javadoc 태스크 옵션만 조정(아티팩트 추가는 플러그인이 처리)
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
        // NMS 모듈 리오브프에 의존
        nmsProjects.forEach { dependsOn("${it.path}:reobfJar") }

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

    nmsProjects.forEach { implementation(project(it.path)) }
}

nmsProjects.forEach { nmsProject ->
    project(nmsProject.path) {
        dependencies {
            implementation(project(":snowlib-core"))
        }
    }
}

// ❌ 중복 및 충돌 소지 제거: 별도 Javadoc 태스크/아티팩트 연결 제거
// tasks.withType<Javadoc> { ... } 는 위에서 이미 통합했고,
// afterEvaluate { generateMetadataFileForMavenPublication dependsOn plainJavadocJar } 도 제거
// (vanniktech 플러그인이 알아서 퍼블리싱 그래프 구성)

mavenPublishing {
    // Maven Central 타깃
    publishToMavenCentral()
    // 모든 퍼블리케이션 서명
    signAllPublications()
    // GAV
    coordinates("io.github.snow1026", "SnowLib", version.toString())

    // ✅ 무엇을 퍼블리시할지 플러그인 DSL로만 제어 (중복 방지)
    //    - 표준 Javadoc 포함
    //    - Sources JAR 포함
    configure(JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = true
    ))

    pom {
        name.set("SnowLib")
        description.set("SnowLib.")
        url.set("https://github.com/snow1026/SnowLib")

        licenses {
            license {
                name.set("GPL-3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
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
    // 로컬 GPG 사용 (환경에 맞게 이미 설정하신 그대로 유지)
    useGpgCmd()
}
