// Layer 4: External Enrichment. Async by default — consumes decision events and
// feeds FUTURE decisions by updating risk state. The evaluate endpoint never
// awaits this module, and a test asserts that by making the registry hang.
//
// This is the only module permitted to hold an outbound client, because it is
// the only one classified ASYNC that needs one. The ArchUnit guard forbids the
// in-path packages from doing the same.
dependencies {
    implementation(project(":risk-engine"))
    implementation("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
