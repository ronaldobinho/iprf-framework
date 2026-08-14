package io.iprf.domain;

/**
 * The instant-payment rail a transaction is being settled on.
 *
 * <p>Recorded because settlement envelopes and regulatory obligations differ:
 * Pix imposes a 40-second end-to-end limit on the SPI primary channel, while
 * FedNow publishes no equivalent numeric envelope. See
 * {@code docs/framework/latency-model.md}.
 */
public enum Rail {
    FEDNOW,
    PIX,
    FASTER_PAYMENTS,
    OTHER
}
