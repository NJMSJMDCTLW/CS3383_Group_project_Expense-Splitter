package com.splitter.gui;

import com.splitter.exception.StorageException;
import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.service.ExpenseService;
import com.splitter.service.ServiceListener;
import java.nio.file.Path;
import java.util.Optional;

/** Presenter of the main window: New, Open, Save and Undo. Contains no Swing code. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public class MainPresenter implements ServiceListener {

    private final ExpenseService service;
    private final MainView view;

    public MainPresenter(ExpenseService service, MainView view) {
        this.service = service;
        this.view = view;
    }

    /** Starts listening to the service and shows the group name and the undo state. */
    public void start() {
        service.addListener(this);
        refresh();
    }

    public void stop() {
        service.removeListener(this);
    }

    public void onNew() {
        if (hasData() && !view.confirmDiscardChanges()) {
            return;
        }
        Optional<String> name = view.askGroupName();
        if (name.isEmpty()) {
            return;
        }
        try {
            service.newGroup(name.get());
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    public void onOpen() {
        if (hasData() && !view.confirmDiscardChanges()) {
            return;
        }
        Optional<Path> file = view.chooseFileToOpen();
        if (file.isEmpty()) {
            return;
        }
        try {
            service.open(file.get());
        } catch (StorageException | ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    public void onSave() {
        Optional<Path> file = view.chooseFileToSave();
        if (file.isEmpty()) {
            return;
        }
        try {
            service.save(file.get());
        } catch (StorageException | ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    public void onUndo() {
        try {
            service.undo();
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onDataChanged() {
        refresh();
    }

    private boolean hasData() {
        return !service.getMembers().isEmpty() || !service.getExpenses().isEmpty();
    }

    private void refresh() {
        view.showGroupName(service.getGroupName());
        view.setUndoEnabled(service.canUndo());
    }
}
