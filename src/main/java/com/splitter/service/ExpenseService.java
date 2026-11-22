package com.splitter.service;

import com.splitter.exception.StorageException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import com.splitter.model.Transfer;
import com.splitter.split.ParticipantInput;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The only service type the GUI uses. Rejected input throws
 * {@link com.splitter.exception.ValidationException}; a rejected call changes nothing and notifies nobody.
 */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public interface ExpenseService {

    void addListener(ServiceListener listener);

    void removeListener(ServiceListener listener);

    void addMember(String name);

    void removeMember(Member m);

    List<Member> getMembers();

    Expense addExpense(String description, Member payer, Money amount, SplitMethod method,
                       List<ParticipantInput> inputs);

    void removeExpense(int id);

    List<Expense> getExpenses();

    Map<Member, Money> getBalances();

    List<Transfer> getSettlements();

    boolean canUndo();

    void undo();

    void newGroup(String groupName);

    void save(Path file) throws StorageException;

    void open(Path file) throws StorageException;

    String getGroupName();
}
