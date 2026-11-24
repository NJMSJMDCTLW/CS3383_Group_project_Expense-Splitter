package com.splitter.exception;

import com.splitter.meta.AIGenerated;

/**
 * Thrown when saving or loading a group fails (I/O error or a corrupt, empty or wrong-version file).
 * Checked, so callers must handle it. The message is meant to be shown to the user as it is.
 */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public class StorageException extends Exception {

    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
