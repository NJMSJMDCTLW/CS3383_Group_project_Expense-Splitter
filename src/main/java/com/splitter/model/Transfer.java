package com.splitter.model;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;

/** Immutable suggested payment: {@code from} pays {@code to} the given amount. */
@AIGenerated(tool = "Claude Code", prompt = "P02")
public final class Transfer {

    private final Member from;
    private final Member to;
    private final Money amount;

    /** @throws ValidationException if a value is null, from equals to, or amount is not positive */
    public Transfer(Member from, Member to, Money amount) {
        if (from == null || to == null || amount == null) {
            throw new ValidationException("A transfer needs a payer, a receiver and an amount.");
        }
        if (from.equals(to)) {
            throw new ValidationException("A member cannot pay themselves.");
        }
        if (amount.signum() <= 0) {
            throw new ValidationException("Transfer amount must be greater than zero.");
        }
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public Member getFrom() {
        return from;
    }

    public Member getTo() {
        return to;
    }

    public Money getAmount() {
        return amount;
    }

    /** For example "A pays B 12.34" (no currency symbol). */
    @Override
    public String toString() {
        return from.getName() + " pays " + to.getName() + " " + amount;
    }
}
