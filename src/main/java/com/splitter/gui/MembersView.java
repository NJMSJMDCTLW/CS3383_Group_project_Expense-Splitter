package com.splitter.gui;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import java.util.List;

/**
 * The Members tab as seen by its presenter.
 */
@AIGenerated(tool = "Claude Code", prompt = "P07", refinedBy = "Dora", changes = "Fix #13: provide a method to clear the member name field on New/Open.")
public interface MembersView {

    void showMembers(List<Member> members);

    void showError(String message);

    /**
     * Clears the member name input field when the current group is replaced.
     */
    default void clearNameField() {
    }
}
