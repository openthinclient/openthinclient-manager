package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.ui.ComponentContainer;
import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsListDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsListPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter.View;

public class PackageDetailsListView extends PackageDetailsListDesign implements PackageDetailsListPresenter.View {
 
  /** serialVersionUID  */
  private static final long serialVersionUID = -618490472517849307L;

  View view;
  PackageDetailsPresenter detailsPresenter;
  
  public PackageDetailsListView() {
      list.setPrimaryStyleName("otc-this-is-the-list-container");
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

  @Override
  public void setHeight(float height, Unit unit) {
    // list.setHeight(height, unit);
  }

}
