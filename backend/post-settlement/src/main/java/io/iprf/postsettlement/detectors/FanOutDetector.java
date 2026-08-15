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
 * Detects dispersal: one account paying many distinct receivers in a short
 * window.
 *
 * <p>Two different things produce this shape — a compromised account being
 * emptied, and a mule distributing collected proceeds onward. Both are worth
 * flagging, and the detector deliberately does not try to distinguish them: the
 * distinction depends on context this layer does not have, and guessing would
 * put an unfounded label in the evidence trail.
 */
@Component
public class FanOutDetector implements TypologyDetector {

    private final PostSettlementProperties properties;

    public FanOutDetector(PostSettlementProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "FAN_OUT";
    }

    @Override
    public List<PatternDetection> detect(List<SettledTransaction> window) {
        Map<String, Set<String>> receiversByPayer = new LinkedHashMap<>();

        for (SettledTransaction settled : window) {
            if (settled.transaction().isSelfTransfer()) {
                continue;
            }
            receiversByPayer
                    .computeIfAbsent(settled.payerAccountId(), k -> new LinkedHashSet<>())
                    .add(settled.payeeAccountId());
        }

        List<PatternDetection> detections = new ArrayList<>();
        int threshold = properties.fanOutDistinctReceivers();

        receiversByPayer.forEach((payer, receivers) -> {
            if (receivers.size() < threshold) {
                return;
            }
            detections.add(new PatternDetection(
                    payer,
                    NetworkFlag.FAN_OUT,
                    CounterpartyRiskTier.ELEVATED,
                    "DISPERSAL_FAN_OUT",
                    receivers.size() + " distinct receivers paid from this account within the "
                            + properties.windowHours() + "-hour window (threshold "
                            + threshold + ")",
                    receivers.size()));
        });

        return detections;
    }
}
