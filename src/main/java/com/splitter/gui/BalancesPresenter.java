package com.splitter.gui;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.service.ExpenseService;
import com.splitter.service.ServiceListener;

/** Presenter of the Balances tab. Contains no Swing code. */
@AIGenerated(tool = "Claude Code", prompt = "P09")
public class BalancesPresenter implements ServiceListener {

    private final ExpenseService service;
    private final BalancesView view;

    public BalancesPresenter(ExpenseService service, BalancesView view) {
        this.service = service;
        this.view = view;
    }

    /** Starts listening to the service and shows the current balances. */
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
            view.showBalances(service.getBalances());
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }
}
