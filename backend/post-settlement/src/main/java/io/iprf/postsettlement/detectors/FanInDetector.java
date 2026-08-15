package io.iprf.postsettlement.detectors;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import io.iprf.domain.SettledTransaction;
import io.iprf.postsettlement.PatternDetection;
import io.iprf.postsettlement.PostSettlementProperties;
import io.iprf.postsettlement.TypologyDetector;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Detects mule collection: many unrelated payers sending to one receiver in a
 * short window.
 *
 * <p>This is the framework's central example of a pattern that is invisible
 * in-path. Each individual payment is unremarkable — correctly authorized, from
 * a genuine customer, to a plausible destination. The pattern exists only across
 * the set, and only after the payments have settled.
 *
 * <p>Under the UK reimbursement regime in force since 7 October 2024, the cost
 * of an APP scam is split equally between the sending and receiving firms, so a
 * receiving institution has a direct financial interest in finding these before
 * the next victim pays.
 */
@Component
public class FanInDetector implements TypologyDetector {

    private final PostSettlementProperties properties;

    public FanInDetector(PostSettlementProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "FAN_IN";
    }

    @Override
    public List<PatternDetection> detect(List<SettledTransaction> window) {
        Map<String, Set<String>> payersByReceiver = new LinkedHashMap<>();

        for (SettledTransaction settled : window) {
            // A payer sending to their own account is not a fan-in contributor.
            if (settled.transaction().isSelfTransfer()) {
                continue;
            }
            payersByReceiver
                    .computeIfAbsent(settled.payeeAccountId(), k -> new LinkedHashSet<>())
                    .add(settled.payerAccountId());
        }

        List<PatternDetection> detections = new ArrayList<>();
        int threshold = properties.fanInDistinctPayers();

        payersByReceiver.forEach((receiver, payers) -> {
            if (payers.size() < threshold) {
                return;
            }
            detections.add(new PatternDetection(
                    receiver,
                    NetworkFlag.FAN_IN,
                    // ELEVATED rather than CONFIRMED: a fan-in pattern is strong
                    // evidence, not proof. A payroll bureau or a crowdfunding
                    // account looks identical from this angle, and the tier this
                    // sets will be charged against every later payment to the
                    // account.
                    tierFor(payers.size(), threshold),
                    "MULE_COLLECTION_FAN_IN",
                    payers.size() + " distinct payers sent to this account within the "
                            + properties.windowHours() + "-hour window (threshold "
                            + threshold + ")",
                    payers.size()));
        });

        return detections;
    }

    /**
     * Well past the threshold is a stronger signal than just over it.
     *
     * <p>Ramping the tier rather than assigning one fixed level is what keeps a
     * payroll account that marginally trips the threshold from being treated
     * like an account collecting from fifty unrelated strangers.
     */
    private static CounterpartyRiskTier tierFor(int distinctPayers, int threshold) {
        return distinctPayers >= threshold * 3
                ? CounterpartyRiskTier.HIGH
                : CounterpartyRiskTier.ELEVATED;
    }
}
