package io.iprf.postsettlement;

import io.iprf.domain.SettledTransaction;
import java.util.List;

/**
 * One post-settlement pattern detector.
 *
 * <p>Detectors run asynchronously over settled history. They have no latency
 * budget and are free to be expensive — that freedom is the entire reason the
 * classification exists. What they must not do is produce a detection without
 * the evidence that justifies it.
 */
public interface TypologyDetector {

    /** A stable name, used in logs and in the evidence trail. */
    String name();

    /**
     * Analyses a window of settled transactions.
     *
     * @return detections, or an empty list. Never null, never partial results
     *         without evidence.
     */
    List<PatternDetection> detect(List<SettledTransaction> window);
}
