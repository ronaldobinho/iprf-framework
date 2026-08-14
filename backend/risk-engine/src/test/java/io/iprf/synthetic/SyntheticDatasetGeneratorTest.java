package io.iprf.synthetic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.iprf.state.AccountProfile;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The generator's contract is reproducibility. A reported detection or
 * false-positive rate that cannot be regenerated from its seed is a claim, not a
 * measurement.
 */
class SyntheticDatasetGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");

    @Test
    @DisplayName("the same seed produces an identical dataset")
    void sameSeedIsReproducible() {
        List<AccountProfile> first = new SyntheticDatasetGenerator(42).generateProfiles(50, NOW);
        List<AccountProfile> second = new SyntheticDatasetGenerator(42).generateProfiles(50, NOW);

        assertThat(first).isEqualTo(second);

        List<LabelledTransaction> firstTx =
                new SyntheticDatasetGenerator(42).generateTransactions(first, 200, NOW);
        List<LabelledTransaction> secondTx =
                new SyntheticDatasetGenerator(42).generateTransactions(second, 200, NOW);

        assertThat(firstTx).isEqualTo(secondTx);
    }

    @Test
    @DisplayName("a different seed produces a different dataset")
    void differentSeedDiffers() {
        List<AccountProfile> a = new SyntheticDatasetGenerator(42).generateProfiles(50, NOW);
        List<AccountProfile> b = new SyntheticDatasetGenerator(43).generateProfiles(50, NOW);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("changing the transaction count does not reshuffle the profile population")
    void transactionCountIsIndependentOfProfiles() {
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator(42);
        List<AccountProfile> profiles = generator.generateProfiles(50, NOW);

        List<LabelledTransaction> hundred = generator.generateTransactions(profiles, 100, NOW);
        List<LabelledTransaction> twoHundred = generator.generateTransactions(profiles, 200, NOW);

        // The first hundred must be identical, so a larger run extends the
        // dataset rather than replacing it.
        assertThat(twoHundred.subList(0, 100)).isEqualTo(hundred);
    }

    @Test
    @DisplayName("both fraudulent and legitimate transactions are generated")
    void producesBothLabels() {
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator(42);
        List<AccountProfile> profiles = generator.generateProfiles(50, NOW);
        List<LabelledTransaction> dataset = generator.generateTransactions(profiles, 1_000, NOW);

        assertThat(dataset).anyMatch(LabelledTransaction::isFraudulent);
        assertThat(dataset).anyMatch(t -> !t.isFraudulent());
    }

    @Test
    @DisplayName("the hard scenarios are present, so the metrics are not self-flattering")
    void includesHardScenarios() {
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator(42);
        List<AccountProfile> profiles = generator.generateProfiles(50, NOW);
        List<LabelledTransaction> dataset = generator.generateTransactions(profiles, 1_000, NOW);

        // Without these two, the run would report a near-perfect detection rate
        // and a near-zero false-positive rate as artifacts of the generator.
        assertThat(dataset).anyMatch(t -> t.scenario() == Scenario.UNUSUAL_LEGITIMATE);
        assertThat(dataset).anyMatch(t -> t.scenario() == Scenario.FRAUD_SUBTLE);
    }

    @Test
    @DisplayName("every generated transaction is structurally valid")
    void transactionsAreValid() {
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator(7);
        List<AccountProfile> profiles = generator.generateProfiles(30, NOW);

        assertThat(generator.generateTransactions(profiles, 500, NOW))
                .allSatisfy(labelled -> {
                    assertThat(labelled.transaction().amount().signum()).isPositive();
                    assertThat(labelled.transaction().transactionId()).isNotBlank();
                    assertThat(labelled.transaction().payerAccountId()).isNotBlank();
                    assertThat(labelled.transaction().payeeAccountId()).isNotBlank();
                });
    }

    @Test
    @DisplayName("generating transactions without profiles fails loudly")
    void requiresProfiles() {
        assertThatThrownBy(() -> new SyntheticDatasetGenerator(1)
                .generateTransactions(List.of(), 10, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
