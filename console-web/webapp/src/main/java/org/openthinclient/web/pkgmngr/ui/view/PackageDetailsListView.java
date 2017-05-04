package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsListDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsListPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter.View;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.v7.ui.VerticalLayout;

public class PackageDetailsListView extends PackageDetailsListDesign implements PackageDetailsListPresenter.View {
 
  /** serialVersionUID  */
  private static final long serialVersionUID = -618490472517849307L;

  View view;
  PackageDetailsPresenter detailsPresenter;
  
  public PackageDetailsListView() {
      list = new VerticalLayout();
      list.setSpacing(true);
      this.addComponent(list);
  }

  @Override
  public void addPackageDetails(PackageDetailsView packageDetailsView) {
    list.addComponent(packageDetailsView);
  }

  @Override
  public void clearPackageList() {
    list.removeAllComponents();
  }
  
  @Override
  public void hide() {
    setVisible(false);
  }

  @Override
  public void show() {
    setVisible(true);
  }  
  
  @Override
  public ComponentContainer getActionBar() {
    return actionBar;
  }  

}
