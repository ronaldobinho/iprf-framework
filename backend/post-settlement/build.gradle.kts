// Layer 5: Post-Settlement Analysis. Consumes settlement events and detects
// typologies (fan-in mule patterns, fan-out, structuring), then closes the
// feedback loop by updating counterparty risk state.
//
// ASYNC by classification: no latency budget, and nothing on the payment path
// waits for it. It is therefore free to hold write access to risk state, which
// the in-path modules deliberately cannot — see the ArchUnit guard.
dependencies {
    implementation(project(":risk-engine"))
    implementation("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
