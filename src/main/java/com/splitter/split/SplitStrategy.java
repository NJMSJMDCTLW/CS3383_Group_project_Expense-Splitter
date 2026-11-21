package com.splitter.split;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.util.List;
import java.util.Map;

/** Strategy for dividing an expense total between participants. */
@AIGenerated(tool = "Claude Code", prompt = "P03")
public interface SplitStrategy {

    /**
     * Divides {@code total} between the participants. The list order is the participant order and the
     * rounding order. The returned map keeps that order and its values always sum exactly to
     * {@code total}.
     *
     * @throws com.splitter.exception.ValidationException if the arguments are invalid for this strategy
     */
    Map<Member, Money> split(Money total, List<ParticipantInput> inputs);

    /** The split method this strategy implements. */
    SplitMethod method();
}
