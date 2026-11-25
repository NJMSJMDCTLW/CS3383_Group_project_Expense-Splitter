package com.splitter.service;

import com.splitter.meta.AIGenerated;

/**
 * Observer of {@link ExpenseService}: told after every successful change of the
 * data.
 */
@AIGenerated(
        tool = "Claude Code",
        prompt = "P08",
        refinedBy = "Dora",
        changes = "Fix #7: notify listeners with onGroupReplaced on New/Open."
)
public interface ServiceListener {

    /**
     * Called once after a member or expense was added or removed, or after undo.
     */
    void onDataChanged();

    /**
     * Called once after New or Open replaced the whole group. By default this is
     * the same as
     * {@link #onDataChanged()}; a listener can override it to reset state that
     * belongs to the old group.
     */
    default void onGroupReplaced() {
        onDataChanged();
    }
}
