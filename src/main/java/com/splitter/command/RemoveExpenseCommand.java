package com.splitter.command;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;

/**
 * Removes an expense from a group. Undo puts the same expense back; because the group keeps expenses
 * sorted by id, it returns to its original position.
 */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public final class RemoveExpenseCommand implements Command {

    private final Group group;
    private final int expenseId;
    private Expense removed;

    /** @throws ValidationException if the group is null */
    public RemoveExpenseCommand(Group group, int expenseId) {
        if (group == null) {
            throw new ValidationException("A command needs a group.");
        }
        this.group = group;
        this.expenseId = expenseId;
    }

    /** @throws ValidationException if there is no expense with this id */
    @Override
    public void execute() {
        Expense found = null;
        for (Expense expense : group.getExpenses()) {
            if (expense.getId() == expenseId) {
                found = expense;
                break;
            }
        }
        if (found == null) {
            throw new ValidationException("There is no expense with id " + expenseId + ".");
        }
        group.removeExpense(expenseId);
        removed = found;
    }

    /**
     * @throws ValidationException if the group rejects the expense, for example because its payer was
     *                             removed from the group in the meantime
     * @throws IllegalStateException if {@link #execute()} has not run successfully
     */
    @Override
    public void undo() {
        if (removed == null) {
            throw new IllegalStateException("The command has not been executed.");
        }
        group.addExpense(removed);
    }
}
