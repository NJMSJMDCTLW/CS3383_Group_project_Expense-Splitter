package com.splitter.command;

import com.splitter.meta.AIGenerated;

/** An undoable change to the group (Command pattern). */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public interface Command {

    /** Applies the change. @throws com.splitter.exception.ValidationException if it is rejected */
    void execute();

    /** Reverts the change made by {@link #execute()}. */
    void undo();
}
