package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Money;
import com.splitter.model.Transfer;

/** The only place that knows the currency symbol. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public class MoneyFormatter {

    private static final String SYMBOL = "$";

    /** For example "$12.34"; a negative amount is shown as "-$12.34". */
    public String format(Money money) {
        String plain = money.toString();
        if (plain.startsWith("-")) {
            return "-" + SYMBOL + plain.substring(1);
        }
        return SYMBOL + plain;
    }

    /** For example "A pays B $12.34". */
    public String format(Transfer transfer) {
        return transfer.getFrom().getName() + " pays " + transfer.getTo().getName() + " "
                + format(transfer.getAmount());
    }
}
