package com.splitter.model;

import com.splitter.meta.AIGenerated;

/** The ways an expense can be split between its participants. */
@AIGenerated(tool = "Claude Code", prompt = "P02")
public enum SplitMethod {
    EQUAL("Equal"),
    EXACT("Exact amounts"),
    PERCENTAGE("Percentages"),
    SHARES("Shares");

    private final String displayName;

    SplitMethod(String displayName) {
        this.displayName = displayName;
    }

    /** Text shown in the split method combo box. */
    public String getDisplayName() {
        return displayName;
    }
}
