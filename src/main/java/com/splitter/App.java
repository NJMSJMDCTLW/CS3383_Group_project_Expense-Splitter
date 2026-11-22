package com.splitter;

import com.splitter.command.CommandHistory;
import com.splitter.gui.BalancesPanel;
import com.splitter.gui.BalancesPresenter;
import com.splitter.gui.ExpensesPanel;
import com.splitter.gui.ExpensesPresenter;
import com.splitter.gui.MainFrame;
import com.splitter.gui.MainPresenter;
import com.splitter.gui.MembersPanel;
import com.splitter.gui.MembersPresenter;
import com.splitter.gui.SettleUpPanel;
import com.splitter.gui.SettleUpPresenter;
import com.splitter.meta.AIGenerated;
import com.splitter.service.ExpenseService;
import com.splitter.service.ExpenseServiceImpl;
import com.splitter.storage.FileRepository;
import javax.swing.SwingUtilities;

/** Application entry point: wires the service, the presenters and the views, then shows the window. */
@AIGenerated(tool = "Claude Code", prompt = "P01, P07, P08, P09")
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExpenseService service = new ExpenseServiceImpl(new FileRepository(), new CommandHistory());

            MembersPanel membersPanel = new MembersPanel();
            ExpensesPanel expensesPanel = new ExpensesPanel();
            BalancesPanel balancesPanel = new BalancesPanel();
            SettleUpPanel settleUpPanel = new SettleUpPanel();
            MainFrame frame = new MainFrame(membersPanel, expensesPanel, balancesPanel, settleUpPanel);

            MembersPresenter membersPresenter = new MembersPresenter(service, membersPanel);
            ExpensesPresenter expensesPresenter = new ExpensesPresenter(service, expensesPanel);
            BalancesPresenter balancesPresenter = new BalancesPresenter(service, balancesPanel);
            SettleUpPresenter settleUpPresenter = new SettleUpPresenter(service, settleUpPanel);
            MainPresenter mainPresenter = new MainPresenter(service, frame);
            membersPanel.setPresenter(membersPresenter);
            expensesPanel.setPresenter(expensesPresenter);
            frame.setPresenter(mainPresenter);

            membersPresenter.start();
            expensesPresenter.start();
            balancesPresenter.start();
            settleUpPresenter.start();
            mainPresenter.start();
            frame.setVisible(true);
        });
    }
}
