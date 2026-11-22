package com.splitter.service;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;
import com.splitter.model.Member;
import com.splitter.model.Money;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Package-private helper for {@code ExpenseService.getBalances()} (DESIGN.md 4.2).
 * Not part of the public design; it only holds the arithmetic so that it can be unit-tested on its own.
 */
@AIGenerated(tool = "Claude Code", prompt = "P04")
final class BalanceCalculator {

    private BalanceCalculator() {
    }

    /**
     * Net balance per member: paid minus owed. Positive means the member is owed money, negative means
     * the member owes money. The map is ordered by member insertion order and includes members whose
     * balance is zero. The balances always sum to zero.
     */
    static Map<Member, Money> calculate(Group group) {
        Map<Member, Money> balances = new LinkedHashMap<>();
        for (Member member : group.getMembers()) {
            balances.put(member, Money.ZERO);
        }
        for (Expense expense : group.getExpenses()) {
            balances.merge(expense.getPayer(), expense.getTotal(), Money::plus);
            for (Map.Entry<Member, Money> share : expense.getShares().entrySet()) {
                balances.merge(share.getKey(), share.getValue().negate(), Money::plus);
            }
        }
        return balances;
    }
}
