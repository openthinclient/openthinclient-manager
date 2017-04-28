package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

import com.vaadin.v7.data.Item;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.v7.ui.Table;
import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import java.util.List;

public class PackageDetailsView extends PackageDetailsDesign implements PackageDetailsPresenter.View {
 
  /** serialVersionUID  */
  private static final long serialVersionUID = -2726203031530856857L;
  
  private final PackageListContainer packageListContainer;
  private final PackageListContainer conflictsListContainer;
  private final PackageListContainer providesListContainer;

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

    // conflicts
    conflictsListContainer = new PackageListContainer();
    conflicts.setContainerDataSource(conflictsListContainer);
    conflicts.setVisibleColumns("name", "displayVersion");
    conflicts.setColumnHeader("name", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    conflicts.setColumnHeader("displayVersion", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    conflicts.setHeight("39px");

    // provides
    providesListContainer = new PackageListContainer();
    provides.setContainerDataSource(providesListContainer);
    provides.setVisibleColumns("name", "displayVersion");
    provides.setColumnHeader("name", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    provides.setColumnHeader("displayVersion", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    provides.setHeight("39px");


    this.changeLog.setContentMode(ContentMode.PREFORMATTED);
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
  public void hideConflictsTable() {
    conflictsLabel.setVisible(false);
    conflicts.setVisible(false);
  }

  @Override
  public void hideProvidesTable() {
    providesLabel.setVisible(false);
    provides.setVisible(false);
  }

  @Override
  public void addDependencies(List<AbstractPackageItem> depends) {
    if (depends != null) {
      for (AbstractPackageItem api : depends) {
        if (api instanceof MissingPackageItem) {
          Item item = packageListContainer.getItem(packageListContainer.addItem(api));
        } else {
          packageListContainer.addItem(api);
        }
      }
      dependencies.setHeight(39 + (packageListContainer.size() * 38) + "px");
    }
  }

  @Override
  public void addConflicts(List<AbstractPackageItem> conflicts) {
    if (conflicts != null) {
      for (AbstractPackageItem api : conflicts) {
        if (api instanceof MissingPackageItem) {
          Item item = conflictsListContainer.getItem(conflictsListContainer.addItem(api));
        } else {
          conflictsListContainer.addItem(api);
        }
      }
      this.conflicts.setHeight(39 + (conflictsListContainer.size() * 38) + "px");
    }
  }

  @Override
  public void addProvides(List<AbstractPackageItem> provides) {
    if (provides != null) {
      for (AbstractPackageItem api : provides) {
        if (api instanceof MissingPackageItem) {
          Item item = providesListContainer.getItem(providesListContainer.addItem(api));
        } else {
          providesListContainer.addItem(api);
        }
      }
      this.provides.setHeight(39 + (providesListContainer.size() * 38) + "px");
    }
  }

  @Override
  public void clearLists() {
    packageListContainer.removeAllItems();
    conflictsListContainer.removeAllItems();
    providesListContainer.removeAllItems();
  }
}
