rootProject.name = "iprf-backend"

dependencyResolutionManagement {
    // Modules must not declare their own repositories — resolution is centralized here.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

// The nine modules of the modular monolith, exactly as documented in CLAUDE.md.
// Modules for later phases are declared empty on purpose: the architecture is
// visible from day one and later phases add code inside these names rather than
// reshaping the build.
include(
    // Phase 1 — in-path decision engine
    "transaction-api",      // 1.3 HTTP boundary, validation, correlation ID, OpenAPI
    "risk-engine",          // 1.4 Layers 1-2, deterministic in-path rules
    // Phase 2 — Layers 3-5, events, audit
    "risk-state",           // 2.1 Redis-backed pre-computed RiskStateStore
    "network-risk",         // 2.1 Layer 3, counterparty & network signals
    "external-enrichment",  // 2.2 Layer 4, async enrichment
    "post-settlement",      // 2.3 Layer 5, pattern detection & feedback loop
    "audit",                // 2.4 immutable append-only decision trail
    // Phase 3 — assessment
    "assessment-engine",    // 3.x 12 categories x maturity 0-4, config-driven
    // Phase 6 — measurement
    "benchmarks",           // 6.1 JMH microbenchmarks and load harness
)
