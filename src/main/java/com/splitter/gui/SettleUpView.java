package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Transfer;
import java.util.List;

/** The Settle Up tab as seen by its presenter. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public interface SettleUpView {

    void showSettlements(List<Transfer> transfers);

    void showError(String message);
}
