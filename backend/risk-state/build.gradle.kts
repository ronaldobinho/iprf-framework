// The RiskStateStore: pre-computed risk state backed by Redis, populated at
// startup and by async updaters — never written or queried on the transaction
// path. This module is what makes the in-path constraint achievable.
//
// Populated in Session 2.1.
