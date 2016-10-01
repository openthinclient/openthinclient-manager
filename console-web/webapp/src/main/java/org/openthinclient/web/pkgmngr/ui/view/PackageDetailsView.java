package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.util.dpkg.PackageReference.SingleReference;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageDetailsView extends PackageDetailsDesign implements PackageDetailsPresenter.View {
 
  /** serialVersionUID  */
  private static final long serialVersionUID = -2726203031530856857L;
  
  private final PackageListContainer packageListContainer;

  public PackageDetailsView() {
    packageListContainer = new PackageListContainer();
    dependencies.setContainerDataSource(packageListContainer);
    dependencies.setVisibleColumns("name", "displayVersion");

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    dependencies.setColumnHeader("name", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    dependencies.setColumnHeader("displayVersion", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    
    dependencies.setHeight("39px");
  }
  
  @Override
  public ComponentContainer getActionBar() {
    return null;
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

  @Override
  public void setShortDescription(String shortDescription) {
   this.shortDescription.setValue(shortDescription);
  }

  @Override
  public void addDependency(Package otcPackage) {
    packageListContainer.addItem(otcPackage);
    // TODO: magic numbers 
    dependencies.setHeight(39 + (packageListContainer.size() * 38) + "px");
  }

  @Override
  public void clearPackageList() {
    packageListContainer.removeAllItems();
  }

  @Override
  public void addMissingPackage(PackageReference packageReference) {
    Item item = packageListContainer.getItem(packageListContainer.addItem());
    if (packageReference instanceof SingleReference) {
      SingleReference sr = (SingleReference) packageReference;
      Property<String> itemName = item.getItemProperty("name");
      itemName.setValue(sr.getName() + " (Missing)");
      Property<String> itemVersion = item.getItemProperty("version");
      itemVersion.setValue(sr.getVersion().toString());      
      // TODO: line-color of missing package
    }
    
  }
}
