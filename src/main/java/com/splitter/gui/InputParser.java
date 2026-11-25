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

/**
 * Turns the text typed in the GUI into typed values.
 * Every failure is reported using a ValidationException.
 */
@AIGenerated(tool = "Claude Code", prompt = "P07", refinedBy = "Dora", changes = "Fix #6: reject blank participant values for non-EQUAL splits.")
public class InputParser {

    private static final Pattern DECIMAL = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

    /**
     * Parses an amount such as "12.50".
     * Surrounding spaces are ignored.
     *
     * @param text the amount entered by the user
     * @return the parsed Money value
     */
    public Money parseMoney(String text) {
        return Money.parse(text == null ? null : text.trim());
    }

    /**
     * Parses a plain decimal number such as "30", "33.33" or "-1".
     * Surrounding spaces are ignored.
     *
     * @param text the decimal number entered by the user
     * @return the parsed BigDecimal value
     * @throws ValidationException if the text is blank or invalid
     */
    public BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Please enter a number.");
        }

        String trimmed = text.trim();

        if (!DECIMAL.matcher(trimmed).matches()) {
            throw new ValidationException(
                    "\"" + trimmed + "\" is not a valid number.");
        }

        return new BigDecimal(trimmed);
    }

    /**
     * Finds the split method whose display name matches the given text.
     *
     * @param displayName the split method name selected by the user
     * @return the matching SplitMethod
     * @throws ValidationException if no valid method is selected
     */
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
     * Converts the typed participant texts into participant inputs,
     * keeping the original order.
     *
     * For EQUAL, participant text values are ignored and represented
     * as zero because the amounts are calculated automatically.
     *
     * For the other split methods (EXACT, PERCENTAGE and SHARES),
     * each participant must enter a valid number. Blank values are
     * rejected.
     *
     * @param texts  the participant names and entered values
     * @param method the selected split method
     * @return the parsed participant inputs
     * @throws ValidationException if the participant list, method,
     *                             participant or required value is invalid
     */
    public List<ParticipantInput> parseInputs(
            List<ParticipantText> texts, SplitMethod method) {

        if (texts == null) {
            throw new ValidationException(
                    "At least one participant is required.");
        }

        List<ParticipantInput> inputs = new ArrayList<>();

        for (ParticipantText item : texts) {
            if (item == null || item.member() == null) {
                throw new ValidationException(
                        "A participant entry must not be empty.");
            }

            BigDecimal value;

            if (method == SplitMethod.EQUAL) {
                value = BigDecimal.ZERO;
            } else {
                if (item.text() == null || item.text().isBlank()) {
                    throw new ValidationException(
                            "Please enter a value for \""
                                    + item.member().getName() + "\".");
                }

                value = parseDecimal(item.text());
            }

            inputs.add(new ParticipantInput(item.member(), value));
        }

        return inputs;
    }
}
