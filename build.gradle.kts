plugins {
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.djtools.ayan"
version = "0.1.0"

subprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://mvn.0110.be/releases") }
        maven { url = uri("https://repo.spring.io/milestone") }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
