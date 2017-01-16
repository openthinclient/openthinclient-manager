package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

import com.vaadin.data.Item;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Table;
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
    
    Table.CellStyleGenerator cellStyleGenerator = new Table.CellStyleGenerator() {
      @Override
      public String getStyle(Table source, Object itemId, Object propertyId) {
         if (itemId != null && itemId instanceof MissingPackageItem) {
            return "highlight-red";
         }
         return null;
      }
   }; 
   dependencies.setCellStyleGenerator(cellStyleGenerator);
    
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
  public void setSourceUrl(String url) {
     this.sourceUrl.setValue(url);
  }

  @Override
  public void setChangeLog(String changeLog) {
     this.changeLog.setValue(changeLog);
  }

  @Override
  public void addDependency(ResolvedPackageItem rpi) {
    packageListContainer.addItem(rpi);
    setPackageContainerHeight();
  }

  @Override
  public void clearPackageList() {
    packageListContainer.removeAllItems();
  }

  @Override
  public void addMissingPackage(MissingPackageItem mpi) {
    Item item = packageListContainer.getItem(packageListContainer.addItem(mpi));
    setPackageContainerHeight();
  }

   private void setPackageContainerHeight() {
      dependencies.setHeight(39 + (packageListContainer.size() * 38) + "px");
   }

}
