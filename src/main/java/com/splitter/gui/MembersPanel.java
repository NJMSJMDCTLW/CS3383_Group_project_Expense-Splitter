
package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * The Members tab: a list of members, a text field with an Add button,
 * and a Remove selected button.
 *
 * It only collects input and shows what the presenter gives it.
 */
@AIGenerated(
        tool = "Claude Code",
        prompt = "P07",
        refinedBy = "Dora",
        changes = "Fix #13: clear the member name field whenever a group is replaced."
)
public class MembersPanel extends JPanel implements MembersView {

    private static final long serialVersionUID = 1L;

    private final DefaultListModel<Member> listModel =
            new DefaultListModel<>();

    private final JList<Member> memberList =
            new JList<>(listModel);

    private final JTextField nameField = new JTextField(20);

    private MembersPresenter presenter;

    public MembersPanel() {
        super(new BorderLayout(8, 8));

        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        memberList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        add(new JScrollPane(memberList), BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove selected");

        JPanel controls =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        controls.add(new JLabel("Name:"));
        controls.add(nameField);
        controls.add(addButton);
        controls.add(removeButton);

        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addMember());
        nameField.addActionListener(e -> addMember());
        removeButton.addActionListener(e -> removeSelected());
    }

    /**
     * Connects the presenter.
     */
    public void setPresenter(MembersPresenter presenter) {
        this.presenter = presenter;
    }

    private void addMember() {
        if (presenter != null) {
            presenter.onAddMember(nameField.getText());
            nameField.requestFocusInWindow();
        }
    }

    private void removeSelected() {
        Member selected = memberList.getSelectedValue();

        if (selected == null) {
            showError("Please select a member to remove.");
            return;
        }

        if (presenter != null) {
            presenter.onRemoveMember(selected);
        }
    }

    @Override
    public void showMembers(List<Member> members) {

        // Keep the existing behavior: clear the field after a successful add.
        boolean grew = members.size() > listModel.size();

        listModel.clear();

        for (Member member : members) {
            listModel.addElement(member);
        }

        if (grew) {
            clearNameField();
        }
    }

    /**
     * Clears the member name input field.
     */
    @Override
    public void clearNameField() {
        nameField.setText("");
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
