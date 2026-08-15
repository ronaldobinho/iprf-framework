package io.iprf.postsettlement;

import io.iprf.domain.SettledTransaction;
import io.iprf.state.RiskStateWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Layer 5 — runs the typology detectors over settled history and closes the
 * feedback loop by writing what they find into pre-computed risk state.
 *
 * <p><b>This class is the mechanism the whole framework is built around.</b> The
 * analysis here is cross-transactional and genuinely expensive; its output is a
 * single pre-computed value that Layer 3 reads in-path at the cost of one
 * lookup. Nothing is traded away except immediacy — and immediacy was never
 * available for this class of detection anyway, because the pattern did not
 * exist yet at the time of the first payment.
 *
 * <p>Runs asynchronously. Nothing on the payment path waits for it.
 */
@Component
public class PostSettlementAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PostSettlementAnalyzer.class);

    private final List<TypologyDetector> detectors;
    private final RiskStateWriter riskStateWriter;

    public PostSettlementAnalyzer(List<TypologyDetector> detectors, RiskStateWriter riskStateWriter) {
        this.detectors = List.copyOf(detectors);
        this.riskStateWriter = riskStateWriter;
    }

    /**
     * Analyses a window of settled transactions and updates risk state.
     *
     * @return every detection made, for reporting and event publication
     */
    public List<PatternDetection> analyse(List<SettledTransaction> window) {
        List<PatternDetection> all = new ArrayList<>();

        for (TypologyDetector detector : detectors) {
            List<PatternDetection> detections = detector.detect(window);
            for (PatternDetection detection : detections) {
                apply(detection);
                all.add(detection);
            }
            if (!detections.isEmpty()) {
                log.info("detector={} produced {} detection(s)", detector.name(), detections.size());
            }
        }

        return all;
    }

    private void apply(PatternDetection detection) {
        long version = riskStateWriter.raiseTier(
                detection.accountId(), detection.tier(), Set.of(detection.flag()));
        riskStateWriter.recordTypology(detection.accountId(), detection.typology());

        log.info("pattern detected accountId={} flag={} tier={} observations={} stateVersion={}",
                detection.accountId(), detection.flag(), detection.tier(),
                detection.observations(), version);
    }

    /** Detector names, for reporting which analyses ran. */
    public List<String> detectorNames() {
        return detectors.stream().map(TypologyDetector::name).toList();
    }
}
