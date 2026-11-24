package com.splitter.split;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Package-private helpers shared by the strategies: common validation and leftover distribution. */
@AIGenerated(tool = "Claude Code", prompt = "P03")
final class SplitSupport {

    private SplitSupport() {
    }

    /** Checks what is common to all strategies. */
    static void checkCommon(Money total, List<ParticipantInput> inputs) {
        if (total == null) {
            throw new ValidationException("Amount must not be empty.");
        }
        if (total.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new ValidationException("At least one participant is required.");
        }
        Set<Member> seen = new HashSet<>();
        for (ParticipantInput input : inputs) {
            if (input == null) {
                throw new ValidationException("A participant entry must not be empty.");
            }
            if (!seen.add(input.member())) {
                throw new ValidationException("\"" + input.member().getName()
                        + "\" appears more than once in the participant list.");
            }
        }
    }

    static void checkNotNegative(ParticipantInput input, String what) {
        if (input.value().signum() < 0) {
            throw new ValidationException("The " + what + " of \"" + input.member().getName()
                    + "\" must not be negative.");
        }
    }

    /** Accepts 12.5 and 12.500 (numerically 12.5) but rejects 12.505. */
    static void checkAtMostTwoDecimals(ParticipantInput input, String what) {
        if (input.value().stripTrailingZeros().scale() > 2) {
            throw new ValidationException("The " + what + " of \"" + input.member().getName()
                    + "\" must have at most 2 decimal places.");
        }
    }

    /**
     * Gives the leftover cents (total minus the sum of the base amounts) one cent each, in participant
     * order, starting from the first participant, only to participants marked eligible. Returns the
     * ordered result map.
     */
    static Map<Member, Money> distribute(Money total, List<ParticipantInput> inputs, long[] base,
                                         boolean[] eligible) {
        long sum = 0;
        for (long amount : base) {
            sum += amount;
        }
        long leftover = total.cents() - sum;
        for (int i = 0; i < base.length && leftover > 0; i++) {
            if (eligible[i]) {
                base[i]++;
                leftover--;
            }
        }
        if (leftover != 0) {
            throw new IllegalStateException("Split rounding error: " + leftover + " cents left over.");
        }
        Map<Member, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < base.length; i++) {
            result.put(inputs.get(i).member(), Money.ofCents(base[i]));
        }
        return result;
    }

    /** Floor of {@code total * numerator / denominator}, computed without intermediate rounding. */
    static long floorShare(Money total, BigDecimal numerator, BigDecimal denominator) {
        return BigDecimal.valueOf(total.cents())
                .multiply(numerator)
                .divide(denominator, 0, java.math.RoundingMode.FLOOR)
                .longValueExact();
    }
}
