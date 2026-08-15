package io.iprf.postsettlement.detectors;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import io.iprf.domain.SettledTransaction;
import io.iprf.postsettlement.PatternDetection;
import io.iprf.postsettlement.PostSettlementProperties;
import io.iprf.postsettlement.TypologyDetector;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Detects threshold evasion: repeated payments sized just below a known
 * reporting or control threshold.
 *
 * <p>Structuring exists <em>because</em> controls have thresholds. Any absolute
 * limit an institution publishes, enforces consistently and never varies becomes
 * a number to sit underneath — which is the strongest argument in this framework
 * for baseline-relative rules over absolute ones, since a threshold derived from
 * the payer's own history is not knowable by the attacker.
 *
 * <p>This detector is the one that catches what remains: a sequence that is
 * individually unremarkable and collectively deliberate.
 */
@Component
public class StructuringDetector implements TypologyDetector {

    private final PostSettlementProperties properties;

    public StructuringDetector(PostSettlementProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "STRUCTURING";
    }

    @Override
    public List<PatternDetection> detect(List<SettledTransaction> window) {
        if (properties.structuringThresholds().isEmpty()) {
            return List.of();
        }

        Map<String, Integer> nearThresholdCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> matchedThreshold = new LinkedHashMap<>();

        for (SettledTransaction settled : window) {
            BigDecimal amount = settled.transaction().amount();
            for (BigDecimal threshold : properties.structuringThresholds()) {
                if (isJustBelow(amount, threshold)) {
                    String payer = settled.payerAccountId();
                    nearThresholdCounts.merge(payer, 1, Integer::sum);
                    matchedThreshold.putIfAbsent(payer, threshold);
                    break;
                }
            }
        }

        List<PatternDetection> detections = new ArrayList<>();
        int minRepeats = properties.structuringMinRepeats();

        nearThresholdCounts.forEach((payer, count) -> {
            if (count < minRepeats) {
                return;
            }
            detections.add(new PatternDetection(
                    payer,
                    NetworkFlag.STRUCTURING,
                    CounterpartyRiskTier.ELEVATED,
                    "THRESHOLD_STRUCTURING",
                    count + " payments sized just below " + matchedThreshold.get(payer)
                            + " within the " + properties.windowHours() + "-hour window "
                            + "(threshold " + minRepeats + " repeats)",
                    count));
        });

        return detections;
    }

    /**
     * Whether an amount sits in the band immediately below a threshold.
     *
     * <p>Strictly below: an amount exactly at the threshold is not evasion, it
     * is a payment that would have triggered the control.
     */
    private boolean isJustBelow(BigDecimal amount, BigDecimal threshold) {
        BigDecimal bandFloor = threshold.multiply(
                BigDecimal.valueOf(1.0 - properties.structuringBandPercent()));
        return amount.compareTo(bandFloor) >= 0 && amount.compareTo(threshold) < 0;
    }
}
