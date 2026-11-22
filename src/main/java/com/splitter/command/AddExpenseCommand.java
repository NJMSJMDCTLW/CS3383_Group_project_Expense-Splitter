package com.splitter.command;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;

/**
 * Adds an expense to a group. Undo removes it again; the group's expense id counter is not decremented.
 */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public final class AddExpenseCommand implements Command {

    private final Group group;
    private final Expense expense;

    /** @throws ValidationException if the group or the expense is null */
    public AddExpenseCommand(Group group, Expense expense) {
        if (group == null || expense == null) {
            throw new ValidationException("A command needs a group and an expense.");
        }
        this.group = group;
        this.expense = expense;
    }

    @Override
    public void execute() {
        group.addExpense(expense);
    }

    @Override
    public void undo() {
        group.removeExpense(expense.getId());
    }
}
