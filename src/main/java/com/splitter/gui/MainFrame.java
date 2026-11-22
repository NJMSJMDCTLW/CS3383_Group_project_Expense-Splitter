package com.splitter.gui;

import com.splitter.meta.AIGenerated;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The main window: four tabs and a menu with New, Open, Save and Undo. It only collects input and shows
 * dialogs; every decision is made by the {@link MainPresenter}.
 */
@AIGenerated(tool = "Claude Code", prompt = "P07, P08")
public class MainFrame extends JFrame implements MainView {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Expense Splitter";

    private final JMenuItem undoItem = new JMenuItem("Undo expense change");
    private MainPresenter presenter;

    public MainFrame(MembersPanel membersPanel, ExpensesPanel expensesPanel) {
        super(TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Members", membersPanel);
        tabs.addTab("Expenses", expensesPanel);
        tabs.addTab("Balances", placeholder("Balances: coming in the next step"));
        tabs.addTab("Settle Up", placeholder("Settle Up: coming in the next step"));
        add(tabs);

        setJMenuBar(createMenuBar());
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    /** Connects the presenter. It is a setter because the presenter needs this view in its constructor. */
    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
    }

    private static JPanel placeholder(String text) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.add(new JLabel(text, SwingConstants.CENTER), java.awt.BorderLayout.CENTER);
        return panel;
    }

    private JMenuBar createMenuBar() {
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuItem newItem = new JMenuItem("New...");
        JMenuItem openItem = new JMenuItem("Open...");
        JMenuItem saveItem = new JMenuItem("Save...");
        newItem.addActionListener(e -> {
            if (presenter != null) {
                presenter.onNew();
            }
        });
        openItem.addActionListener(e -> {
            if (presenter != null) {
                presenter.onOpen();
            }
        });
        saveItem.addActionListener(e -> {
            if (presenter != null) {
                presenter.onSave();
            }
        });
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut));
        undoItem.setEnabled(false);
        undoItem.addActionListener(e -> {
            if (presenter != null) {
                presenter.onUndo();
            }
        });

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(undoItem);

        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu);
        bar.add(editMenu);
        return bar;
    }

    @Override
    public void showGroupName(String name) {
        setTitle(TITLE + " - " + name);
    }

    @Override
    public void setUndoEnabled(boolean enabled) {
        undoItem.setEnabled(enabled);
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public Optional<String> askGroupName() {
        String name = JOptionPane.showInputDialog(this, "Name of the new group:", "New group",
                JOptionPane.PLAIN_MESSAGE);
        return Optional.ofNullable(name);
    }

    @Override
    public boolean confirmDiscardChanges() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Unsaved changes will be lost.\nDo you want to continue?", "Unsaved changes",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.OK_OPTION;
    }

    @Override
    public Optional<Path> chooseFileToOpen() {
        JFileChooser chooser = createChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        return Optional.of(chooser.getSelectedFile().toPath());
    }

    @Override
    public Optional<Path> chooseFileToSave() {
        JFileChooser chooser = createChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        Path path = chooser.getSelectedFile().toPath();
        String fileName = path.getFileName().toString();
        if (fileName.indexOf('.') < 0) {
            path = path.resolveSibling(fileName + ".csv");
        }
        if (Files.exists(path)) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "The file " + path.getFileName() + " already exists.\nDo you want to replace it?",
                    "Replace file", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return Optional.empty();
            }
        }
        return Optional.of(path);
    }

    private static JFileChooser createChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Expense Splitter files (*.csv)", "csv"));
        return chooser;
    }
}
