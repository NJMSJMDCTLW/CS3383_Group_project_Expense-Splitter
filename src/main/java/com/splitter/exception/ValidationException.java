package com.splitter.exception;

import com.splitter.meta.AIGenerated;

/**
 * Thrown when input is rejected. The message is meant to be shown to the user as it is.
 * Unchecked, so it can be thrown from the model and split packages without declaring it.
 */
@AIGenerated(tool = "Claude Code", prompt = "P02")
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
