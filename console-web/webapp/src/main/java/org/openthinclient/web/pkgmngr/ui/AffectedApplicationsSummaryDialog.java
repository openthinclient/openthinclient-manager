package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.server.Sizeable;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Profile;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AffectedApplicationsSummaryDialog extends AbstractSummaryDialog {

  private final BeanItemContainer<Application> container;
  private final List<Runnable> proceedCallbacks;

  public AffectedApplicationsSummaryDialog(Collection<Application> affectedApplications) {
    this.container = new BeanItemContainer<>(Application.class, affectedApplications);
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

    Table table = new Table();
    table.addStyleName(ValoTheme.TABLE_BORDERLESS);
//    table.addStyleName(ValoTheme.TABLE_NO_HEADER);
    table.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
    table.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
    table.setContainerDataSource(container);

    table.addGeneratedColumn("schemaName", new Table.ColumnGenerator() {
      @Override
      public Object generateCell(Table source, Object itemId, Object columnId) {

        Profile profile = (Profile) itemId;
        return profile.getSchema(profile.getRealm()).getName();

      }
    });

    table.setVisibleColumns("name", "schemaName");
    table.setRowHeaderMode(Table.RowHeaderMode.ICON_ONLY);
    table.setColumnExpandRatio("name", 1);

    table.setColumnHeader("name", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_TABLE_NAME));
    table.setColumnHeader("schemaName", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_TABLE_SCHEMANAME));

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
