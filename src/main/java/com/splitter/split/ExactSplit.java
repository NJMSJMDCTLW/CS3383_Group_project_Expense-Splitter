package com.splitter.split;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Splits by exact amounts given in currency units (at most 2 decimals). No rounding. */
@AIGenerated(tool = "Claude Code", prompt = "P03")
public final class ExactSplit implements SplitStrategy {

    @Override
    public Map<Member, Money> split(Money total, List<ParticipantInput> inputs) {
        SplitSupport.checkCommon(total, inputs);
        Map<Member, Money> result = new LinkedHashMap<>();
        Money sum = Money.ZERO;
        for (ParticipantInput input : inputs) {
            SplitSupport.checkNotNegative(input, "amount");
            SplitSupport.checkAtMostTwoDecimals(input, "amount");
            Money amount;
            try {
                amount = Money.ofCents(input.value().movePointRight(2).longValueExact());
            } catch (ArithmeticException e) {
                throw new ValidationException("The amount of \"" + input.member().getName()
                        + "\" is too large.");
            }
            result.put(input.member(), amount);
            sum = sum.plus(amount);
        }
        if (!sum.equals(total)) {
            throw new ValidationException("The exact amounts (" + sum + ") must add up to the total ("
                    + total + ").");
        }
        return result;
    }

    @Override
    public SplitMethod method() {
        return SplitMethod.EXACT;
    }
}
