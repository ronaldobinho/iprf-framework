plugins {
    alias(libs.plugins.spring.boot)
}

// The only bootable module. It is the HTTP boundary of the modular monolith:
// request validation, correlation ID propagation, structured logging, and the
// decision pipeline entry point. It must never contain rule logic — that lives
// in risk-engine (Session 1.4).
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
