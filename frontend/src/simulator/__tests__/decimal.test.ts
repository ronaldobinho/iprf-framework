import { describe, expect, it } from "vitest";
import { Decimal, dec } from "../decimal";

/**
 * The decimal type exists for one reason: both amount rules compare with `>=`,
 * so exact equality at a threshold FIRES the rule. That boundary is where a
 * float port silently diverges from the Java engine, and it is what the parity
 * test checks.
 */
describe("Decimal", () => {
  it("compares numerically, ignoring scale, like BigDecimal.compareTo", () => {
    expect(dec("2500.00").compareTo(dec("2500"))).toBe(0);
    expect(dec("2500.0000").compareTo(dec("2500.00"))).toBe(0);
    expect(dec("2500.00").gte(dec("2500.00"))).toBe(true);
  });

  it("holds values a float cannot represent exactly", () => {
    // 0.1 + 0.2 === 0.30000000000000004 as a float.
    expect(dec("0.1").add(dec("0.2")).toString()).toBe("0.3");
    expect(dec("0.1").add(dec("0.2")).compareTo(dec("0.3"))).toBe(0);
  });

  it("adds scales on multiply, exactly, without rounding", () => {
    // stdDev(scale 2) * sigmas — the mean + 3 sigma threshold computation.
    expect(dec("20.00").multiply(dec(3)).toString()).toBe("60.00");
    expect(dec("20.55").multiply(dec("1.5")).toString()).toBe("30.825");
  });

  it("reproduces the mean + 3 sigma threshold exactly", () => {
    const mean = dec("100.00");
    const stdDev = dec("20.00");
    const threshold = mean.add(stdDev.multiply(dec(3)));

    expect(threshold.compareTo(dec("160.00"))).toBe(0);
    // Exactly at the boundary fires; a cent below does not.
    expect(dec("160.00").gte(threshold)).toBe(true);
    expect(dec("159.99").gte(threshold)).toBe(false);
  });

  it("divides with HALF_UP, away from zero, not banker's rounding", () => {
    // BigDecimal.divide(d, 1, HALF_UP)
    expect(dec("100.00").divide(dec("20.00"), 1).toString()).toBe("5.0");
    expect(dec("0.25").divide(dec("1"), 1).toString()).toBe("0.3");
    expect(dec("0.35").divide(dec("1"), 1).toString()).toBe("0.4");
    expect(dec("-0.25").divide(dec("1"), 1).toString()).toBe("-0.3");
  });

  it("renders plainly, never in exponent notation", () => {
    expect(dec("0.00001").toString()).toBe("0.00001");
    expect(dec("98000.00").toString()).toBe("98000.00");
    expect(Decimal.ZERO.toString()).toBe("0");
  });

  it("rejects input that is not a decimal", () => {
    expect(() => dec("abc")).toThrow();
    expect(() => dec("")).toThrow();
  });
});
