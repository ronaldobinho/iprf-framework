plugins {
    alias(libs.plugins.spring.boot)
}

// The only bootable module. It is the HTTP boundary of the modular monolith:
// request validation, correlation ID propagation, structured logging, and the
// decision pipeline entry point. It must never contain rule logic — that lives
// in risk-engine.
dependencies {
    implementation(project(":risk-engine"))
    implementation(project(":network-risk"))
    implementation(project(":post-settlement"))
    implementation(project(":external-enrichment"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.webmvc)

    // The architecture guard lives here rather than in risk-engine because this
    // is the only module whose classpath contains every other one — ArchUnit can
    // only enforce a rule about classes it can actually see, and a rule scanning
    // an absent package passes vacuously.
    testImplementation(libs.archunit.junit5)
}

// Evaluates a seeded synthetic dataset and prints the decision distribution,
// detection and false-positive rates, and latency percentiles.
//
//     ./gradlew runScenario
//
// Deterministic: the seed lives in application.yml, so the numbers are
// reproducible by anyone who runs the same command.
tasks.register<JavaExec>("runScenario") {
    group = "verification"
    description = "Evaluates a synthetic dataset and prints the decision distribution and metrics"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.iprf.transaction.TransactionApiApplication")
    args(
        "--iprf.scenario.enabled=true",
        // A batch job, not a service: no HTTP listener, and per-decision logging
        // suppressed so the report is the output rather than buried in it.
        "--spring.main.web-application-type=none",
        "--spring.main.banner-mode=off",
        "--logging.level.root=WARN",
        "--logging.level.io.iprf=WARN",
    )
}
