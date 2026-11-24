package com.splitter.command;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import java.util.ArrayDeque;
import java.util.Deque;

/** Undo stack. There is no redo. */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public class CommandHistory {

    private final Deque<Command> undoStack = new ArrayDeque<>();

    /**
     * Executes the command and, only if that succeeded, remembers it for undo.
     *
     * @throws ValidationException if the command is null or rejected (nothing is remembered then)
     */
    public void run(Command command) {
        if (command == null) {
            throw new ValidationException("A command is required.");
        }
        command.execute();
        undoStack.push(command);
    }

    /**
     * Undoes the most recent command. If the command cannot be undone, the exception is passed on and
     * the command stays on the stack.
     *
     * @throws ValidationException if there is nothing to undo
     */
    public void undo() {
        Command last = undoStack.peek();
        if (last == null) {
            throw new ValidationException("There is nothing to undo.");
        }
        last.undo();
        undoStack.pop();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Forgets all commands. */
    public void clear() {
        undoStack.clear();
    }
}
