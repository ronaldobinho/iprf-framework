plugins {
    // Declared here so the plugin is on the build classpath for every module,
    // applied only by the module that is actually bootable (transaction-api).
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "io.iprf"
    version = "0.1.0-SNAPSHOT"
}

// Read from the version catalog at root scope and captured into locals: inside
// `subprojects { }` the receiver is the subproject, which has no `libs` accessor.
val javaLanguageVersion = libs.versions.java.get().toInt()
val springBootBom = libs.spring.boot.bom

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            // Pinned rather than inherited from the ambient JDK: CI and every
            // developer machine must compile against the same language level.
            languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
        }
    }

    dependencies {
        // Gradle's native platform support instead of the legacy Spring
        // dependency-management plugin. Starters are declared without versions;
        // the BOM resolves them consistently across all nine modules.
        add("implementation", platform(springBootBom))
        // annotationProcessor does not extend implementation, so it needs the
        // platform declared separately or processors resolve without a version.
        add("annotationProcessor", platform(springBootBom))
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        // Gradle 9 no longer puts the JUnit Platform launcher on the test runtime
        // classpath implicitly. The Spring Boot plugin adds it, but it is applied
        // only to transaction-api, so every other module needs it declared.
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Keeps parameter names in the bytecode — Bean Validation messages and
        // audit records reference them by name.
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
