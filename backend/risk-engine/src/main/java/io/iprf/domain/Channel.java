package io.iprf.domain;

/** The channel a payment was initiated through. Each carries a different base rate. */
public enum Channel {
    MOBILE_APP,
    WEB,
    API,
    BRANCH,
    PHONE,
    UNKNOWN
}
