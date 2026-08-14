package io.iprf.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The version of a single rule.
 *
 * <p>Rules are versioned independently of the framework because they change
 * independently: a threshold adjustment is a rule change, not a release. Audit
 * records persist the version of every rule that executed, so a decision made
 * under an older threshold can still be explained after the threshold moves.
 *
 * @param value semantic version, e.g. {@code 1.0.0}
 */
public record RuleVersion(String value) implements Comparable<RuleVersion> {

    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    public RuleVersion {
        Objects.requireNonNull(value, "value");
        if (!SEMVER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "rule version must be MAJOR.MINOR.PATCH, was '" + value + "'");
        }
    }

    public static RuleVersion of(String value) {
        return new RuleVersion(value);
    }

    public static final RuleVersion INITIAL = new RuleVersion("1.0.0");

    @Override
    public int compareTo(RuleVersion other) {
        String[] a = value.split("\\.");
        String[] b = other.value.split("\\.");
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return value;
    }
}
