// Layers 1-2 of the control model: Identity & Account Posture, and Real-Time
// Behavioral Scoring. This module runs IN-PATH under a strict latency budget.
//
// It also holds the shared domain types (io.iprf.domain). The brief's module
// layout has no separate core-domain module, and the decision domain is this
// module's subject matter — transaction-api and audit depend on it rather than
// the reverse.
//
// Hard constraint, enforced by ArchUnit rather than by review: this module may
// not import JPA or repository types. It reads pre-computed state only. The
// dependencies below are deliberately minimal — spring-boot-starter brings
// context and logging, and nothing that can reach a database.
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(libs.archunit.junit5)
}
