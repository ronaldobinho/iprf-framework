package io.iprf.postsettlement;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import java.util.Objects;

/**
 * A typology detected against an account by post-settlement analysis.
 *
 * <p>Carries the evidence that justified it, not just the verdict. A detection
 * that raises a counterparty's tier without supporting evidence degrades every
 * future decision about that account, and there would be no way to tell a real
 * finding from a detector misfiring.
 *
 * @param accountId   the account the pattern was detected against
 * @param flag        the behavioural pattern
 * @param tier        the tier this detection justifies
 * @param typology    human-readable typology label
 * @param evidence    what was observed
 * @param observations the count that triggered it, for the evidence trail
 */
public record PatternDetection(
        String accountId,
        NetworkFlag flag,
        CounterpartyRiskTier tier,
        String typology,
        String evidence,
        int observations) {

    public PatternDetection {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(flag, "flag");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(evidence, "evidence");
    }
}
