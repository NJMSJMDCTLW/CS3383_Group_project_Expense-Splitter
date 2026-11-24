package com.splitter.service;

import com.splitter.meta.AIGenerated;

/** Observer of {@link ExpenseService}: told after every successful change of the data. */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public interface ServiceListener {

    /** Called once after a member or expense was added or removed, or after undo, new or open. */
    void onDataChanged();
}
