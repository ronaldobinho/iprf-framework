/**
 * The demo dataset.
 *
 * SYNTHETIC. These profiles and counterparties describe nobody. They exist so a
 * visitor can watch the engine produce three differentiated, fully explained
 * outcomes without any backend.
 *
 * The third scenario is the important one: its counterparty carries risk state
 * of exactly the shape Layer 5 writes after detecting a fan-in pattern. That is
 * the framework thesis made visible in a single click, rather than argued.
 */
import { dec } from "./decimal";
import type {
  AccountProfile,
  Channel,
  CounterpartyRiskState,
  Transaction,
} from "./types";

const DAY = 86_400_000;
const MINUTE = 60_000;

export const DEMO_PAYER = "acct-00417";

export function demoProfiles(now: number): AccountProfile[] {
  return [
    {
      accountId: DEMO_PAYER,
      openedAt: now - 640 * DAY,
      verified: true,
      knownDeviceIds: ["dev-8f21c4"],
      typicalChannels: ["MOBILE_APP", "WEB"],
      // Threshold for the amount rule is mean + 3 sigma = 420.00
      baselineAmountMean: dec("240.00"),
      baselineAmountStdDev: dec("60.00"),
      knownCounterparties: ["acct-utility-8842", "acct-landlord-2201", "acct-grocer-0771"],
      activeHourStart: 7,
      activeHourEnd: 22,
      transactionCount: 312,
      restricted: false,
      computedAt: now - 5 * MINUTE,
    },
  ];
}

/**
 * Counterparty risk state, as Layer 5 would have written it.
 *
 * Nothing here was configured by hand to make the demo work: this is the same
 * record shape the fan-in detector emits when it observes many unrelated payers
 * settling into one account inside the aggregation window.
 */
export function demoRiskState(now: number): CounterpartyRiskState[] {
  return [
    {
      counterpartyId: "acct-9931-collector",
      tier: "ELEVATED",
      flags: ["FAN_IN"],
      reportedTypologies: ["MULE_COLLECTION_FAN_IN"],
      distinctPayers: 7,
      version: 4,
      computedAt: now - 90 * MINUTE,
    },
  ];
}

export interface Preset {
  id: string;
  label: string;
  /** What a reader should take away, not what the code does. */
  note: string;
  expected: "ALLOW" | "REVIEW" | "DECLINE";
  build(now: number): Transaction;
}

function at(now: number, hourUtc: number): number {
  const day = new Date(now);
  day.setUTCHours(hourUtc, 17, 0, 0);
  return day.getTime();
}

const base = (now: number) => ({
  currency: "USD",
  rail: "FEDNOW" as const,
  payerAccountId: DEMO_PAYER,
  initiatedAt: at(now, 14),
});

export const PRESETS: Preset[] = [
  {
    id: "ordinary",
    label: "Ordinary payment",
    note: "Known device, a counterparty this payer has used before, inside their usual hours. Nothing fires.",
    expected: "ALLOW",
    build: (now) => ({
      ...base(now),
      transactionId: "txn-demo-ordinary",
      payeeAccountId: "acct-utility-8842",
      amount: dec("184.20"),
      channel: "MOBILE_APP" as Channel,
      deviceId: "dev-8f21c4",
    }),
  },
  {
    id: "uncertain",
    label: "Unfamiliar, but plausible",
    note: "A new counterparty, an unrecognised device, the middle of the night. Individually weak signals that together warrant a look, not a rejection.",
    expected: "REVIEW",
    build: (now) => ({
      ...base(now),
      transactionId: "txn-demo-uncertain",
      payeeAccountId: "acct-newpayee-4410",
      amount: dec("210.00"),
      channel: "MOBILE_APP" as Channel,
      deviceId: "dev-unrecognised",
      initiatedAt: at(now, 3),
    }),
  },
  {
    id: "collector",
    label: "Payment into a known collector",
    note: "The same weak signals, plus a destination that Layer 5 already flagged as a fan-in collection account after watching earlier settlements. Layer 3 reads that in-path, at the cost of one lookup.",
    expected: "DECLINE",
    build: (now) => ({
      ...base(now),
      transactionId: "txn-demo-collector",
      payeeAccountId: "acct-9931-collector",
      amount: dec("2480.00"),
      channel: "API" as Channel,
      deviceId: "dev-unrecognised",
      initiatedAt: at(now, 3),
    }),
  },
];

export const CHANNELS: Channel[] = ["MOBILE_APP", "WEB", "API", "BRANCH", "PHONE"];
