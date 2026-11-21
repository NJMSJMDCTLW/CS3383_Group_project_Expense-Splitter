package com.splitter.split;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Splits by percentages (at most 2 decimals) that must add up to exactly 100. */
@AIGenerated(tool = "Claude Code", prompt = "P03")
public final class PercentageSplit implements SplitStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public Map<Member, Money> split(Money total, List<ParticipantInput> inputs) {
        SplitSupport.checkCommon(total, inputs);
        BigDecimal sum = BigDecimal.ZERO;
        for (ParticipantInput input : inputs) {
            SplitSupport.checkNotNegative(input, "percentage");
            SplitSupport.checkAtMostTwoDecimals(input, "percentage");
            sum = sum.add(input.value());
        }
        if (sum.compareTo(HUNDRED) != 0) {
            throw new ValidationException("The percentages add up to " + sum.stripTrailingZeros().toPlainString()
                    + " but must add up to exactly 100.");
        }
        int n = inputs.size();
        long[] base = new long[n];
        boolean[] eligible = new boolean[n];
        for (int i = 0; i < n; i++) {
            BigDecimal pct = inputs.get(i).value();
            base[i] = SplitSupport.floorShare(total, pct, HUNDRED);
            eligible[i] = pct.signum() > 0;
        }
        return SplitSupport.distribute(total, inputs, base, eligible);
    }

    @Override
    public SplitMethod method() {
        return SplitMethod.PERCENTAGE;
    }
}
