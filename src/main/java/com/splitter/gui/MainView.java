package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import java.nio.file.Path;
import java.util.Optional;

/** The main window (title, menu, dialogs) as seen by its presenter. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public interface MainView {

    void showGroupName(String name);

    void setUndoEnabled(boolean enabled);

    void showError(String message);

    /** Asks for the name of a new group; empty if the user cancels. */
    Optional<String> askGroupName();

    /** Asks whether to continue although "Unsaved changes will be lost". True means continue. */
    boolean confirmDiscardChanges();

    /** Lets the user choose a file to open; empty if the user cancels. */
    Optional<Path> chooseFileToOpen();

    /** Lets the user choose a file to save to; empty if the user cancels. */
    Optional<Path> chooseFileToSave();
}
