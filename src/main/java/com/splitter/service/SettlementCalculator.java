package com.splitter.service;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.Transfer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Package-private helper for {@code ExpenseService.getSettlements()} (DESIGN.md 4.2).
 * Not part of the public design; it only holds the greedy algorithm so that it can be unit-tested on its own.
 */
@AIGenerated(tool = "Claude Code", prompt = "P04")
final class SettlementCalculator {

    private SettlementCalculator() {
    }

    /** A member with the amount (in cents, always greater than zero) still to be paid or received. */
    private static final class Open {
        private final Member member;
        private long cents;

        private Open(Member member, long cents) {
            this.member = member;
            this.cents = cents;
        }
    }

    /**
     * Greedy settle-up: repeatedly the largest debtor pays the largest creditor the smaller of the two
     * amounts, until everything is settled. Ties are broken by the iteration order of {@code balances}
     * (member insertion order). Members with a zero balance never appear. Suggestions only.
     *
     * @param balances net balances, positive = is owed; they must sum to zero
     * @throws IllegalStateException if the balances do not sum to zero (internal error)
     */
    static List<Transfer> suggest(Map<Member, Money> balances) {
        List<Open> debtors = new ArrayList<>();
        List<Open> creditors = new ArrayList<>();
        for (Map.Entry<Member, Money> entry : balances.entrySet()) {
            long cents = entry.getValue().cents();
            if (cents < 0) {
                debtors.add(new Open(entry.getKey(), -cents));
            } else if (cents > 0) {
                creditors.add(new Open(entry.getKey(), cents));
            }
        }
        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Open debtor = largest(debtors);
            Open creditor = largest(creditors);
            long amount = Math.min(debtor.cents, creditor.cents);
            transfers.add(new Transfer(debtor.member, creditor.member, Money.ofCents(amount)));
            debtor.cents -= amount;
            creditor.cents -= amount;
            removeSettled(debtors);
            removeSettled(creditors);
        }
        if (!debtors.isEmpty() || !creditors.isEmpty()) {
            throw new IllegalStateException("Balances do not sum to zero.");
        }
        return transfers;
    }

    /** The entry with the largest amount; the earliest one wins a tie. */
    private static Open largest(List<Open> list) {
        Open best = list.get(0);
        for (Open candidate : list) {
            if (candidate.cents > best.cents) {
                best = candidate;
            }
        }
        return best;
    }

    private static void removeSettled(List<Open> list) {
        Iterator<Open> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().cents == 0) {
                it.remove();
            }
        }
    }
}
