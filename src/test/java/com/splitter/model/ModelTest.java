package com.splitter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Package model, 10 test cases. Techniques: boundary value analysis, equivalence classes and a decision
 * table across {@link Money}, {@link Member}, {@link Group}, {@link Expense} and {@link Transfer}
 * (DESIGN.md 2.1-2.6).
 */
@AIGenerated(tool = "Claude Code", prompt = "T02")
class ModelTest {

    private static Expense expense(int id, Member payer, Member participant, long cents) {
        Map<Member, Money> shares = new LinkedHashMap<>();
        shares.put(participant, Money.ofCents(cents));
        return new Expense(id, "d", payer, Money.ofCents(cents), SplitMethod.EQUAL, shares);
    }

    @Test
    @DisplayName("TC-M01 Money.parse valid forms and 2-decimal output (equivalence 2.1)")
    void moneyParseAndFormat() {
        assertEquals(1200L, Money.parse("12").cents());
        assertEquals(1250L, Money.parse("12.5").cents());
        assertEquals(1250L, Money.parse("12.50").cents());
        assertEquals("1.00", Money.ofCents(100).toString());
        assertEquals("-0.05", Money.ofCents(-5).toString());
    }

    @Test
    @DisplayName("TC-M02 Money.parse rejects empty/negative/3-decimal/text/overflow (equivalence 2.1)")
    void moneyParseRejects() {
        assertThrows(ValidationException.class, () -> Money.parse(null));
        assertThrows(ValidationException.class, () -> Money.parse("   "));
        assertThrows(ValidationException.class, () -> Money.parse("-1"));
        assertThrows(ValidationException.class, () -> Money.parse("1.234"));
        assertThrows(ValidationException.class, () -> Money.parse("abc"));
        assertThrows(ValidationException.class, () -> Money.parse("92233720368547758.08"));
    }

    @Test
    @DisplayName("TC-M03 Money arithmetic and overflow (equivalence/boundary 2.1)")
    void moneyArithmetic() {
        assertEquals(Money.ofCents(1300), Money.parse("12.34").plus(Money.parse("0.66")));
        assertEquals(-1, Money.parse("0.05").minus(Money.parse("0.10")).signum());
        assertEquals(Money.ofCents(-1234), Money.parse("12.34").negate());
        assertThrows(ValidationException.class, () -> Money.ofCents(Long.MAX_VALUE).plus(Money.ofCents(1)));
    }

    @Test
    @DisplayName("TC-D01 member name trimmed, 30 ok, 31 rejected (boundary 2.2)")
    void memberNameBoundary() {
        assertEquals("Bob", new Member("  Bob  ").getName());
        assertEquals("a".repeat(30), new Member("a".repeat(30)).getName());
        assertThrows(ValidationException.class, () -> new Member("a".repeat(31)));
    }

    @Test
    @DisplayName("TC-D02 member blank/null/line break rejected; identity by case-insensitive key (2.2)")
    void memberValidationAndIdentity() {
        assertThrows(ValidationException.class, () -> new Member(" "));
        assertThrows(ValidationException.class, () -> new Member(null));
        assertThrows(ValidationException.class, () -> new Member("a\nb"));
        assertEquals(new Member("Ann"), new Member(" ann "));
        assertNotEquals(new Member("Ann"), new Member("Bob"));
    }

    @Test
    @DisplayName("TC-D03 addMember rejects duplicate key (decision table 2.3)")
    void groupDuplicateRejected() {
        Group g = new Group("G");
        g.addMember(new Member("Ann"));
        assertThrows(ValidationException.class, () -> g.addMember(new Member("ann")));
    }

    @Test
    @DisplayName("TC-D04 removeMember blocked for payer/participant; unused removed (decision table 2.3/N1)")
    void removeMemberDecisionTable() {
        Member a = new Member("A");
        Member b = new Member("B");
        Group g = new Group("G");
        g.addMember(a);
        g.addMember(b);
        g.addExpense(expense(1, a, b, 100));
        assertThrows(ValidationException.class, () -> g.removeMember(a));
        assertThrows(ValidationException.class, () -> g.removeMember(b));
        Group g2 = new Group("G");
        g2.addMember(a);
        g2.removeMember(a);
        assertEquals(List.of(), g2.getMembers());
    }

    @Test
    @DisplayName("TC-D05 ids monotonic, sorted by id, restoreNextExpenseId bounds (state/boundary 2.3)")
    void idAllocationAndRestore() {
        Member a = new Member("A");
        Group g = new Group("G");
        g.addMember(a);
        assertEquals(1, g.allocateExpenseId());
        g.addExpense(expense(2, a, a, 100));
        g.addExpense(expense(1, a, a, 100));
        assertEquals(List.of(1, 2), g.getExpenses().stream().map(Expense::getId).toList());
        assertThrows(ValidationException.class, () -> g.restoreNextExpenseId(2));
        g.restoreNextExpenseId(3);
        assertEquals(3, g.getNextExpenseId());
    }

    @Test
    @DisplayName("TC-D06 Expense total bounds 1,000,000.00 ok / +1 and 0 rejected (boundary 2.5)")
    void expenseTotalBounds() {
        Member a = new Member("A");
        assertEquals(Expense.MAX_TOTAL_CENTS,
                new Expense(1, "d", a, Money.ofCents(Expense.MAX_TOTAL_CENTS), SplitMethod.EQUAL,
                        Map.of(a, Money.ofCents(Expense.MAX_TOTAL_CENTS))).getTotal().cents());
        assertThrows(ValidationException.class, () -> new Expense(1, "d", a,
                Money.ofCents(Expense.MAX_TOTAL_CENTS + 1), SplitMethod.EQUAL,
                Map.of(a, Money.ofCents(Expense.MAX_TOTAL_CENTS + 1))));
        assertThrows(ValidationException.class, () -> new Expense(1, "d", a, Money.ZERO,
                SplitMethod.EQUAL, Map.of(a, Money.ZERO)));
    }

    @Test
    @DisplayName("TC-D07 Transfer rejects from==to / non-positive; toString has no symbol (2.6)")
    void transferRules() {
        Member a = new Member("A");
        Member b = new Member("B");
        assertThrows(ValidationException.class, () -> new Transfer(a, a, Money.ofCents(100)));
        assertThrows(ValidationException.class, () -> new Transfer(a, b, Money.ZERO));
        assertEquals("A pays B 12.34", new Transfer(a, b, Money.ofCents(1234)).toString());
    }

    @Test
    @DisplayName("TC-D08 Group name blank/null/line break rejected (equivalence 2.3)")
    void groupNameValidation() {
        assertThrows(ValidationException.class, () -> new Group("  "));
        assertThrows(ValidationException.class, () -> new Group(null));
        assertThrows(ValidationException.class, () -> new Group("a\nb"));
        assertEquals("Trip", new Group("Trip").getName());
    }

    @Test
    @DisplayName("TC-D09 Expense rejects blank/line-break description (equivalence 2.5)")
    void expenseDescriptionValidation() {
        Member a = new Member("A");
        assertThrows(ValidationException.class, () -> new Expense(1, "  ", a, Money.ofCents(100),
                SplitMethod.EQUAL, Map.of(a, Money.ofCents(100))));
        assertThrows(ValidationException.class, () -> new Expense(1, "a\nb", a, Money.ofCents(100),
                SplitMethod.EQUAL, Map.of(a, Money.ofCents(100))));
    }

    @Test
    @DisplayName("TC-D10 Expense rejects empty shares, negative share, sum != total (equivalence 2.5)")
    void expenseShareValidation() {
        Member a = new Member("A");
        Member b = new Member("B");
        assertThrows(ValidationException.class, () -> new Expense(1, "d", a, Money.ofCents(100),
                SplitMethod.EQUAL, Map.of()));
        Map<Member, Money> negative = new LinkedHashMap<>();
        negative.put(a, Money.ofCents(200));
        negative.put(b, Money.ofCents(-100));
        assertThrows(ValidationException.class, () -> new Expense(1, "d", a, Money.ofCents(100),
                SplitMethod.EQUAL, negative));
        assertThrows(ValidationException.class, () -> new Expense(1, "d", a, Money.ofCents(100),
                SplitMethod.EQUAL, Map.of(a, Money.ofCents(90))));
    }

    @Test
    @DisplayName("TC-D11 Group.addExpense rejects payer/participant not a member (equivalence 2.3)")
    void addExpenseMembership() {
        Member a = new Member("A");
        Group g = new Group("G");
        g.addMember(a);
        assertThrows(ValidationException.class, () -> g.addExpense(expense(1, new Member("X"), a, 100)));
        assertThrows(ValidationException.class, () -> g.addExpense(expense(1, a, new Member("X"), 100)));
    }
}
