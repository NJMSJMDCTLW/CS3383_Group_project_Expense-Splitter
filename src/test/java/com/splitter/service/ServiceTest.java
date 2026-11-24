package com.splitter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splitter.command.CommandHistory;
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
import com.splitter.storage.Repository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Package service, 10 test cases. Techniques: canonical scenarios from the rules, state-based id and undo
 * behaviour, and observer branch checks. Uses the package-private helper classes and a fake Repository.
 */
@AIGenerated(tool = "Claude Code", prompt = "T02")
class ServiceTest {

    /** Minimal in-memory Repository so the service can be tested without the file layer. */
    private static final class FakeRepository implements Repository {
        Group stored;

        @Override
        public void save(Group g, Path file) {
            this.stored = g;
        }

        @Override
        public Group load(Path file) throws StorageException {
            if (stored == null) {
                throw new StorageException("empty", null);
            }
            return stored;
        }
    }

    private static ParticipantInput in(Member m, String value) {
        return new ParticipantInput(m, new BigDecimal(value));
    }

    private static Expense expense(int id, Member payer, Member participant, long cents) {
        Map<Member, Money> shares = new LinkedHashMap<>();
        shares.put(participant, Money.ofCents(cents));
        return new Expense(id, "d", payer, Money.ofCents(cents), SplitMethod.EQUAL, shares);
    }

    @Test
    @DisplayName("TC-V01 A pays 90 equally for A,B,C -> B pays A 30 and C pays A 30 (balances + settle 4.2)")
    void balancesAndSettle() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        s.addMember("B");
        s.addMember("C");
        Member a = s.getMembers().get(0);
        s.addExpense("x", a, Money.ofCents(90), SplitMethod.EQUAL,
                List.of(in(s.getMembers().get(0), "0"), in(s.getMembers().get(1), "0"), in(s.getMembers().get(2), "0")));
        Map<Member, Money> bal = s.getBalances();
        assertEquals(Money.ofCents(60), bal.get(a));
        long sum = bal.values().stream().mapToLong(Money::cents).sum();
        assertEquals(0L, sum);
        List<Transfer> t = s.getSettlements();
        assertEquals(2, t.size());
        assertEquals(Money.ofCents(30), t.get(0).getAmount());
    }

    @Test
    @DisplayName("TC-V02 two creditors one debtor: C pays A 50 and C pays B 20 (canonical settle 4.2)")
    void twoCreditorsOneDebtor() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        s.addMember("B");
        s.addMember("C");
        Member a = s.getMembers().get(0);
        Member b = s.getMembers().get(1);
        Member c = s.getMembers().get(2);
        s.addExpense("x", a, Money.parse("50.00"), SplitMethod.EXACT, List.of(in(c, "50")));
        s.addExpense("y", b, Money.parse("20.00"), SplitMethod.EXACT, List.of(in(c, "20")));
        List<Transfer> t = s.getSettlements();
        assertEquals(2, t.size());
        assertTrue(t.stream().anyMatch(tr -> tr.getFrom().equals(c) && tr.getTo().equals(a)
                && tr.getAmount().equals(Money.parse("50.00"))));
        assertTrue(t.stream().anyMatch(tr -> tr.getFrom().equals(c) && tr.getTo().equals(b)
                && tr.getAmount().equals(Money.parse("20.00"))));
    }

    @Test
    @DisplayName("TC-V03 circular debts that cancel give no transfers (equivalence 4.2)")
    void circularDebtCancels() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        s.addMember("B");
        s.addMember("C");
        Member a = s.getMembers().get(0);
        Member b = s.getMembers().get(1);
        Member c = s.getMembers().get(2);
        s.addExpense("1", a, Money.parse("10.00"), SplitMethod.EXACT, List.of(in(b, "10")));
        s.addExpense("2", b, Money.parse("10.00"), SplitMethod.EXACT, List.of(in(c, "10")));
        s.addExpense("3", c, Money.parse("10.00"), SplitMethod.EXACT, List.of(in(a, "10")));
        assertTrue(s.getSettlements().isEmpty());
    }

    @Test
    @DisplayName("TC-V04 ids increase across undo: add, add, undo, add -> ids 1,2,3 (state-based N2)")
    void idsSurviveUndo() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        Member a = s.getMembers().get(0);
        s.addExpense("1", a, Money.ofCents(1), SplitMethod.EXACT, List.of(in(a, "0.01")));
        s.addExpense("2", a, Money.ofCents(1), SplitMethod.EXACT, List.of(in(a, "0.01")));
        s.undo();
        Expense third = s.addExpense("3", a, Money.ofCents(1), SplitMethod.EXACT, List.of(in(a, "0.01")));
        assertEquals(3, third.getId());
    }

    @Test
    @DisplayName("TC-V05 undo of a removal restores position (expenses sorted by id) (state-based 4.2)")
    void undoRemovalRestoresPosition() {
        Member a = new Member("A");
        Group g = new Group("G");
        g.addMember(a);
        g.addExpense(expense(1, a, a, 100));
        g.addExpense(expense(2, a, a, 100));
        g.addExpense(expense(3, a, a, 100));
        CommandHistory h = new CommandHistory();
        com.splitter.command.RemoveExpenseCommand cmd = new com.splitter.command.RemoveExpenseCommand(g, 2);
        h.run(cmd);
        assertEquals(List.of(1, 3), g.getExpenses().stream().map(Expense::getId).toList());
        h.undo();
        assertEquals(List.of(1, 2, 3), g.getExpenses().stream().map(Expense::getId).toList());
    }

    @Test
    @DisplayName("TC-V06 undo with empty history throws ValidationException (equivalence 4.2)")
    void undoEmptyRejected() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        assertFalse(s.canUndo());
        assertThrows(ValidationException.class, s::undo);
    }

    @Test
    @DisplayName("TC-V07 removing an unused member clears undo history (R2)")
    void removeMemberClearsHistory() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        s.addMember("B");
        Member a = s.getMembers().get(0);
        Member b = s.getMembers().get(1);
        s.addExpense("1", a, Money.ofCents(1), SplitMethod.EXACT, List.of(in(a, "0.01")));
        assertTrue(s.canUndo());
        s.removeMember(b);
        assertFalse(s.canUndo());
    }

    @Test
    @DisplayName("TC-V08 listener notified once on success, never on failure, never on save (observer 4.1)")
    void listenerNotifications() throws StorageException {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        AtomicInteger calls = new AtomicInteger();
        s.addListener(calls::incrementAndGet);
        s.addMember("A");
        assertEquals(1, calls.get());
        assertThrows(ValidationException.class, () -> s.addMember("A"));
        assertEquals(1, calls.get());
        s.save(Path.of("x.csv"));
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("TC-V09 returned members/balances are unmodifiable (encapsulation 4.2)")
    void returnedCollectionsUnmodifiable() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        assertThrows(UnsupportedOperationException.class, () -> s.getMembers().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.getBalances().clear());
    }

    @Test
    @DisplayName("TC-V10 failed open leaves current data unchanged (state-based 4.2)")
    void failedOpenKeepsData() {
        FakeRepository repo = new FakeRepository();
        ExpenseServiceImpl s = new ExpenseServiceImpl(repo, new CommandHistory());
        s.addMember("A");
        assertThrows(StorageException.class, () -> s.open(Path.of("missing.csv")));
        assertEquals(1, s.getMembers().size());
    }

    @Test
    @DisplayName("TC-V11 settle-up ties broken by insertion order, deterministic (decision table 4.2/Q10)")
    void settlementTieBreaksByInsertionOrder() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("D");
        s.addMember("C1");
        s.addMember("C2");
        Member d = s.getMembers().get(0);
        Member c1 = s.getMembers().get(1);
        Member c2 = s.getMembers().get(2);
        s.addExpense("a", c1, Money.parse("10.00"), SplitMethod.EXACT, List.of(in(d, "10")));
        s.addExpense("b", c2, Money.parse("10.00"), SplitMethod.EXACT, List.of(in(d, "10")));
        List<Transfer> t = s.getSettlements();
        assertEquals(List.of("D"), List.of(t.get(0).getFrom().getName()));
        assertEquals("C1", t.get(0).getTo().getName());
        assertEquals(List.of("D"), List.of(t.get(1).getFrom().getName()));
        assertEquals("C2", t.get(1).getTo().getName());
    }

    @Test
    @DisplayName("TC-V12 no expenses: balances all zero but listed, no transfers (equivalence 4.2)")
    void noExpensesNoTransfers() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        s.addMember("B");
        assertTrue(s.getSettlements().isEmpty());
        assertEquals(2, s.getBalances().size());
        assertTrue(s.getBalances().values().stream().allMatch(m -> m.equals(Money.ZERO)));
    }

    @Test
    @DisplayName("TC-V13 newGroup clears undo history (R2 / 4.2)")
    void newGroupClearsHistory() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        s.addMember("A");
        Member a = s.getMembers().get(0);
        s.addExpense("1", a, Money.ofCents(1), SplitMethod.EXACT, List.of(in(a, "0.01")));
        assertTrue(s.canUndo());
        s.newGroup("Other");
        assertFalse(s.canUndo());
        assertTrue(s.getMembers().isEmpty());
    }

    @Test
    @DisplayName("TC-V14 addListener(null) rejected; adding same listener twice notifies once (observer 4.2)")
    void listenerRegistrationRules() {
        ExpenseServiceImpl s = new ExpenseServiceImpl(new FakeRepository(), new CommandHistory());
        assertThrows(ValidationException.class, () -> s.addListener(null));
        AtomicInteger calls = new AtomicInteger();
        ServiceListener l = calls::incrementAndGet;
        s.addListener(l);
        s.addListener(l);
        s.addMember("A");
        assertEquals(1, calls.get());
    }
}
