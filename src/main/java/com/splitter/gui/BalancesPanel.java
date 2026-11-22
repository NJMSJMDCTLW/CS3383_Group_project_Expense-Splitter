package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.model.Money;
import java.awt.BorderLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * The Balances tab: one row per member with the balance and a status text. It only shows what the
 * presenter gives it, in the order it is given.
 */
@AIGenerated(tool = "Claude Code", prompt = "P09")
public class BalancesPanel extends JPanel implements BalancesView {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Member", "Balance", "Status"};

    private final MoneyFormatter formatter = new MoneyFormatter();
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public BalancesPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTable table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(right);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void showBalances(Map<Member, Money> balances) {
        tableModel.setRowCount(0);
        for (Map.Entry<Member, Money> entry : balances.entrySet()) {
            Money balance = entry.getValue();
            String status;
            if (balance.signum() > 0) {
                status = "is owed";
            } else if (balance.signum() < 0) {
                status = "owes";
            } else {
                status = "settled";
            }
            tableModel.addRow(new Object[] {entry.getKey().getName(), formatter.format(balance), status});
        }
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
