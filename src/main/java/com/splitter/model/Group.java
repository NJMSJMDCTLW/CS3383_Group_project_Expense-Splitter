package com.splitter.model;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable aggregate that holds the members and the expenses of one group.
 * Expenses are kept sorted by id. Expense ids only increase and are never
 * reused.
 */
@AIGenerated(tool = "Claude Code", prompt = "P02", refinedBy = "Dora", changes = "Fix #5: long id counter, reject overflow.")
public class Group {

    private final String name;
    private final List<Member> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private long nextExpenseId = 1L;

    /**
     * @throws ValidationException if the name is null, blank or contains a line
     *                             break
     */
    public Group(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Group name must not be empty.");
        }
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            throw new ValidationException("Group name must not contain line breaks.");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * @throws ValidationException if the member is null or a member with the same
     *                             key exists
     */
    public void addMember(Member member) {
        if (member == null) {
            throw new ValidationException("Member must not be empty.");
        }
        if (members.contains(member)) {
            throw new ValidationException("A member named \"" + member.getName() + "\" already exists.");
        }
        members.add(member);
    }

    /**
     * @throws ValidationException if the member is not in the group, or is the
     *                             payer or a participant
     *                             (including a zero share) of any existing expense
     */
    public void removeMember(Member member) {
        if (!hasMember(member)) {
            throw new ValidationException("Member is not in the group.");
        }
        for (Expense expense : expenses) {
            if (expense.getPayer().equals(member) || expense.getShares().containsKey(member)) {
                throw new ValidationException("Cannot remove \"" + member.getName()
                        + "\": the member is used by the expense \"" + expense.getDescription() + "\".");
            }
        }
        members.remove(member);
    }

    /** Unmodifiable snapshot, in insertion order. */
    public List<Member> getMembers() {
        return List.copyOf(members);
    }

    public boolean hasMember(Member member) {
        return member != null && members.contains(member);
    }

    /**
     * Stores the expense at the position given by its id.
     *
     * @throws ValidationException if the expense is null, the payer or a
     *                             participant is not a member,
     *                             or an expense with the same id already exists
     */
    public Expense addExpense(Expense expense) {
        if (expense == null) {
            throw new ValidationException("Expense must not be empty.");
        }
        if (!hasMember(expense.getPayer())) {
            throw new ValidationException("The payer \"" + expense.getPayer().getName()
                    + "\" is not in the group.");
        }
        for (Member participant : expense.getParticipants()) {
            if (!hasMember(participant)) {
                throw new ValidationException("The participant \"" + participant.getName()
                        + "\" is not in the group.");
            }
        }
        int index = 0;
        while (index < expenses.size() && expenses.get(index).getId() < expense.getId()) {
            index++;
        }
        if (index < expenses.size() && expenses.get(index).getId() == expense.getId()) {
            throw new ValidationException("An expense with id " + expense.getId() + " already exists.");
        }
        expenses.add(index, expense);
        return expense;
    }

    /** @throws ValidationException if no expense has this id */
    public void removeExpense(int id) {
        for (int i = 0; i < expenses.size(); i++) {
            if (expenses.get(i).getId() == id) {
                expenses.remove(i);
                return;
            }
        }
        throw new ValidationException("There is no expense with id " + id + ".");
    }

    /** Unmodifiable snapshot, sorted by id. */
    public List<Expense> getExpenses() {
        return List.copyOf(expenses);
    }

    /** Returns the current counter value, then advances the counter by one. */
    public int allocateExpenseId() {
        if (nextExpenseId > Integer.MAX_VALUE) {
            throw new ValidationException(
                    "No more expense IDs are available.");
        }

        return (int) nextExpenseId++;
    }

    /** Returns the counter value without advancing it (used when saving). */
    public long getNextExpenseId() {
        return nextExpenseId;
    }

    /**
     * Sets the counter when a saved group is loaded.
     *
     * @throws ValidationException if {@code next} is below 1 or not greater than
     *                             every expense id
     *                             currently in the group
     */
    public void restoreNextExpenseId(long next) {
        if (next < 1 || next > (long) Integer.MAX_VALUE + 1L) {
            throw new ValidationException(
                    "The next expense id is outside the supported range.");
        }

        for (Expense expense : expenses) {
            if (next <= expense.getId()) {
                throw new ValidationException(
                        "The next expense id must be greater than every existing expense id.");
            }
        }

        nextExpenseId = next;
    }
}
