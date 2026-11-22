package com.splitter;

import com.splitter.command.CommandHistory;
import com.splitter.gui.MainFrame;
import com.splitter.gui.MainPresenter;
import com.splitter.gui.MembersPanel;
import com.splitter.gui.MembersPresenter;
import com.splitter.meta.AIGenerated;
import com.splitter.service.ExpenseService;
import com.splitter.service.ExpenseServiceImpl;
import com.splitter.storage.FileRepository;
import javax.swing.SwingUtilities;

/** Application entry point: wires the service, the presenters and the views, then shows the window. */
@AIGenerated(tool = "Claude Code", prompt = "P01, P07")
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExpenseService service = new ExpenseServiceImpl(new FileRepository(), new CommandHistory());

            MembersPanel membersPanel = new MembersPanel();
            MainFrame frame = new MainFrame(membersPanel);

            MembersPresenter membersPresenter = new MembersPresenter(service, membersPanel);
            MainPresenter mainPresenter = new MainPresenter(service, frame);
            membersPanel.setPresenter(membersPresenter);
            frame.setPresenter(mainPresenter);

            membersPresenter.start();
            mainPresenter.start();
            frame.setVisible(true);
        });
    }
}
