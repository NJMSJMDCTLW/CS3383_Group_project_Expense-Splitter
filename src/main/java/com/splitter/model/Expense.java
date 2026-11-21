package com.splitter.model;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable expense. It stores the already computed shares, so balances and saved files do not
 * depend on the split strategy code. All fields are validated on construction.
 */
@AIGenerated(tool = "Claude Code", prompt = "P02")
public final class Expense {

    private static final long MAX_TOTAL_CENTS = 100_000_000L;

    private final int id;
    private final String description;
    private final Member payer;
    private final Money total;
    private final SplitMethod method;
    private final Map<Member, Money> shares;

    /**
     * @throws ValidationException if the id is below 1; the description is blank or has a line break;
     *                             the payer, total or method is null; the total is not positive or is
     *                             above 1,000,000.00; the shares are empty or contain a negative or
     *                             null value; or the shares do not sum exactly to the total
     */
    public Expense(int id, String description, Member payer, Money total, SplitMethod method,
                   Map<Member, Money> shares) {
        if (id < 1) {
            throw new ValidationException("Expense id must be at least 1.");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description must not be empty.");
        }
        if (description.indexOf('\n') >= 0 || description.indexOf('\r') >= 0) {
            throw new ValidationException("Description must not contain line breaks.");
        }
        if (payer == null) {
            throw new ValidationException("An expense needs a payer.");
        }
        if (method == null) {
            throw new ValidationException("An expense needs a split method.");
        }
        if (total == null || total.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (total.cents() > MAX_TOTAL_CENTS) {
            throw new ValidationException("Amount must not be more than 1000000.00.");
        }
        if (shares == null || shares.isEmpty()) {
            throw new ValidationException("An expense needs at least one participant.");
        }
        Map<Member, Money> copy = new LinkedHashMap<>();
        Money sum = Money.ZERO;
        for (Map.Entry<Member, Money> entry : shares.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new ValidationException("Every share needs a member and an amount.");
            }
            if (entry.getValue().signum() < 0) {
                throw new ValidationException("A share must not be negative.");
            }
            copy.put(entry.getKey(), entry.getValue());
            sum = sum.plus(entry.getValue());
        }
        if (!sum.equals(total)) {
            throw new ValidationException("The shares (" + sum + ") must add up to the total (" + total + ").");
        }
        this.id = id;
        this.description = description;
        this.payer = payer;
        this.total = total;
        this.method = method;
        this.shares = Collections.unmodifiableMap(copy);
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Member getPayer() {
        return payer;
    }

    public Money getTotal() {
        return total;
    }

    public SplitMethod getMethod() {
        return method;
    }

    /** Unmodifiable map, member to share, in participant order. */
    public Map<Member, Money> getShares() {
        return shares;
    }

    /** The members that have a share (including a zero share), in participant order. */
    public List<Member> getParticipants() {
        return List.copyOf(shares.keySet());
    }
}
