# SnowLib

**SnowLib**는 Minecraft Paper/Bukkit 플러그인 개발을 돕는 경량 유틸리티 라이브러리입니다.
명령어 등록, NMS 버전 처리, 데이터 직렬화/저장소, Reflection 헬퍼 등 자주 사용하는 기능들을 제공합니다.

---

## ✨ 주요 기능

### 📌 명령어 시스템 (`Sommand` & `KotlinSommand`)

* `CommandMap`에 자동 등록되는 명령어 빌더.
* **트리 구조** 기반의 `CommandNode` 시스템.
* 인자 파싱(`ArgumentParser`) 및 자동 탭 완성 지원.
* **권한**, **설명**, **사용법**, **별칭** 설정 가능.
* Kotlin DSL (`KotlinSommand`) 제공 → 람다 기반 선언형 명령어 등록.

---

## 📝 명령어 사용 예시

### `/team <create|delete|list>` 구조 예시

```java
sommand.register("team", root -> {
    root.literal("create", create -> {
        create.arg(String.class, arg -> {
            arg.exec((ctx, args) -> {
                String name = ctx.arg(0);
                ctx.sender().sendMessage("§aTeam '" + name + "' created!");
            });
        });
    });

    root.literal("delete", del -> {
        del.arg(String.class, arg -> {
            arg.exec((ctx, args) -> {
                String name = ctx.arg(0);
                ctx.sender().sendMessage("§cTeam '" + name + "' deleted!");
            });
        });
    });

    root.literal("list", list -> {
        list.exec((ctx, args) -> {
            ctx.sender().sendMessage("§eTeams: Alpha, Beta, Gamma");
        });
    });
});
```

### Kotlin DSL 버전

```kotlin
commands.register("team") {
    literal("create") {
        arg(String::class) {
            exec { ctx, _ ->
                val name = ctx.arg<String>(0)
                ctx.sender().sendMessage("§aTeam '$name' created!")
            }
        }
    }

    literal("delete") {
        arg(String::class) {
            exec { ctx, _ ->
                val name = ctx.arg<String>(0)
                ctx.sender().sendMessage("§cTeam '$name' deleted!")
            }
        }
    }

    literal("list") {
        exec { ctx, _ ->
            ctx.sender().sendMessage("§eTeams: Alpha, Beta, Gamma")
        }
    }
}
```

---

## 📌 Store (JSON Key-Value 저장소)

* 플러그인 데이터 폴더에 JSON 파일로 자동 저장.
* 객체 단위로 **저장 / 불러오기 / 수정 / 삭제 / 키 목록 조회 / 전체 삭제** 지원.
* Bukkit `ConfigurationSerializable` 객체도 직렬화 가능.

---

## 📌 NMS 버전 관리 (`VersionUtil`)

* `Bukkit.getBukkitVersion()`을 기반으로 서버 버전 감지.
* 1.17 \~ 1.21.8까지 지원.
* Enum 기반 `MappingsVersion`으로 안전한 버전 분기 처리.

---

## 📌 Reflection 헬퍼 (`ReflectFinder`)

* 클래스 동적 로드 (`findClass`).
* 접근 제한 무시하고 생성자 로드 (`findConstructor`).

---

## 📦 Gradle 빌드 설정

### 빌드 요구사항

* **Java 21**
* Gradle 8 이상
* PaperMC API 저장소

### 빌드 예시

```bash
./gradlew build
```

빌드 결과물은 `build/libs/SnowLib-<버전>.jar` 로 생성됩니다.

## 🚀 모듈 구조

* `snowlib-core` → 핵심 기능 (명령어, Store, Util 등)
* `snowlib-kotlin` → Kotlin 지원 모듈
* `mappings/*` → NMS 버전별 모듈

---

**사용 예시:**

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.snow1026:SnowLib:${Version}")
}
```

---

## 📖 라이선스

이 프로젝트는 [GPL-3.0 라이선스](https://www.gnu.org/licenses/gpl-3.0.txt)를 따릅니다.

---

## 👤 기여자

* **snow1026** ([GitHub](https://github.com/snow1026))
