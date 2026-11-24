package com.splitter.gui;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import com.splitter.split.ParticipantInput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Turns the text typed in the GUI into typed values. Every failure is a {@link ValidationException}. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public class InputParser {

    private static final Pattern DECIMAL = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

    /** Parses an amount such as "12.50"; surrounding spaces are ignored. Uses {@link Money#parse}. */
    public Money parseMoney(String text) {
        return Money.parse(text == null ? null : text.trim());
    }

    /** Parses a plain decimal number such as "30", "33.33" or "-1"; surrounding spaces are ignored. */
    public BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Please enter a number.");
        }
        String trimmed = text.trim();
        if (!DECIMAL.matcher(trimmed).matches()) {
            throw new ValidationException("\"" + trimmed + "\" is not a valid number.");
        }
        return new BigDecimal(trimmed);
    }

    /** Finds the split method whose display name is {@code displayName}. */
    public SplitMethod parseMethod(String displayName) {
        if (displayName != null) {
            String trimmed = displayName.trim();
            for (SplitMethod method : SplitMethod.values()) {
                if (method.getDisplayName().equalsIgnoreCase(trimmed)) {
                    return method;
                }
            }
        }
        throw new ValidationException("Please choose a split method.");
    }

    /**
     * Converts the typed texts to participant inputs, keeping the order. A blank text becomes zero
     * (so the empty fields of an equal split work); any other text must be a valid number.
     */
    public List<ParticipantInput> parseInputs(List<ParticipantText> texts) {
        if (texts == null) {
            throw new ValidationException("At least one participant is required.");
        }
        List<ParticipantInput> inputs = new ArrayList<>();
        for (ParticipantText item : texts) {
            if (item == null || item.member() == null) {
                throw new ValidationException("A participant entry must not be empty.");
            }
            BigDecimal value = item.text() == null || item.text().isBlank()
                    ? BigDecimal.ZERO
                    : parseDecimal(item.text());
            inputs.add(new ParticipantInput(item.member(), value));
        }
        return inputs;
    }
}
