/**
 * A minimal decimal type mirroring the subset of java.math.BigDecimal the rule
 * engine uses.
 *
 * Money in the Java engine is BigDecimal, and both amount rules compare with
 * `>=` — so exact equality at a threshold FIRES the rule. An amount of exactly
 * 2500.00 against a 2500.00 fallback, or exactly `mean + 3 * stdDev`, is
 * precisely where a JavaScript `number` port diverges from the engine, and
 * precisely what the parity test checks. Using floats here would make the
 * boundary the broken case.
 *
 * Represented as an unscaled BigInt plus a scale, exactly as BigDecimal does.
 * Comparison is numeric and ignores scale, matching BigDecimal.compareTo.
 */
export class Decimal {
  private constructor(
    readonly unscaled: bigint,
    readonly scale: number,
  ) {}

  static parse(value: string | number): Decimal {
    // String(number) yields the shortest round-tripping representation, the
    // same contract Double.toString has — which is what BigDecimal.valueOf(double)
    // is defined in terms of.
    const text = typeof value === "number" ? String(value) : value.trim();

    if (!/^[+-]?\d+(\.\d+)?([eE][+-]?\d+)?$/.test(text)) {
      throw new Error(`not a decimal: ${text}`);
    }
    if (/[eE]/.test(text)) {
      // Normalise exponent form by round-tripping through a fixed rendering.
      return Decimal.parse(Number(text).toFixed(20).replace(/0+$/, "").replace(/\.$/, ".0"));
    }

    const negative = text.startsWith("-");
    const digits = text.replace(/^[+-]/, "");
    const [whole, fraction = ""] = digits.split(".");
    const unscaled = BigInt(whole + fraction);
    return new Decimal(negative ? -unscaled : unscaled, fraction.length);
  }

  static readonly ZERO = Decimal.parse("0");

  private static align(a: Decimal, b: Decimal): [bigint, bigint, number] {
    const scale = Math.max(a.scale, b.scale);
    return [
      a.unscaled * 10n ** BigInt(scale - a.scale),
      b.unscaled * 10n ** BigInt(scale - b.scale),
      scale,
    ];
  }

  add(other: Decimal): Decimal {
    const [left, right, scale] = Decimal.align(this, other);
    return new Decimal(left + right, scale);
  }

  subtract(other: Decimal): Decimal {
    const [left, right, scale] = Decimal.align(this, other);
    return new Decimal(left - right, scale);
  }

  /** Scales add, as in BigDecimal.multiply. Exact — never rounds. */
  multiply(other: Decimal): Decimal {
    return new Decimal(this.unscaled * other.unscaled, this.scale + other.scale);
  }

  /** Divide to a fixed scale with HALF_UP rounding, as BigDecimal.divide(d, scale, HALF_UP). */
  divide(other: Decimal, scale: number): Decimal {
    if (other.isZero()) {
      throw new Error("division by zero");
    }
    // Shift the numerator so integer division lands one digit past the target
    // scale, then round that digit half-away-from-zero.
    const shift = BigInt(scale + 1 + other.scale - this.scale);
    const numerator =
      shift >= 0n ? this.unscaled * 10n ** shift : this.unscaled / 10n ** -shift;
    const quotient = numerator / other.unscaled;

    const negative = quotient < 0n;
    const magnitude = negative ? -quotient : quotient;
    const lastDigit = magnitude % 10n;
    let rounded = magnitude / 10n;
    if (lastDigit >= 5n) {
      rounded += 1n; // HALF_UP: away from zero, not banker's rounding
    }
    return new Decimal(negative ? -rounded : rounded, scale);
  }

  /** Numeric comparison, ignoring scale — BigDecimal.compareTo semantics. */
  compareTo(other: Decimal): number {
    const [left, right] = Decimal.align(this, other);
    if (left < right) return -1;
    if (left > right) return 1;
    return 0;
  }

  gte(other: Decimal): boolean {
    return this.compareTo(other) >= 0;
  }

  lt(other: Decimal): boolean {
    return this.compareTo(other) < 0;
  }

  isZero(): boolean {
    return this.unscaled === 0n;
  }

  signum(): number {
    return this.unscaled === 0n ? 0 : this.unscaled < 0n ? -1 : 1;
  }

  /** Plain string, never exponent notation — BigDecimal.toPlainString. */
  toString(): string {
    const negative = this.unscaled < 0n;
    let digits = (negative ? -this.unscaled : this.unscaled).toString();
    if (this.scale === 0) {
      return (negative ? "-" : "") + digits;
    }
    digits = digits.padStart(this.scale + 1, "0");
    const whole = digits.slice(0, digits.length - this.scale);
    const fraction = digits.slice(digits.length - this.scale);
    return `${negative ? "-" : ""}${whole}.${fraction}`;
  }

  /** Lossy. For display and for the composite score only — never for a rule comparison. */
  toNumber(): number {
    return Number(this.toString());
  }
}

export const dec = (value: string | number): Decimal => Decimal.parse(value);
