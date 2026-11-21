package com.splitter.model;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import java.util.Locale;

/**
 * Immutable group member. Identity is the trimmed name lower-cased with {@link Locale#ROOT};
 * {@link #getName()} keeps the user's own casing.
 */
@AIGenerated(tool = "Claude Code", prompt = "P02")
public final class Member {

    private static final int MAX_NAME_LENGTH = 30;

    private final String name;
    private final String key;

    /**
     * @throws ValidationException if the name is null, blank, longer than 30 characters
     *                             (after trimming) or contains a line break
     */
    public Member(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Member name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Member name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            throw new ValidationException("Member name must not contain line breaks.");
        }
        this.name = trimmed;
        this.key = trimmed.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Member && ((Member) o).key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
