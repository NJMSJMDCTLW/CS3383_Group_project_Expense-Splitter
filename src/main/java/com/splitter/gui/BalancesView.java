package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import java.util.Map;

/** The Balances tab as seen by its presenter. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public interface BalancesView {

    void showBalances(Map<Member, Money> balances);

    void showError(String message);
}
