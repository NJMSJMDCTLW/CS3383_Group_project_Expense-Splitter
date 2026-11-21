package com.splitter.split;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.SplitMethod;

/** Creates the {@link SplitStrategy} for a {@link SplitMethod}. */
@AIGenerated(tool = "Claude Code", prompt = "P03")
public final class SplitStrategyFactory {

    private SplitStrategyFactory() {
    }

    /** @throws ValidationException if {@code m} is null */
    public static SplitStrategy forMethod(SplitMethod m) {
        if (m == null) {
            throw new ValidationException("A split method is required.");
        }
        switch (m) {
            case EQUAL:
                return new EqualSplit();
            case EXACT:
                return new ExactSplit();
            case PERCENTAGE:
                return new PercentageSplit();
            case SHARES:
                return new SharesSplit();
            default:
                throw new ValidationException("Unknown split method: " + m);
        }
    }
}
