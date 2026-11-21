package com.splitter.split;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Splits in proportion to share counts (weights). At least one weight must be greater than zero. */
@AIGenerated(tool = "Claude Code", prompt = "P03", refinedBy = "Wang Man Nga", changes = "SHARES weights is limited to 2 decimal places, consistent to percentages")
public final class SharesSplit implements SplitStrategy {

    @Override
    public Map<Member, Money> split(Money total, List<ParticipantInput> inputs) {
        SplitSupport.checkCommon(total, inputs);
        BigDecimal sum = BigDecimal.ZERO;
        for (ParticipantInput input : inputs) {
            SplitSupport.checkNotNegative(input, "number of shares");
            SplitSupport.checkAtMostTwoDecimals(input, "number of shares");
            sum = sum.add(input.value());
        }
        if (sum.signum() == 0) {
            throw new ValidationException("At least one participant must have more than zero shares.");
        }
        int n = inputs.size();
        long[] base = new long[n];
        boolean[] eligible = new boolean[n];
        for (int i = 0; i < n; i++) {
            BigDecimal weight = inputs.get(i).value();
            base[i] = SplitSupport.floorShare(total, weight, sum);
            eligible[i] = weight.signum() > 0;
        }
        return SplitSupport.distribute(total, inputs, base, eligible);
    }

    @Override
    public SplitMethod method() {
        return SplitMethod.SHARES;
    }
}
