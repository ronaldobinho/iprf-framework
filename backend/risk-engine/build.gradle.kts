// Layers 1-2 of the control model: Identity & Account Posture, and Real-Time
// Behavioral Scoring. This module runs IN-PATH under a strict latency budget.
//
// Hard constraint (enforced by an ArchUnit test in Session 1.4): this module may
// not import JPA or repository types. It reads pre-computed state only. Adding a
// persistence dependency here is a build failure, not a review comment.
//
// Populated in Session 1.4.
