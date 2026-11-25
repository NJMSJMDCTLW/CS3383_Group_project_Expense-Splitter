package com.splitter.service;

import com.splitter.meta.AIGenerated;

/** Observer of {@link ExpenseService}: told after every successful change of the data. */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public interface ServiceListener {

    void onDataChanged();

    default void onGroupReplaced() {
        onDataChanged();
    }
}
