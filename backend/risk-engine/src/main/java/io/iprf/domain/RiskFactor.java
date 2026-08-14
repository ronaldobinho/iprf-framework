package io.iprf.domain;

import java.util.Objects;

/**
 * A single rule's contribution to the composite risk score.
 *
 * <p>This is what makes a score decomposable. A composite of 0.71 is never
 * reported without the factors that produced it — see
 * {@code docs/framework/methodology.md} on the refusal to ship black-box
 * scoring.
 *
 * @param code         stable reason code
 * @param layer        the layer that produced it
 * @param contribution this factor's contribution to the composite, in {@code [0, 1]}
 * @param ruleId       identifier of the rule that fired
 * @param ruleVersion  version of that rule, persisted so the decision can be reproduced
 * @param explanation  human-readable explanation, specific to this evaluation
 */
public record RiskFactor(
        ReasonCode code,
        ControlLayer layer,
        double contribution,
        String ruleId,
        String ruleVersion,
        String explanation) {

    public RiskFactor {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(explanation, "explanation");
        if (contribution < 0.0 || contribution > 1.0) {
            throw new IllegalArgumentException(
                    "contribution must be within [0, 1], was " + contribution);
        }
    }

    /** Convenience factory for a degradation factor, which contributes nothing. */
    public static RiskFactor degraded(ReasonCode code, ControlLayer layer) {
        return new RiskFactor(code, layer, 0.0, "degradation", "n/a", code.explanation());
    }
}
