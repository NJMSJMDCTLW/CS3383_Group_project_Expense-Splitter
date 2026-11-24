package com.splitter.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Package command, 10 test cases. Techniques: state-based behaviour of the undo stack and the two
 * commands, plus equivalence classes for null/invalid arguments (DESIGN.md 4.2 notes).
 */
@AIGenerated(tool = "Claude Code", prompt = "T02")
class CommandTest {

    private static final Member A = new Member("A");

    private static Group group() {
        Group g = new Group("G");
        g.addMember(A);
        return g;
    }

    private static Expense expense(int id, long cents) {
        Map<Member, Money> shares = new LinkedHashMap<>();
        shares.put(A, Money.ofCents(cents));
        return new Expense(id, "d", A, Money.ofCents(cents), SplitMethod.EQUAL, shares);
    }

    @Test
    @DisplayName("TC-C01 AddExpenseCommand adds then undo removes it (state-based)")
    void addExecuteAndUndo() {
        Group g = group();
        AddExpenseCommand cmd = new AddExpenseCommand(g, expense(1, 100));
        cmd.execute();
        assertEquals(1, g.getExpenses().size());
        cmd.undo();
        assertTrue(g.getExpenses().isEmpty());
    }

    @Test
    @DisplayName("TC-C02 AddExpenseCommand rejects null group/expense (equivalence class)")
    void addRejectsNull() {
        assertThrows(ValidationException.class, () -> new AddExpenseCommand(null, expense(1, 100)));
        assertThrows(ValidationException.class, () -> new AddExpenseCommand(group(), null));
    }

    @Test
    @DisplayName("TC-C03 RemoveExpenseCommand removes then undo restores it (state-based)")
    void removeExecuteAndUndo() {
        Group g = group();
        g.addExpense(expense(1, 100));
        RemoveExpenseCommand cmd = new RemoveExpenseCommand(g, 1);
        cmd.execute();
        assertTrue(g.getExpenses().isEmpty());
        cmd.undo();
        assertEquals(1, g.getExpenses().size());
    }

    @Test
    @DisplayName("TC-C04 RemoveExpenseCommand.execute rejects unknown id (equivalence class)")
    void removeUnknownIdRejected() {
        RemoveExpenseCommand cmd = new RemoveExpenseCommand(group(), 9);
        assertThrows(ValidationException.class, cmd::execute);
    }

    @Test
    @DisplayName("TC-C05 RemoveExpenseCommand.undo before execute throws IllegalStateException (state-based)")
    void removeUndoBeforeExecute() {
        RemoveExpenseCommand cmd = new RemoveExpenseCommand(group(), 1);
        assertThrows(IllegalStateException.class, cmd::undo);
    }

    @Test
    @DisplayName("TC-C06 CommandHistory.run executes and becomes undoable (state-based)")
    void runMakesUndoable() {
        CommandHistory h = new CommandHistory();
        Group g = group();
        assertFalse(h.canUndo());
        h.run(new AddExpenseCommand(g, expense(1, 100)));
        assertTrue(h.canUndo());
        assertEquals(1, g.getExpenses().size());
    }

    @Test
    @DisplayName("TC-C07 CommandHistory.undo reverses the last command (state-based)")
    void historyUndo() {
        CommandHistory h = new CommandHistory();
        Group g = group();
        h.run(new AddExpenseCommand(g, expense(1, 100)));
        h.undo();
        assertTrue(g.getExpenses().isEmpty());
        assertFalse(h.canUndo());
    }

    @Test
    @DisplayName("TC-C08 CommandHistory.undo with empty stack throws ValidationException (equivalence class)")
    void historyUndoEmptyRejected() {
        assertThrows(ValidationException.class, () -> new CommandHistory().undo());
    }

    @Test
    @DisplayName("TC-C09 CommandHistory.run with null command throws and stays empty (equivalence class)")
    void historyNullCommandRejected() {
        CommandHistory h = new CommandHistory();
        assertThrows(ValidationException.class, () -> h.run(null));
        assertFalse(h.canUndo());
    }

    @Test
    @DisplayName("TC-C10 a failed execute is not remembered; clear empties the stack (state-based)")
    void failedExecuteNotRememberedAndClear() {
        CommandHistory h = new CommandHistory();
        Group g = group();
        assertThrows(ValidationException.class, () -> h.run(new RemoveExpenseCommand(g, 9)));
        assertFalse(h.canUndo());
        h.run(new AddExpenseCommand(g, expense(1, 100)));
        h.clear();
        assertFalse(h.canUndo());
        assertEquals(1, g.getExpenses().size());
    }

    @Test
    @DisplayName("TC-C11 multiple undos run LIFO (last added removed first) (state-based)")
    void multipleUndosLifo() {
        CommandHistory h = new CommandHistory();
        Group g = group();
        h.run(new AddExpenseCommand(g, expense(1, 100)));
        h.run(new AddExpenseCommand(g, expense(2, 100)));
        h.undo();
        assertEquals(List.of(1), g.getExpenses().stream().map(Expense::getId).toList());
        h.undo();
        assertTrue(g.getExpenses().isEmpty());
        assertFalse(h.canUndo());
    }
}
