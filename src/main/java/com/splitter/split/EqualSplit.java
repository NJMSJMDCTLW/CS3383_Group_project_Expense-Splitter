package com.splitter.split;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Splits the total equally. The values of the inputs are ignored. */
@AIGenerated(tool = "Claude Code", prompt = "P03")
public final class EqualSplit implements SplitStrategy {

    @Override
    public Map<Member, Money> split(Money total, List<ParticipantInput> inputs) {
        SplitSupport.checkCommon(total, inputs);
        int n = inputs.size();
        long[] base = new long[n];
        Arrays.fill(base, total.cents() / n);
        boolean[] eligible = new boolean[n];
        Arrays.fill(eligible, true);
        return SplitSupport.distribute(total, inputs, base, eligible);
    }

    @Override
    public SplitMethod method() {
        return SplitMethod.EQUAL;
    }
}
