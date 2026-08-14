package io.iprf.domain;

import java.util.Objects;

/**
 * The configured definition of one rule.
 *
 * <p>Loaded from {@code application-rules.yml}, never hardcoded. Changing a
 * threshold or a weight is a risk decision with a response time measured in
 * hours; coupling it to a release cycle means an institution cannot respond to
 * an active attack faster than it can ship software.
 *
 * @param id          stable rule identifier, e.g. {@code L2.AMOUNT_DEVIATION}
 * @param layer       the layer this rule belongs to
 * @param reasonCode  the code emitted when it fires
 * @param version     this rule's version
 * @param weight      relative weight within its layer
 * @param enabled     whether the rule participates in evaluation
 * @param description what the rule detects, carried into generated documentation
 */
public record RuleDefinition(
        String id,
        ControlLayer layer,
        ReasonCode reasonCode,
        RuleVersion version,
        double weight,
        boolean enabled,
        String description) {

    public RuleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(reasonCode, "reasonCode");
        Objects.requireNonNull(version, "version");
        if (weight < 0.0) {
            throw new IllegalArgumentException("weight must not be negative, was " + weight);
        }
        if (reasonCode.layer() != null && reasonCode.layer() != layer) {
            throw new IllegalArgumentException(
                    "rule " + id + " is declared on " + layer
                            + " but emits " + reasonCode + ", which belongs to " + reasonCode.layer());
        }
    }
}
