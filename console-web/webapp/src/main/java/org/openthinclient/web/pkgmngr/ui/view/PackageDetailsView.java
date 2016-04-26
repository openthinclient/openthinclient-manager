package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.ui.ComponentContainer;

import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

public class PackageDetailsView extends PackageDetailsDesign implements PackageDetailsPresenter.View {
  @Override
  public ComponentContainer getActionBar() {
    return actionBar;
  }

  @Override
  public void setName(String name) {
    this.name.setValue(name);
  }

  @Override
  public void setVersion(String version) {
    this.version.setValue(version);
  }

  @Override
  public void setDescription(String description) {
    this.description.setValue(description);
  }

  @Override
  public void hide() {
    setVisible(false);
  }

  @Override
  public void show() {
    setVisible(true);
  }
}
