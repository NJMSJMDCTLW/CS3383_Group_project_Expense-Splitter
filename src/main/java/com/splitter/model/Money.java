package com.splitter.model;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Immutable amount of money stored as whole cents in a {@code long}. Never uses floating point. */
@AIGenerated(tool = "Claude Code", prompt = "P02")
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(0L);

    private static final Pattern VALID = Pattern.compile("[0-9]+(\\.[0-9]{1,2})?");
    private static final Pattern TOO_MANY_DECIMALS = Pattern.compile("[0-9]+\\.[0-9]{3,}");

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    /** Creates an amount from cents. Negative values are allowed (used for balances). */
    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /**
     * Parses a non-negative decimal such as "12", "12.5" or "12.50".
     *
     * @throws ValidationException if the text is null, blank, negative, not a plain decimal,
     *                             has more than 2 decimals, or is too large
     */
    public static Money parse(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Amount must not be empty.");
        }
        if (text.startsWith("-")) {
            throw new ValidationException("Amount must not be negative: " + text);
        }
        if (TOO_MANY_DECIMALS.matcher(text).matches()) {
            throw new ValidationException("Amount must have at most 2 decimal places: " + text);
        }
        if (!VALID.matcher(text).matches()) {
            throw new ValidationException("Amount is not a valid number: " + text);
        }
        int dot = text.indexOf('.');
        String whole = dot < 0 ? text : text.substring(0, dot);
        String fraction = dot < 0 ? "" : text.substring(dot + 1);
        while (fraction.length() < 2) {
            fraction = fraction + "0";
        }
        try {
            long wholeCents = Math.multiplyExact(Long.parseLong(whole), 100L);
            return new Money(Math.addExact(wholeCents, Long.parseLong(fraction)));
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ValidationException("Amount is too large: " + text);
        }
    }

    public long cents() {
        return cents;
    }

    /** Returns this + other. @throws ValidationException on overflow */
    public Money plus(Money other) {
        try {
            return new Money(Math.addExact(cents, other.cents));
        } catch (ArithmeticException e) {
            throw new ValidationException("Amount is too large.");
        }
    }

    /** Returns this - other. @throws ValidationException on overflow */
    public Money minus(Money other) {
        try {
            return new Money(Math.subtractExact(cents, other.cents));
        } catch (ArithmeticException e) {
            throw new ValidationException("Amount is too large.");
        }
    }

    /** Returns -this. @throws ValidationException on overflow */
    public Money negate() {
        try {
            return new Money(Math.negateExact(cents));
        } catch (ArithmeticException e) {
            throw new ValidationException("Amount is too large.");
        }
    }

    /** Returns -1, 0 or 1 for a negative, zero or positive amount. */
    public int signum() {
        return Long.signum(cents);
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money && ((Money) o).cents == cents;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(cents);
    }

    /** Plain amount with 2 decimals and no currency symbol, e.g. "12.34" or "-0.05". */
    @Override
    public String toString() {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
