package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Transfer;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The Settle Up tab: one line per suggested transfer. These are suggestions only; payments are not
 * recorded.
 */
@AIGenerated(tool = "Claude Code", prompt = "P09")
public class SettleUpPanel extends JPanel implements SettleUpView {

    private static final long serialVersionUID = 1L;

    private final MoneyFormatter formatter = new MoneyFormatter();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    public SettleUpPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(new JLabel("These are suggestions only. Payments are not recorded."), BorderLayout.NORTH);
        add(new JScrollPane(new JList<>(listModel)), BorderLayout.CENTER);
    }

    @Override
    public void showSettlements(List<Transfer> transfers) {
        listModel.clear();
        if (transfers.isEmpty()) {
            listModel.addElement("Everyone is settled up.");
            return;
        }
        for (Transfer transfer : transfers) {
            listModel.addElement(formatter.format(transfer));
        }
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
