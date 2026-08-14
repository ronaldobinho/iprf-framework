// Immutable, append-only decision trail: transaction ID, framework version,
// rules executed with their versions, risk factors, decision, timestamp,
// latency, risk-state versions read, correlation ID.
//
// Append-only is enforced in the schema migration (no UPDATE/DELETE grants),
// not by convention.
//
// Populated in Session 2.4.
