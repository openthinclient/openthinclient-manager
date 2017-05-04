package org.openthinclient.web.pkgmngr.ui.view;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.UI;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

import java.util.Collections;
import java.util.List;

//import com.vaadin.v7.data.Item;
//import com.vaadin.v7.ui.Table;

public class PackageDetailsView extends PackageDetailsDesign implements PackageDetailsPresenter.View {
 
  /** serialVersionUID  */
  private static final long serialVersionUID = -2726203031530856857L;
  
//  private PackageListContainer packageListContainer;
//  private PackageListContainer conflictsListContainer;
//  private PackageListContainer providesListContainer;

  public PackageDetailsView() {
//
    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    dependencies.setDataProvider(DataProvider.ofCollection(Collections.EMPTY_LIST));
    dependencies.setSelectionMode(Grid.SelectionMode.NONE);
    dependencies.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    dependencies.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    dependencies.setHeight("39px");
    
    dependencies.setStyleGenerator(new StyleGenerator<AbstractPackageItem>() {
      @Override
      public String apply(AbstractPackageItem item) {
        if (item != null && item instanceof MissingPackageItem) {
          return "highlight-red";
        }
        return null;
      }
    });

    // conflicts
    conflicts.setDataProvider(DataProvider.ofCollection(Collections.EMPTY_LIST));
    conflicts.setSelectionMode(Grid.SelectionMode.NONE);
    conflicts.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    conflicts.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    conflicts.setHeight("39px");

    // provides
    provides.setDataProvider(DataProvider.ofCollection(Collections.EMPTY_LIST));
    provides.setSelectionMode(Grid.SelectionMode.NONE);
    provides.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    provides.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
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
//      for (AbstractPackageItem api : depends) {
//        if (api instanceof MissingPackageItem) {
//          Item item = packageListContainer.getItem(packageListContainer.addItem(api));
//        } else {
//          packageListContainer.addItem(api);
//        }
//      }
      dependencies.setDataProvider(DataProvider.ofCollection(depends));
      dependencies.setHeight(39 + (depends.size() * 38) + "px");
    }
  }

  @Override
  public void addConflicts(List<AbstractPackageItem> packageConflicts) {
    if (conflicts != null) {
//      for (AbstractPackageItem api : conflicts) {
//        if (api instanceof MissingPackageItem) {
//          Item item = conflictsListContainer.getItem(conflictsListContainer.addItem(api));
//        } else {
//          conflictsListContainer.addItem(api);
//        }
//      }
//      this.conflicts.setHeight(39 + (conflictsListContainer.size() * 38) + "px");
      conflicts.setDataProvider(DataProvider.ofCollection(packageConflicts));
      conflicts.setHeight(39 + (packageConflicts.size() * 38) + "px");
    }
  }

  @Override
  public void addProvides(List<AbstractPackageItem> packageProvides) {
    if (provides != null) {
//      for (AbstractPackageItem api : provides) {
//        if (api instanceof MissingPackageItem) {
//          Item item = providesListContainer.getItem(providesListContainer.addItem(api));
//        } else {
//          providesListContainer.addItem(api);
//        }
//      }
//      this.provides.setHeight(39 + (providesListContainer.size() * 38) + "px");
      provides.setDataProvider(DataProvider.ofCollection(packageProvides));
      provides.setHeight(39 + (packageProvides.size() * 38) + "px");
    }
  }

//  @Override
//  public void clearLists() {
////    packageListContainer.removeAllItems();
////    conflictsListContainer.removeAllItems();
////    providesListContainer.removeAllItems();
//  }
}
