package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

import java.util.List;

public class PackageDetailsWindow extends Window implements PackageDetailsPresenter.View {

  private final PackageDetailsPresenter.View target;
  private final ComponentContainer actionBar;


  public PackageDetailsWindow(PackageDetailsPresenter.View target, Component viewComponent) {
    this.target = target;

    final VerticalLayout contents = new VerticalLayout();
    contents.setMargin(true);
    contents.addComponent(viewComponent);
    contents.setExpandRatio(viewComponent, 1);
    contents.setSizeFull();

    final HorizontalLayout footer = new HorizontalLayout();
    footer.addStyleNames(ValoTheme.WINDOW_BOTTOM_TOOLBAR);

    actionBar = new CssLayout();
    footer.addComponent(actionBar);
    footer.setWidth("100%");
    footer.setExpandRatio(actionBar, 1);
    footer.setComponentAlignment(actionBar, Alignment.TOP_RIGHT);

    final Button closeButton = new Button();
    closeButton.addStyleNames(ValoTheme.BUTTON_QUIET);
    closeButton.setIcon(VaadinIcons.CLOSE);
    closeButton.setCaption("Close");
    closeButton.addClickListener(e -> hide());
    footer.addComponent(closeButton);

    contents.addComponent(footer);

    setContent(contents);
  }

  @Override
  public ComponentContainer getActionBar() {
    return actionBar;
  }

  @Override
  public void setName(String name) {
    setCaption(name);
    target.setName(name);
  }

  @Override
  public void setVersion(String version) {
    target.setVersion(version);
  }

  @Override
  public void setDescription(String description) {
    target.setDescription(description);
  }

  @Override
  public void hide() {

    if (UI.getCurrent().getWindows().contains(this)) {
      UI.getCurrent().removeWindow(this);
    }
  }

  @Override
  public void show() {
    setModal(true);
    setClosable(true);
    setWindowMode(WindowMode.NORMAL);
    setWidth("90%");
    setHeight("90%");
    UI.getCurrent().addWindow(this);
    super.setVisible(true);
  }

  @Override
  public void setShortDescription(String shortDescription) {
    target.setShortDescription(shortDescription);
  }

  @Override
  public void addDependencies(List<AbstractPackageItem> apis) {
    target.addDependencies(apis);
  }

  @Override
  public void addConflicts(List<AbstractPackageItem> apis) {
    target.addConflicts(apis);
  }

  @Override
  public void addProvides(List<AbstractPackageItem> apis) {
    target.addProvides(apis);
  }

  @Override
  public void setSourceUrl(String url) {
    target.setSourceUrl(url);
  }

  @Override
  public void setChangeLog(String changeLog) {
    target.setChangeLog(changeLog);
  }

  @Override
  public void hideConflictsTable() {
    target.hideConflictsTable();
  }

  @Override
  public void hideProvidesTable() {
    target.hideProvidesTable();
  }
}
