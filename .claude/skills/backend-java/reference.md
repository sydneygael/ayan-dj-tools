# Backend Java — Reference Rapide

## Java 25 Features Utilisees

| Feature | Usage dans le projet |
|---------|---------------------|
| Records | DTOs, VOs, responses, config properties |
| Sealed interfaces | (futur) hierarchies de types domaine |
| Pattern matching (`instanceof`) | Simplification casts dans adapters |
| Text blocks `"""` | SQL queries, prompts IA |
| Virtual threads | (futur) Spring Boot 4 par defaut |
| `var` | Variables locales au type evident |

## Spring Boot 4.0.2 — Breaking Changes vs 3.x

| Changement | Ancien (3.x) | Nouveau (4.x) |
|------------|--------------|----------------|
| Jackson | `com.fasterxml.jackson` | `tools.jackson.databind` (Jackson 3) |
| Redis autoconfig | `org.springframework.boot.autoconfigure.data.redis` | `org.springframework.boot.data.redis.autoconfigure` |
| WebMvc autoconfig | `org.springframework.boot.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.autoconfigure` |
| WebMvc test | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| Serializer Redis | `GenericJackson2JsonRedisSerializer` | `GenericJacksonJsonRedisSerializer` (Jackson 3) |

## Gradle 9.2 Kotlin DSL — Cheat Sheet

```kotlin
// Root build.gradle.kts — subprojects config
subprojects {
    configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
    }
    tasks.withType<Test> { useJUnitPlatform() }
}

// Module domain (java-library, zero Spring)
plugins { `java-library` }
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

// Module infra (Spring Boot)
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}
dependencies {
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

## Annotations Frequentes

| Annotation | Usage |
|-----------|-------|
| `@ConfigurationProperties(prefix = "dj-tagger")` | Record config (nested records) |
| `@RestController` + `@RequestMapping` | REST endpoints |
| `@Valid` + `@RequestBody` | Validation entree |
| `@Value("${...}")` | Injection propriete simple |
| `@ExtendWith(MockitoExtension.class)` | Tests unitaires |
| `@SpringBootTest` + `@AutoConfigureMockMvc` | Tests integration |
| `@RestControllerAdvice` + `@ExceptionHandler` | Gestion erreurs globale |

## Config Keys (`dj-tagger.*`)

```yaml
dj-tagger:
  audio:
    supported-extensions: mp3,flac,wav,aiff,m4a,ogg
    max-file-size-mb: 100
  agent:
    default-mode: PLAN
    batch-size: 10
    confidence-threshold: 0.7
  rag:
    similarity-threshold: 0.7
    max-similar-tracks: 5
    embedding-dimension: 768
```

## Gotchas

- Domain module : pas de BOM Spring → versions explicites pour deps test
- `application.yml` dans `infra/src/test/resources/` obligatoire pour ITs (placeholders `audio.*`, `spotify.*`)
- IT context : `@SpringBootTest(classes = {...})` focuse, PAS full app context
- Testcontainers Redis : `new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)`
- MockWebServer : `com.squareup.okhttp3:mockwebserver:4.12.0` pour tests HTTP
