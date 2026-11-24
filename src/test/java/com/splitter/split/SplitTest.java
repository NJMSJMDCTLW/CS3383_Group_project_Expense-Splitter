package com.splitter.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Package split, 10 test cases. Techniques: canonical examples from the rounding rule, equivalence classes
 * and a decision table for the factory (DESIGN.md 3.1-3.2).
 */
@AIGenerated(tool = "Claude Code", prompt = "T02")
class SplitTest {

    private static final Member A = new Member("A");
    private static final Member B = new Member("B");
    private static final Member C = new Member("C");

    private static ParticipantInput in(Member m, String value) {
        return new ParticipantInput(m, new BigDecimal(value));
    }

    @Test
    @DisplayName("TC-S01 EQUAL 10c/3 -> 4,3,3 (canonical leftover example 3.1)")
    void equalTenCents() {
        Map<Member, Money> r = new EqualSplit().split(Money.ofCents(10), List.of(in(A, "0"), in(B, "0"), in(C, "0")));
        assertEquals(Money.ofCents(4), r.get(A));
        assertEquals(Money.ofCents(3), r.get(B));
        assertEquals(Money.ofCents(3), r.get(C));
    }

    @Test
    @DisplayName("TC-S02 EQUAL 100.00/3 -> 33.34,33.33,33.33 (canonical leftover example 3.1)")
    void equalHundredYuan() {
        Map<Member, Money> r = new EqualSplit().split(Money.parse("100.00"),
                List.of(in(A, "0"), in(B, "0"), in(C, "0")));
        assertEquals(Money.parse("33.34"), r.get(A));
        assertEquals(Money.parse("33.33"), r.get(B));
        assertEquals(Money.parse("33.33"), r.get(C));
    }

    @Test
    @DisplayName("TC-S03 EQUAL rejects empty and duplicate participant (equivalence class 3.1)")
    void equalRejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> new EqualSplit().split(Money.ofCents(100), List.of()));
        assertThrows(ValidationException.class,
                () -> new EqualSplit().split(Money.ofCents(100), List.of(in(A, "0"), in(A, "0"))));
    }

    @Test
    @DisplayName("TC-S04 EXACT 30+70=100 ok; 60+30 rejected (equivalence class 3.1)")
    void exactSum() {
        Map<Member, Money> r = new ExactSplit().split(Money.parse("100.00"),
                List.of(in(A, "30"), in(B, "70")));
        assertEquals(Money.parse("30.00"), r.get(A));
        assertThrows(ValidationException.class,
                () -> new ExactSplit().split(Money.parse("100.00"), List.of(in(A, "60"), in(B, "30"))));
    }

    @Test
    @DisplayName("TC-S05 EXACT rejects 3 decimals and negative (boundary + equivalence 3.1)")
    void exactRejectsInvalid() {
        assertThrows(ValidationException.class,
                () -> new ExactSplit().split(Money.parse("100.00"), List.of(in(A, "12.345"), in(B, "87.655"))));
        assertThrows(ValidationException.class,
                () -> new ExactSplit().split(Money.parse("100.00"), List.of(in(A, "-10"), in(B, "110"))));
    }

    @Test
    @DisplayName("TC-S06 PERCENTAGE 99c on 50/30/20 -> 50,30,19 (canonical leftover 3.1)")
    void percentageLeftover() {
        Map<Member, Money> r = new PercentageSplit().split(Money.ofCents(99),
                List.of(in(A, "50"), in(B, "30"), in(C, "20")));
        assertEquals(Money.ofCents(50), r.get(A));
        assertEquals(Money.ofCents(30), r.get(B));
        assertEquals(Money.ofCents(19), r.get(C));
    }

    @Test
    @DisplayName("TC-S07 PERCENTAGE 80/0/20 on 99c -> 80,0,19 (zero weight stays zero 3.1)")
    void percentageZeroWeight() {
        Map<Member, Money> r = new PercentageSplit().split(Money.ofCents(99),
                List.of(in(A, "80"), in(B, "0"), in(C, "20")));
        assertEquals(Money.ofCents(80), r.get(A));
        assertEquals(Money.ZERO, r.get(B));
        assertEquals(Money.ofCents(19), r.get(C));
    }

    @Test
    @DisplayName("TC-S08 PERCENTAGE rejects 99.99, 100.01 and 33.33x3 (exact sum, no tolerance 3.1)")
    void percentageSumRejected() {
        assertThrows(ValidationException.class, () -> new PercentageSplit().split(Money.ofCents(100), List.of(in(A, "99.99"))));
        assertThrows(ValidationException.class, () -> new PercentageSplit().split(Money.ofCents(100),
                List.of(in(A, "50.01"), in(B, "50"))));
        assertThrows(ValidationException.class, () -> new PercentageSplit().split(Money.ofCents(100),
                List.of(in(A, "33.33"), in(B, "33.33"), in(C, "33.33"))));
    }

    @Test
    @DisplayName("TC-S09 SHARES 0,1,1 on 101c -> 0,51,50 (canonical 3.1); all zero rejected")
    void sharesLeftover() {
        Map<Member, Money> r = new SharesSplit().split(Money.ofCents(101),
                List.of(in(A, "0"), in(B, "1"), in(C, "1")));
        assertEquals(Money.ZERO, r.get(A));
        assertEquals(Money.ofCents(51), r.get(B));
        assertEquals(Money.ofCents(50), r.get(C));
        assertThrows(ValidationException.class,
                () -> new SharesSplit().split(Money.ofCents(100), List.of(in(A, "0"), in(B, "0"))));
    }

    @Test
    @DisplayName("TC-S10 factory maps each method (decision table 3.2); SHARES 1.005 rejected (R1)")
    void factoryAndSharesDecimalLimit() {
        assertInstanceOf(EqualSplit.class, SplitStrategyFactory.forMethod(SplitMethod.EQUAL));
        assertInstanceOf(ExactSplit.class, SplitStrategyFactory.forMethod(SplitMethod.EXACT));
        assertInstanceOf(PercentageSplit.class, SplitStrategyFactory.forMethod(SplitMethod.PERCENTAGE));
        assertInstanceOf(SharesSplit.class, SplitStrategyFactory.forMethod(SplitMethod.SHARES));
        assertThrows(ValidationException.class, () -> SplitStrategyFactory.forMethod(null));
        assertThrows(ValidationException.class,
                () -> new SharesSplit().split(Money.ofCents(100), List.of(in(A, "1.005"), in(B, "1"))));
    }

    @Test
    @DisplayName("TC-S11 EQUAL 100 cents / 3 -> 34,33,33 (leftover to first, 3.1)")
    void equalHundredCents() {
        Map<Member, Money> r = new EqualSplit().split(Money.ofCents(100),
                List.of(in(A, "0"), in(B, "0"), in(C, "0")));
        assertEquals(Money.ofCents(34), r.get(A));
        assertEquals(Money.ofCents(33), r.get(B));
        assertEquals(Money.ofCents(33), r.get(C));
    }

    @Test
    @DisplayName("TC-S12 PERCENTAGE rejects 3 decimals and negative (boundary/equivalence 3.1)")
    void percentageInvalidValues() {
        assertThrows(ValidationException.class, () -> new PercentageSplit().split(Money.ofCents(100),
                List.of(in(A, "33.333"), in(B, "66.667"))));
        assertThrows(ValidationException.class, () -> new PercentageSplit().split(Money.ofCents(100),
                List.of(in(A, "-10"), in(B, "110"))));
    }

    @Test
    @DisplayName("TC-S13 SHARES rejects negative weight (equivalence 3.1)")
    void sharesNegativeRejected() {
        assertThrows(ValidationException.class, () -> new SharesSplit().split(Money.ofCents(100),
                List.of(in(A, "-1"), in(B, "2"))));
    }

    @Test
    @DisplayName("TC-S14 EXACT rejects amount too large to fit in cents (boundary 3.1)")
    void exactTooLargeRejected() {
        assertThrows(ValidationException.class, () -> new ExactSplit().split(Money.parse("100.00"),
                List.of(in(A, "92233720368547758.08"))));
    }
}
