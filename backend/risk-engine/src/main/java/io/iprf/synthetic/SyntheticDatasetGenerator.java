package io.iprf.synthetic;

import io.iprf.domain.Channel;
import io.iprf.domain.Rail;
import io.iprf.domain.Transaction;
import io.iprf.state.AccountProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates a reproducible synthetic dataset: payer profiles with baselines, and
 * labelled transactions across normal, unusual-legitimate and fraudulent
 * scenarios.
 *
 * <p><b>SYNTHETIC / DEMO DATA.</b> Nothing here models any real institution's
 * traffic. The fraud prevalence is set far above real-world rates so that a
 * thousand-transaction run yields statistically meaningful counts — actual
 * instant-payment fraud prevalence is orders of magnitude lower, and a realistic
 * rate would put roughly one fraudulent payment in a run of a thousand.
 *
 * <p>Fully deterministic: the same seed produces the same dataset, so a
 * reported detection or false-positive rate can be reproduced exactly by anyone
 * who runs the same command.
 */
public class SyntheticDatasetGenerator {

    /** Scenario mix. Chosen for demonstrability, not realism — see class javadoc. */
    private static final double SHARE_NORMAL = 0.80;
    private static final double SHARE_UNUSUAL_LEGITIMATE = 0.12;
    private static final double SHARE_FRAUD_OVERT = 0.03;
    private static final double SHARE_FRAUD_SUBTLE = 0.03;
    // remainder is FRAUD_TAKEOVER

    private final long seed;

    public SyntheticDatasetGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * Generates payer profiles.
     *
     * <p>Deliberately heterogeneous. A population of identical payers would make
     * baseline-relative rules look far better than they are, because every
     * threshold would be tuned to the single payer that exists.
     */
    public List<AccountProfile> generateProfiles(int count, Instant now) {
        Random random = new Random(seed);
        List<AccountProfile> profiles = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String accountId = "acct-%05d".formatted(i);

            // A minority are genuinely new accounts, which is where mule
            // activity concentrates and where deviation rules cannot help.
            boolean newAccount = random.nextDouble() < 0.10;
            int ageDays = newAccount ? random.nextInt(29) + 1 : 90 + random.nextInt(2_000);
            int transactionCount = newAccount ? random.nextInt(9) : 15 + random.nextInt(500);

            BigDecimal mean = BigDecimal.valueOf(40 + random.nextInt(1_200))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal stdDev = mean.multiply(BigDecimal.valueOf(0.20 + random.nextDouble() * 0.30))
                    .setScale(2, RoundingMode.HALF_UP);

            Set<String> devices = new LinkedHashSet<>();
            devices.add("dev-%05d-a".formatted(i));
            if (random.nextBoolean()) {
                devices.add("dev-%05d-b".formatted(i));
            }

            Set<Channel> channels = new LinkedHashSet<>();
            channels.add(Channel.MOBILE_APP);
            if (random.nextDouble() < 0.4) {
                channels.add(Channel.WEB);
            }

            Set<String> counterparties = new LinkedHashSet<>();
            int knownCount = newAccount ? random.nextInt(3) : 3 + random.nextInt(12);
            for (int c = 0; c < knownCount; c++) {
                counterparties.add("acct-cp-%05d-%02d".formatted(i, c));
            }

            int activeStart = 6 + random.nextInt(4);   // 06:00-09:00
            int activeEnd = 19 + random.nextInt(4);    // 19:00-22:00

            profiles.add(new AccountProfile(
                    accountId,
                    now.minus(Duration.ofDays(ageDays)),
                    !newAccount || random.nextBoolean(),
                    devices,
                    channels,
                    mean,
                    stdDev,
                    counterparties,
                    activeStart,
                    activeEnd,
                    transactionCount,
                    random.nextDouble() < 0.01,
                    now.minus(Duration.ofMinutes(random.nextInt(120))))); // fresh profiles
        }
        return profiles;
    }

    /** Generates labelled transactions against the supplied profiles. */
    public List<LabelledTransaction> generateTransactions(
            List<AccountProfile> profiles, int count, Instant now) {

        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("cannot generate transactions without profiles");
        }
        // Derived from the same seed but distinct, so changing the transaction
        // count does not reshuffle the profile population.
        Random random = new Random(seed * 31 + 7);
        List<LabelledTransaction> transactions = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            AccountProfile profile = profiles.get(random.nextInt(profiles.size()));
            Scenario scenario = pickScenario(random);
            transactions.add(new LabelledTransaction(
                    buildTransaction(profile, scenario, i, random, now), scenario));
        }
        return transactions;
    }

    private static Scenario pickScenario(Random random) {
        double roll = random.nextDouble();
        double cumulative = SHARE_NORMAL;
        if (roll < cumulative) {
            return Scenario.NORMAL;
        }
        cumulative += SHARE_UNUSUAL_LEGITIMATE;
        if (roll < cumulative) {
            return Scenario.UNUSUAL_LEGITIMATE;
        }
        cumulative += SHARE_FRAUD_OVERT;
        if (roll < cumulative) {
            return Scenario.FRAUD_OVERT;
        }
        cumulative += SHARE_FRAUD_SUBTLE;
        if (roll < cumulative) {
            return Scenario.FRAUD_SUBTLE;
        }
        return Scenario.FRAUD_TAKEOVER;
    }

    private Transaction buildTransaction(
            AccountProfile profile, Scenario scenario, int index, Random random, Instant now) {

        String knownCounterparty = profile.knownCounterparties().stream().findFirst()
                .orElse("acct-cp-fallback");
        String knownDevice = profile.knownDeviceIds().stream().findFirst().orElse(null);
        Channel typicalChannel = profile.typicalChannels().stream().findFirst()
                .orElse(Channel.MOBILE_APP);

        BigDecimal mean = profile.baselineAmountMean();
        BigDecimal stdDev = profile.baselineAmountStdDev();

        return switch (scenario) {
            case NORMAL -> tx(index, profile,
                    knownCounterparty,
                    aroundMean(mean, stdDev, 0.5, random),
                    typicalChannel,
                    knownDevice,
                    atHour(now, midActiveHour(profile), random));

            // Legitimate but unusual: a new payee and a large amount, the exact
            // shape of paying a deposit on a new flat.
            case UNUSUAL_LEGITIMATE -> tx(index, profile,
                    "acct-cp-new-%05d".formatted(index),
                    aroundMean(mean, stdDev, 2.2, random),
                    typicalChannel,
                    knownDevice,
                    atHour(now, midActiveHour(profile), random));

            case FRAUD_OVERT -> tx(index, profile,
                    "acct-mule-%05d".formatted(index % 40),   // reused destinations
                    aroundMean(mean, stdDev, 6.0, random),
                    Channel.WEB,
                    "dev-unknown-%05d".formatted(index),
                    atHour(now, 3, random));                   // outside every active window

            // Shaped to look ordinary: modest amount, plausible hour, known
            // device. Only the destination is wrong, and Layer 3 is what would
            // catch it — which is exactly the argument for Layer 3 existing.
            case FRAUD_SUBTLE -> tx(index, profile,
                    "acct-mule-%05d".formatted(index % 40),
                    aroundMean(mean, stdDev, 0.4, random),
                    typicalChannel,
                    knownDevice,
                    atHour(now, midActiveHour(profile), random));

            case FRAUD_TAKEOVER -> tx(index, profile,
                    "acct-mule-%05d".formatted(index % 40),
                    aroundMean(mean, stdDev, 3.5, random),
                    Channel.API,                                // channel the payer never uses
                    "dev-unknown-%05d".formatted(index),
                    atHour(now, 4, random));
        };
    }

    private static Transaction tx(
            int index, AccountProfile profile, String payee, BigDecimal amount,
            Channel channel, String deviceId, Instant initiatedAt) {

        return new Transaction(
                "txn-%06d".formatted(index),
                profile.accountId(),
                payee,
                amount,
                "USD",
                channel,
                deviceId,
                Rail.FEDNOW,
                initiatedAt);
    }

    /** An amount {@code sigmas} standard deviations from the payer's mean, with jitter. */
    private static BigDecimal aroundMean(
            BigDecimal mean, BigDecimal stdDev, double sigmas, Random random) {

        double jitter = (random.nextDouble() - 0.5) * 0.3;
        BigDecimal offset = stdDev.multiply(BigDecimal.valueOf(sigmas + jitter));
        BigDecimal amount = mean.add(offset).setScale(2, RoundingMode.HALF_UP);
        return amount.signum() > 0 ? amount : BigDecimal.valueOf(1.00);
    }

    private static int midActiveHour(AccountProfile profile) {
        int start = profile.activeHourStart();
        int end = profile.activeHourEnd();
        return start <= end ? (start + end) / 2 : start;
    }

    private static Instant atHour(Instant now, int hour, Random random) {
        return now.truncatedTo(ChronoUnit.DAYS)
                .plus(Duration.ofHours(hour))
                .plus(Duration.ofMinutes(random.nextInt(60)));
    }
}
