package io.iprf.postsettlement;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Layer 5 detector configuration.
 *
 * <p>Detector sensitivity is a direct input to the false-positive rate of
 * <em>future</em> decisions: a detector that fires too readily raises
 * counterparty tiers that Layer 3 then charges against legitimate payments. That
 * is why these thresholds are configuration with measurable effect rather than
 * constants.
 *
 * @param windowHours              aggregation window for all detectors
 * @param fanInDistinctPayers      distinct payers to one receiver before FAN_IN fires
 * @param fanOutDistinctReceivers  distinct new receivers from one payer before FAN_OUT fires
 * @param structuringMinRepeats    repeated near-threshold amounts before STRUCTURING fires
 * @param structuringThresholds    the thresholds attackers are assumed to be sitting under
 * @param structuringBandPercent   how far below a threshold still counts as "just below"
 */
@ConfigurationProperties(prefix = "iprf.post-settlement")
public record PostSettlementProperties(
        long windowHours,
        int fanInDistinctPayers,
        int fanOutDistinctReceivers,
        int structuringMinRepeats,
        List<BigDecimal> structuringThresholds,
        double structuringBandPercent) {

    public PostSettlementProperties {
        if (windowHours <= 0) {
            throw new IllegalStateException("post-settlement.window-hours must be positive");
        }
        if (fanInDistinctPayers < 2) {
            throw new IllegalStateException(
                    "post-settlement.fan-in-distinct-payers must be at least 2 — a single payer "
                            + "is not a fan-in pattern, it is a payment");
        }
        if (fanOutDistinctReceivers < 2) {
            throw new IllegalStateException(
                    "post-settlement.fan-out-distinct-receivers must be at least 2");
        }
        if (structuringMinRepeats < 2) {
            throw new IllegalStateException(
                    "post-settlement.structuring-min-repeats must be at least 2");
        }
        structuringThresholds = structuringThresholds == null
                ? List.of() : List.copyOf(structuringThresholds);
        if (structuringBandPercent <= 0.0 || structuringBandPercent >= 1.0) {
            throw new IllegalStateException(
                    "post-settlement.structuring-band-percent must be within (0, 1)");
        }
    }
}
