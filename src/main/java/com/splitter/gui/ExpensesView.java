package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Member;
import java.util.List;

/** The Expenses tab as seen by its presenter. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public interface ExpensesView {

    /** Shows the members in the payer combo box and the participant checkboxes. */
    void showMembers(List<Member> members);

    void showExpenses(List<Expense> expenses);

    void clearForm();

    void showError(String message);
}
