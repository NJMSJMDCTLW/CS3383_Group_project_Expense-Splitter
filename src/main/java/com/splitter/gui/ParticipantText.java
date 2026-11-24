package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;

/**
 * Raw text typed for one participant (an amount, a percentage or a number of shares).
 * The list order of these records is the participant order.
 */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public record ParticipantText(Member member, String text) {
}
