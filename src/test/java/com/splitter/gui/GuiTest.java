
package com.splitter.gui;

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
import com.splitter.service.ExpenseService;
import com.splitter.service.ExpenseServiceImpl;
import com.splitter.split.ParticipantInput;
import com.splitter.storage.Repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Package gui, 14 test cases. Techniques: equivalence classes for
 * {@link InputParser} and {@link MoneyFormatter}, and fake-view branch
 * checks for the presenters (DESIGN.md 4.4).
 */
@AIGenerated(tool = "Claude Code", prompt = "T02")
class GuiTest {

    private static final class FakeRepository implements Repository {

        Group stored;

        @Override
        public void save(Group g, Path file) {
            stored = g;
        }

        @Override
        public Group load(Path file) throws StorageException {
            if (stored == null) {
                throw new StorageException("empty", null);
            }

            return stored;
        }
    }

    private static final class RecordingExpensesView implements ExpensesView {

        int cleared;
        String error;

        List<Expense> shownExpenses = new ArrayList<>();
        List<Member> shownMembers = new ArrayList<>();

        @Override
        public void showMembers(List<Member> members) {
            shownMembers = new ArrayList<>(members);
        }

        @Override
        public void showExpenses(List<Expense> expenses) {
            shownExpenses = new ArrayList<>(expenses);
        }

        @Override
        public void clearForm() {
            cleared++;
        }

        @Override
        public void showError(String message) {
            error = message;
        }
    }

    private static final class RecordingMainView implements MainView {

        boolean confirm;
        int confirmCalls;

        Optional<String> newName = Optional.of("Trips");
        Optional<Path> openFile = Optional.empty();

        String error;
        String groupName;
        boolean undoEnabled;

        @Override
        public void showGroupName(String name) {
            groupName = name;
        }

        @Override
        public void setUndoEnabled(boolean enabled) {
            undoEnabled = enabled;
        }

        @Override
        public void showError(String message) {
            error = message;
        }

        @Override
        public Optional<String> askGroupName() {
            return newName;
        }

        @Override
        public boolean confirmDiscardChanges() {
            confirmCalls++;
            return confirm;
        }

        @Override
        public Optional<Path> chooseFileToOpen() {
            return openFile;
        }

        @Override
        public Optional<Path> chooseFileToSave() {
            return Optional.empty();
        }
    }

    private static ExpenseService freshService() {
        return new com.splitter.service.ExpenseServiceImpl(
                new FakeRepository(),
                new CommandHistory());
    }

    @Test
    @DisplayName("TC-G01 parseMoney accepts a valid amount and trims spaces (equivalence)")
    void parseMoneyValid() {
        InputParser p = new InputParser();

        assertEquals(Money.parse("12.50"), p.parseMoney("  12.50 "));
        assertThrows(ValidationException.class, () -> p.parseMoney("abc"));
    }

    @Test
    @DisplayName("TC-G02 parseMoney rejects '-1' and '0.005' (equivalence/boundary)")
    void parseMoneyRejects() {
        InputParser p = new InputParser();

        assertThrows(ValidationException.class, () -> p.parseMoney("-1"));
        assertThrows(ValidationException.class, () -> p.parseMoney("0.005"));
        assertThrows(ValidationException.class, () -> p.parseMoney("abc"));
    }

    @Test
    @DisplayName("TC-G03 parseDecimal accepts signed decimals and rejects text/blank (equivalence)")
    void parseDecimal() {
        InputParser p = new InputParser();

        assertEquals(
                0,
                p.parseDecimal("-1").compareTo(java.math.BigDecimal.valueOf(-1)));

        assertEquals(
                java.math.BigDecimal.valueOf(2.5).stripTrailingZeros(),
                p.parseDecimal(" 2.5 ").stripTrailingZeros());

        assertThrows(ValidationException.class, () -> p.parseDecimal("abc"));
        assertThrows(ValidationException.class, () -> p.parseDecimal(" "));
    }

    @Test
    @DisplayName("TC-G04 parseInputs rejects blank values for EXACT (equivalence)")
    void parseInputsRejectsBlankExact() {
        InputParser p = new InputParser();

        List<ParticipantText> texts = List.of(
                new ParticipantText(new Member("A"), ""),
                new ParticipantText(new Member("B"), "5"));

        assertThrows(
                ValidationException.class,
                () -> p.parseInputs(texts, SplitMethod.EXACT));
    }

    @Test
    @DisplayName("TC-G05 parseMethod matches display name case-insensitively, unknown rejected (equivalence)")
    void parseMethod() {
        InputParser p = new InputParser();

        assertEquals(SplitMethod.EQUAL, p.parseMethod("equal"));
        assertEquals(SplitMethod.SHARES, p.parseMethod("Shares"));

        assertThrows(
                ValidationException.class,
                () -> p.parseMethod("nonsense"));
    }

    @Test
    @DisplayName("TC-G06 MoneyFormatter shows $ and -$ for negatives, no symbol in Transfer (equivalence)")
    void moneyFormatter() {
        MoneyFormatter f = new MoneyFormatter();

        assertEquals("$12.34", f.format(Money.ofCents(1234)));
        assertEquals("-$0.05", f.format(Money.ofCents(-5)));

        Member a = new Member("A");
        Member b = new Member("B");

        assertEquals(
                "A pays B $12.34",
                f.format(new Transfer(a, b, Money.ofCents(1234))));
    }

    @Test
    @DisplayName("TC-G07 ExpensesPresenter clears the form only after a successful add (branch)")
    void expensesClearsOnlyOnSuccess() {
        ExpenseService s = freshService();

        s.addMember("A");

        Member a = s.getMembers().get(0);

        RecordingExpensesView view = new RecordingExpensesView();
        ExpensesPresenter presenter = new ExpensesPresenter(s, view);

        presenter.start();

        presenter.onAddExpense(
                "",
                a,
                "1.00",
                SplitMethod.EQUAL,
                List.of(new ParticipantText(a, "")));

        assertEquals(0, view.cleared);
        assertTrue(view.error != null && !view.error.isBlank());

        presenter.onAddExpense(
                "ok",
                a,
                "1.00",
                SplitMethod.EQUAL,
                List.of(new ParticipantText(a, "")));

        assertEquals(1, view.cleared);
    }

    @Test
    @DisplayName("TC-G08 ExpensesPresenter reports remove errors via showError (branch)")
    void expensesRemoveError() {
        ExpenseService s = freshService();

        RecordingExpensesView view = new RecordingExpensesView();
        ExpensesPresenter presenter = new ExpensesPresenter(s, view);

        presenter.start();
        presenter.onRemoveSelected(99);

        assertTrue(view.error != null && !view.error.isBlank());
    }

    @Test
    @DisplayName("TC-G09 MainPresenter confirms before New when data exists, not when empty (decision table)")
    void mainConfirmBeforeNew() {
        ExpenseService s = freshService();

        RecordingMainView view = new RecordingMainView();
        MainPresenter presenter = new MainPresenter(s, view);

        presenter.start();
        presenter.onNew();

        assertEquals(0, view.confirmCalls);

        s.addMember("A");
        presenter.onNew();

        assertEquals(1, view.confirmCalls);
    }

    @Test
    @DisplayName("TC-G10 MainPresenter confirms before Open with data; declining keeps data (branch)")
    void mainConfirmBeforeOpen() {
        ExpenseService s = freshService();

        s.addMember("A");

        RecordingMainView view = new RecordingMainView();
        view.confirm = false;

        MainPresenter presenter = new MainPresenter(s, view);

        presenter.start();
        presenter.onOpen();

        assertEquals(1, view.confirmCalls);
        assertFalse(s.getMembers().isEmpty());
    }

    @Test
    @DisplayName("TC-G11 InputParser rejects '' and huge amount (equivalence/boundary)")
    void parseMoneyEmptyAndHuge() {
        InputParser p = new InputParser();

        assertThrows(ValidationException.class, () -> p.parseMoney(""));
        assertThrows(
                ValidationException.class,
                () -> p.parseMoney("99999999999999999.99"));
    }

    @Test
    @DisplayName("TC-G12 MembersPresenter adds/removes members and reports errors (branch)")
    void membersPresenter() {
        ExpenseService s = freshService();

        final String[] error = { null };
        final List<List<Member>> shown = new ArrayList<>();

        MembersView view = new MembersView() {

            @Override
            public void showMembers(List<Member> members) {
                shown.add(new ArrayList<>(members));
            }

            @Override
            public void showError(String message) {
                error[0] = message;
            }
        };

        MembersPresenter presenter = new MembersPresenter(s, view);

        presenter.start();
        presenter.onAddMember("A");

        assertEquals(1, shown.get(shown.size() - 1).size());

        presenter.onAddMember("A");

        assertTrue(error[0] != null && !error[0].isBlank());

        presenter.onRemoveMember(s.getMembers().get(0));

        assertEquals(0, shown.get(shown.size() - 1).size());
    }

    @Test
    @DisplayName("TC-G13 BalancesPresenter and SettleUpPresenter show data on start (branch)")
    void balancesAndSettlePresenters() {
        ExpenseService s = freshService();

        s.addMember("A");

        final int[] balanceCalls = { 0 };
        final int[] settleCalls = { 0 };

        BalancesView bv = new BalancesView() {

            @Override
            public void showBalances(Map<Member, Money> balances) {
                balanceCalls[0]++;
            }

            @Override
            public void showError(String message) {
            }
        };

        SettleUpView sv = new SettleUpView() {

            @Override
            public void showSettlements(List<Transfer> transfers) {
                settleCalls[0]++;
            }

            @Override
            public void showError(String message) {
            }
        };

        new BalancesPresenter(s, bv).start();
        new SettleUpPresenter(s, sv).start();

        assertEquals(1, balanceCalls[0]);
        assertEquals(1, settleCalls[0]);
    }

    @Test
    @DisplayName("TC-G14 parseInputs accepts blank values for EQUAL (equivalence)")
    void parseInputsBlankIsZeroForEqual() {
        InputParser parser = new InputParser();

        List<ParticipantInput> inputs = parser.parseInputs(
                List.of(
                        new ParticipantText(new Member("A"), ""),
                        new ParticipantText(new Member("B"), "")),
                SplitMethod.EQUAL);

        assertEquals(0, inputs.get(0).value().signum());
        assertEquals(0, inputs.get(1).value().signum());
    }

    @Test
    @DisplayName("TC-V15 adding an expense to an empty group shows a helpful validation error")
    void addExpenseToEmptyGroup() {

        ExpenseServiceImpl s = new ExpenseServiceImpl(
                new FakeRepository(),
                new CommandHistory());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> s.addExpense(
                        "Dinner",
                        null,
                        Money.parse("100.00"),
                        SplitMethod.EQUAL,
                        List.of()));

        assertEquals(
                "Please add at least one member before creating an expense.",
                exception.getMessage());

        assertTrue(s.getMembers().isEmpty());
        assertTrue(s.getExpenses().isEmpty());
    }

    @Test
    @DisplayName("TC-G16 ExpensesPresenter clears the form when group is replaced (New/Open)")
    void expensesPresenterClearsFormOnGroupReplacement() throws StorageException {

        FakeRepository repository = new FakeRepository();

        ExpenseService s = new com.splitter.service.ExpenseServiceImpl(
                repository,
                new CommandHistory());

        RecordingExpensesView view = new RecordingExpensesView();
        ExpensesPresenter presenter = new ExpensesPresenter(s, view);

        presenter.start();

        // Normal member changes should not clear the expense form.
        s.addMember("Alice");
        assertEquals(0, view.cleared);

        // Test New.
        s.newGroup("Group B");
        assertEquals(1, view.cleared);

        // Prepare a group to be opened.
        Group openedGroup = new Group("Group C");
        openedGroup.addMember(new Member("Charlie"));

        repository.stored = openedGroup;

        // Test Open.
        s.open(Path.of("group-c.csv"));

        assertEquals(2, view.cleared);
        assertEquals(
                List.of(new Member("Charlie")),
                view.shownMembers);

        presenter.stop();
    }

}
