package com.splitter.service;

import com.splitter.command.AddExpenseCommand;
import com.splitter.command.CommandHistory;
import com.splitter.command.RemoveExpenseCommand;
import com.splitter.exception.StorageException;
import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import com.splitter.model.Transfer;
import com.splitter.split.ParticipantInput;
import com.splitter.split.SplitStrategy;
import com.splitter.split.SplitStrategyFactory;
import com.splitter.storage.Repository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default {@link ExpenseService}: one group at a time, undo for expense
 * add/remove only.
 */
@AIGenerated(tool = "Claude Code", prompt = "P05", refinedBy = "Wang Man Nga, Dora", changes = "removeMember() clears undo history so undo will not fail after removing a member; "
        + "Fix #7: notify listeners with onGroupReplaced on New/Open; "
        + "Fix #9: check for an empty group before validating the payer.")

public class ExpenseServiceImpl implements ExpenseService {

    private static final String DEFAULT_GROUP_NAME = "New Group";

    private final Repository repository;
    private final CommandHistory history;
    private final List<ServiceListener> listeners = new ArrayList<>();
    private Group group = new Group(DEFAULT_GROUP_NAME);

    public ExpenseServiceImpl(Repository repository, CommandHistory history) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.history = Objects.requireNonNull(history, "history");
    }

    @Override
    public void addListener(ServiceListener listener) {
        if (listener == null) {
            throw new ValidationException("A listener is required.");
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(ServiceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addMember(String name) {
        group.addMember(new Member(name));
        fireDataChanged();
    }

    @Override
    public void removeMember(Member m) {
        group.removeMember(m);
        history.clear();
        fireDataChanged();
    }

    @Override
    public List<Member> getMembers() {
        return Collections.unmodifiableList(group.getMembers());
    }

    @Override
    public Expense addExpense(String description, Member payer, Money amount, SplitMethod method,
            List<ParticipantInput> inputs) {

        // F15: Check whether the group has members before validating the payer.
        if (group.getMembers().isEmpty()) {
            throw new ValidationException(
                    "Please add at least one member before creating an expense.");
        }

        if (payer == null || !group.hasMember(payer)) {
            throw new ValidationException("The payer is not in the group.");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }

        if (amount.cents() > Expense.MAX_TOTAL_CENTS) {
            throw new ValidationException("Amount must not be more than 1000000.00.");
        }

        if (inputs == null) {
            throw new ValidationException("At least one participant is required.");
        }

        for (ParticipantInput input : inputs) {
            if (input != null && !group.hasMember(input.member())) {
                throw new ValidationException(
                        "The participant \"" + input.member().getName()
                                + "\" is not in the group.");
            }
        }

        SplitStrategy strategy = SplitStrategyFactory.forMethod(method);
        Map<Member, Money> shares = strategy.split(amount, inputs);

        // Validate the description and other expense details before allocating an ID.
        new Expense(1, description, payer, amount, method, shares);

        Expense expense = new Expense(
                group.allocateExpenseId(),
                description,
                payer,
                amount,
                method,
                shares);

        history.run(new AddExpenseCommand(group, expense));
        fireDataChanged();

        return expense;
    }

    @Override
    public void removeExpense(int id) {
        history.run(new RemoveExpenseCommand(group, id));
        fireDataChanged();
    }

    @Override
    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(group.getExpenses());
    }

    @Override
    public Map<Member, Money> getBalances() {
        return Collections.unmodifiableMap(BalanceCalculator.calculate(group));
    }

    @Override
    public List<Transfer> getSettlements() {
        return Collections.unmodifiableList(SettlementCalculator.suggest(BalanceCalculator.calculate(group)));
    }

    @Override
    public boolean canUndo() {
        return history.canUndo();
    }

    @Override
    public void undo() {
        history.undo();
        fireDataChanged();
    }

    @Override
    public void newGroup(String groupName) {
        group = new Group(groupName);
        history.clear();
        fireGroupReplaced();
    }

    @Override
    public void save(Path file) throws StorageException {
        if (file == null) {
            throw new ValidationException("A file is required.");
        }
        repository.save(group, file);
    }

    @Override
    public void open(Path file) throws StorageException {
        if (file == null) {
            throw new ValidationException("A file is required.");
        }
        Group loaded = repository.load(file);
        if (loaded == null) {
            throw new StorageException("The file could not be read: " + file, null);
        }
        group = loaded;
        history.clear();
        fireGroupReplaced();
    }

    private void fireGroupReplaced() {
        for (ServiceListener listener : List.copyOf(listeners)) {
            listener.onGroupReplaced();
        }
    }

    @Override
    public String getGroupName() {
        return group.getName();
    }

    private void fireDataChanged() {
        for (ServiceListener listener : List.copyOf(listeners)) {
            listener.onDataChanged();
        }
    }
}
