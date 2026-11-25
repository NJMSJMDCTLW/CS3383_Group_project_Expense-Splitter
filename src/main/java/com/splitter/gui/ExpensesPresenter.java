package com.splitter.gui;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import com.splitter.service.ExpenseService;
import com.splitter.service.ServiceListener;
import com.splitter.split.ParticipantInput;
import java.util.List;

/** Presenter of the Expenses tab. Contains no Swing code. */
@AIGenerated(
    tool = "Claude Code",
    prompt = "P08",
    refinedBy = "Dora",
    changes = "Fix #6: pass split method to InputParser for participant validation."
)
public class ExpensesPresenter implements ServiceListener {

    private final ExpenseService service;
    private final ExpensesView view;
    private final InputParser parser = new InputParser();

    public ExpensesPresenter(ExpenseService service, ExpensesView view) {
        this.service = service;
        this.view = view;
    }

    /** Starts listening to the service and shows the current members and expenses. */
    public void start() {
        service.addListener(this);
        refresh();
    }

    public void stop() {
        service.removeListener(this);
    }

    /**
     * Turns the typed text into values and adds the expense. The form is cleared only after the expense
     * was added; if anything is rejected, the message is shown and the form stays as it is.
     */
    public void onAddExpense(String description, Member payer, String amountText, SplitMethod method,
                             List<ParticipantText> texts) {
        try {
            Money amount = parser.parseMoney(amountText);
            List<ParticipantInput> inputs = parser.parseInputs(texts, method);            
            service.addExpense(description, payer, amount, method, inputs);
            view.clearForm();
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }
    public void onRemoveSelected(int expenseId) {
        try {
            service.removeExpense(expenseId);
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onDataChanged() {
        refresh();
    }

    private void refresh() {
        view.showMembers(service.getMembers());
        view.showExpenses(service.getExpenses());
    }
}
