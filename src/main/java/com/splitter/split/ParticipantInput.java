package com.splitter.split;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import java.math.BigDecimal;

/**
 * One participant of a split. The meaning of {@code value} depends on the strategy: ignored for EQUAL,
 * an amount for EXACT, a percent for PERCENTAGE and a weight for SHARES.
 *
 * @throws ValidationException if the member or the value is null
 */
@AIGenerated(tool = "Claude Code", prompt = "P03")
public record ParticipantInput(Member member, BigDecimal value) {

    public ParticipantInput {
        if (member == null) {
            throw new ValidationException("A participant needs a member.");
        }
        if (value == null) {
            throw new ValidationException("A participant needs a value.");
        }
    }
}
