package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import java.util.List;

/** The Members tab as seen by its presenter. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public interface MembersView {

    void showMembers(List<Member> members);

    void showError(String message);
}
