package com.splitter.gui;

import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Member;
import com.splitter.service.ExpenseService;
import com.splitter.service.ServiceListener;

/** Presenter of the Members tab. Contains no Swing code. */
@AIGenerated(tool = "Claude Code", prompt = "P07")
public class MembersPresenter implements ServiceListener {

    private final ExpenseService service;
    private final MembersView view;

    public MembersPresenter(ExpenseService service, MembersView view) {
        this.service = service;
        this.view = view;
    }

    /** Starts listening to the service and shows the current members. */
    public void start() {
        service.addListener(this);
        view.showMembers(service.getMembers());
    }

    public void stop() {
        service.removeListener(this);
    }

    public void onAddMember(String name) {
        try {
            service.addMember(name);
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    public void onRemoveMember(Member member) {
        try {
            service.removeMember(member);
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onDataChanged() {
        view.showMembers(service.getMembers());
    }
}
