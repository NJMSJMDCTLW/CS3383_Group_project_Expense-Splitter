package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * The Expenses tab: a table of expenses and a form to add one. It only collects input and shows what the
 * presenter gives it; every decision is made by the {@link ExpensesPresenter}.
 */
@AIGenerated(tool = "Claude Code", prompt = "P08")
public class ExpensesPanel extends JPanel implements ExpensesView {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Description", "Payer", "Total", "Split", "Shares"};

    /** One member in the form: a checkbox (participant) and a text field for the member's value. */
    private static final class ParticipantRow {
        private final Member member;
        private final JCheckBox check;
        private final JTextField field = new JTextField(8);
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));

        private ParticipantRow(Member member) {
            this.member = member;
            this.check = new JCheckBox(member.getName());
            check.setPreferredSize(new Dimension(160, check.getPreferredSize().height));
            panel.add(check);
            panel.add(field);
        }
    }

    private final MoneyFormatter formatter = new MoneyFormatter();
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JTextField descriptionField = new JTextField(18);
    private final JComboBox<Member> payerBox = new JComboBox<>();
    private final JTextField amountField = new JTextField(8);
    private final JComboBox<SplitMethod> methodBox = new JComboBox<>(SplitMethod.values());
    private final JLabel hintLabel = new JLabel();
    private final JPanel participantsPanel = new JPanel();
    private final List<ParticipantRow> rows = new ArrayList<>();
    private ExpensesPresenter presenter;

    public ExpensesPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        JButton removeButton = new JButton("Remove selected");
        removeButton.addActionListener(e -> removeSelected());
        JPanel removePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        removePanel.add(removeButton);
        JPanel tablePanel = new JPanel(new BorderLayout(0, 4));
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tablePanel.add(removePanel, BorderLayout.SOUTH);
        add(tablePanel, BorderLayout.CENTER);

        methodBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SplitMethod) {
                    setText(((SplitMethod) value).getDisplayName());
                }
                return this;
            }
        });
        methodBox.addActionListener(e -> applyMethod());

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topRow.add(new JLabel("Description:"));
        topRow.add(descriptionField);
        topRow.add(new JLabel("Paid by:"));
        topRow.add(payerBox);
        topRow.add(new JLabel("Amount:"));
        topRow.add(amountField);
        topRow.add(new JLabel("Split:"));
        topRow.add(methodBox);

        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        hintRow.add(new JLabel("Participants:"));
        hintRow.add(hintLabel);

        participantsPanel.setLayout(new BoxLayout(participantsPanel, BoxLayout.Y_AXIS));
        JScrollPane participantsScroll = new JScrollPane(participantsPanel);
        participantsScroll.setPreferredSize(new Dimension(100, 130));

        JButton addButton = new JButton("Add expense");
        addButton.addActionListener(e -> addExpense());
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        addRow.add(addButton);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createTitledBorder("New expense"));
        form.add(topRow);
        form.add(hintRow);
        form.add(participantsScroll);
        form.add(addRow);
        add(form, BorderLayout.SOUTH);

        applyMethod();
    }

    /** Connects the presenter. It is a setter because the presenter needs this view in its constructor. */
    public void setPresenter(ExpensesPresenter presenter) {
        this.presenter = presenter;
    }

    private void addExpense() {
        if (presenter == null) {
            return;
        }
        SplitMethod method = (SplitMethod) methodBox.getSelectedItem();
        List<ParticipantText> texts = new ArrayList<>();
        for (ParticipantRow row : rows) {
            if (row.check.isSelected()) {
                String text = method == SplitMethod.EQUAL ? "" : row.field.getText();
                texts.add(new ParticipantText(row.member, text));
            }
        }
        presenter.onAddExpense(descriptionField.getText(), (Member) payerBox.getSelectedItem(),
                amountField.getText(), method, texts);
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Please select an expense to remove.");
            return;
        }
        if (presenter != null) {
            int id = (Integer) tableModel.getValueAt(row, 0);
            presenter.onRemoveSelected(id);
        }
    }

    /** Shows or hides the value fields and the hint text to match the chosen split method. */
    private void applyMethod() {
        SplitMethod method = (SplitMethod) methodBox.getSelectedItem();
        String hint;
        String tip;
        if (method == SplitMethod.EXACT) {
            hint = "tick who takes part and type the amount each person pays (in currency units)";
            tip = "Amount in currency units, e.g. 12.50";
        } else if (method == SplitMethod.PERCENTAGE) {
            hint = "tick who takes part and type the percent each person pays";
            tip = "Percent, e.g. 25";
        } else if (method == SplitMethod.SHARES) {
            hint = "tick who takes part and type the number of shares of each person";
            tip = "Number of shares, e.g. 2";
        } else {
            hint = "tick who takes part; the amount is split equally";
            tip = null;
        }
        hintLabel.setText(hint);
        for (ParticipantRow row : rows) {
            row.field.setVisible(method != SplitMethod.EQUAL);
            row.field.setToolTipText(tip);
        }
        participantsPanel.revalidate();
        participantsPanel.repaint();
    }

    @Override
    public void showMembers(List<Member> members) {
        // Keep what the user already typed for members that are still in the group.
        Map<Member, Boolean> checked = new HashMap<>();
        Map<Member, String> typed = new HashMap<>();
        for (ParticipantRow row : rows) {
            checked.put(row.member, row.check.isSelected());
            typed.put(row.member, row.field.getText());
        }
        Member selectedPayer = (Member) payerBox.getSelectedItem();

        payerBox.removeAllItems();
        for (Member member : members) {
            payerBox.addItem(member);
        }
        if (selectedPayer != null && members.contains(selectedPayer)) {
            payerBox.setSelectedItem(selectedPayer);
        }

        participantsPanel.removeAll();
        rows.clear();
        for (Member member : members) {
            ParticipantRow row = new ParticipantRow(member);
            row.check.setSelected(Boolean.TRUE.equals(checked.get(member)));
            row.field.setText(typed.getOrDefault(member, ""));
            rows.add(row);
            participantsPanel.add(row.panel);
        }
        applyMethod();
    }

    @Override
    public void showExpenses(List<Expense> expenses) {
        tableModel.setRowCount(0);
        for (Expense expense : expenses) {
            tableModel.addRow(new Object[] {
                expense.getId(),
                expense.getDescription(),
                expense.getPayer().getName(),
                formatter.format(expense.getTotal()),
                expense.getMethod().getDisplayName(),
                formatShares(expense)
            });
        }
    }

    private String formatShares(Expense expense) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Member, Money> share : expense.getShares().entrySet()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(share.getKey().getName()).append(' ').append(formatter.format(share.getValue()));
        }
        return text.toString();
    }

    /** Clears the description, the amount, the checkboxes and the value fields; payer and method stay. */
    @Override
    public void clearForm() {
        descriptionField.setText("");
        amountField.setText("");
        for (ParticipantRow row : rows) {
            row.check.setSelected(false);
            row.field.setText("");
        }
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
