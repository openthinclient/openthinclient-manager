package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.Application;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AffectedApplicationsSummaryDialog extends AbstractSummaryDialog {

  private final Collection<Application> affectedApplications;
  private final List<Runnable> proceedCallbacks;

  public AffectedApplicationsSummaryDialog(Collection<Application> affectedApplications) {
    this.affectedApplications = affectedApplications;
    proceedCallbacks = new ArrayList<>();
  }

  @Override
  protected void onCancel() {
    close();
  }

  @Override
  protected void createContent(MVerticalLayout content) {

    final Label l = new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_HEADLINE));
    l.addStyleName(ValoTheme.LABEL_HUGE);
    l.addStyleName(ValoTheme.LABEL_COLORED);
    content.addComponent(l);


    final Label label = new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_MESSAGE));
    label.addStyleName(ValoTheme.LABEL_LARGE);
    content.add(label);

    Grid<Application> table = new Grid<>();
    table.setDataProvider(DataProvider.ofCollection(affectedApplications));
    table.setSelectionMode(Grid.SelectionMode.NONE);
    table.addColumn(Application::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_TABLE_NAME));
    table.addColumn(application -> application.getSchema(application.getRealm()).getName()).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_TABLE_SCHEMANAME));

    table.setWidth(100, Sizeable.Unit.PERCENTAGE);
    table.setHeight(100, Sizeable.Unit.PIXELS);

    content.add(table);
  }

  @Override
  public void update() {

  }

  @Override
  protected void onProceed() {
    proceedCallbacks.forEach(Runnable::run);
  }

  public void onProceed(Runnable callback) {
    proceedCallbacks.add(callback);
  }
}
