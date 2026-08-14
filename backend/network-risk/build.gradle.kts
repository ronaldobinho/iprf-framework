// Layer 3: Counterparty & Network Signals. Evaluates in-path using ONLY the
// pre-computed RiskStateStore, never a synchronous lookup against the primary
// database. Subject to the same ArchUnit guard as risk-engine.
//
// Populated in Session 2.1.
