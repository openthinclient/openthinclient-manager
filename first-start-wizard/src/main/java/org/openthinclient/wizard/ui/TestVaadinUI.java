package org.openthinclient.wizard.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.spring.annotation.VaadinUI;
import org.vaadin.spring.annotation.VaadinUIScope;

@VaadinUI(path = "/test")
@VaadinUIScope
@Theme("otc-wizard")
public class TestVaadinUI extends UI {
  @Override
  protected void init(VaadinRequest request) {

    final VerticalLayout vl = new VerticalLayout();
    vl.setSizeFull();

    vl.addComponent(new Label("Testpage"));

    final SystemInstallProgressView view = new SystemInstallProgressView();
    vl.addComponent(view);
    vl.setExpandRatio(view, 1f);
    vl.setMargin(true);
    vl.setSpacing(true);


    view.setTitle("System Installation...");
    view.setDescription("This is the description with a lot of text");

    final SystemInstallProgressPresenter.InstallItemView finishedItem = view.addItemView();
    finishedItem.setTitle("This item is finished");
    finishedItem.setFinished();

    final SystemInstallProgressPresenter.InstallItemView runningItem = view.addItemView();
    runningItem.setTitle("This item is still running");
    runningItem.setRunning();

    final SystemInstallProgressPresenter.InstallItemView failedItem = view.addItemView();
    failedItem.setTitle("This item failed for some reason");
    failedItem.setFailed();

    final SystemInstallProgressPresenter.InstallItemView pendingItem = view.addItemView();
    pendingItem.setTitle("This item is currently waiting to be installed");
    pendingItem.setPending();

    setContent(vl);

  }
}
