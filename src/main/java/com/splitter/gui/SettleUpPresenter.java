package com.splitter.gui;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.service.ExpenseService;
import com.splitter.service.ServiceListener;

/** Presenter of the Settle Up tab. Contains no Swing code. */
@AIGenerated(tool = "Claude Code", prompt = "P09")
public class SettleUpPresenter implements ServiceListener {

    private final ExpenseService service;
    private final SettleUpView view;

    public SettleUpPresenter(ExpenseService service, SettleUpView view) {
        this.service = service;
        this.view = view;
    }

    /** Starts listening to the service and shows the current suggested transfers. */
    public void start() {
        service.addListener(this);
        refresh();
    }

    public void stop() {
        service.removeListener(this);
    }

    @Override
    public void onDataChanged() {
        refresh();
    }

    private void refresh() {
        try {
            view.showSettlements(service.getSettlements());
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }
}
