package org.openthinclient.web.pkgmngr.ui.presenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openthinclient.pkgmgr.PackageManagerUtils;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageListMasterDetailsPresenter {

  private final View view;
  private final PackageDetailsListPresenter detailsPresenter;
  
  private MyCustomFilter myCustomFilter;
  private PackageVersionFilter packageVersionFilter;
  
  public PackageListMasterDetailsPresenter(View view, PackageDetailsListPresenter detailsPresenter) {
    this.view = view;
    this.detailsPresenter = detailsPresenter;

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    
    // basic wiring.
    view.onPackageSelected(detailsPresenter::setPackages);
    
    // filter checkBox
    this.view.getPackageFilerCheckbox().setCaption("Show all versions");
    this.view.getPackageFilerCheckbox().setValue(true);
    this.view.getPackageFilerCheckbox().addValueChangeListener(e -> {
       handlePackageFilter();
    });
    
    // search
    this.view.getSearchButton().addClickListener(e -> {
      handleSearchInput(view);
    });
    
    this.view.getSearchField().setInputPrompt(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMT));
    this.view.getSearchField().addValueChangeListener(e -> {
      handleSearchInput(view);
    });
    
  }

  /**
   * Process package filtering (all version or latest version)
   * To initialize the Filter, get the 'unfiltered' items from table-item-container
   */
  private void handlePackageFilter() {
    if (!this.view.getPackageFilerCheckbox().getValue()) {
      packageVersionFilter = new PackageVersionFilter(new ArrayList<>(this.view.getItems()));
      view.addContainerFilter(packageVersionFilter);    
    } else {
      view.removeContainerFilter(packageVersionFilter);
    }
  }

  /**
   * Process the search-input
   * @param view View
   */
  private void handleSearchInput(View view) {
    String value = view.getSearchField().getValue();

    // Set new filter for the "Name" column with RegEx
    if (value.trim().length() > 0) {
      if (myCustomFilter != null) {
        view.removeContainerFilter(myCustomFilter);
      }
      myCustomFilter = new MyCustomFilter("name", value);
      view.addContainerFilter(myCustomFilter);    
    } else {
      if (myCustomFilter != null) {
        view.removeContainerFilter(myCustomFilter);
      }
    }
    
    this.detailsPresenter.setPackages(null);
  }

  public void showPackageListLoadingError(Exception e) {
    // FIXME implement me!
  }

  public void setPackages(Collection<Package> packages) {

    view.clearPackageList();

    packages.forEach(view::addPackage);
    detailsPresenter.setPackages(null);
    
    view.adjustHeight();
  }

  public interface View {

    void clearPackageList();

    void removeContainerFilter(Filter filter);

    Button getSearchButton();

    TextField getSearchField();

    void addPackage(Package otcPackage);

    void onPackageSelected(Consumer<Collection<Package>> consumer);

    void addContainerFilter(Filter filter);
    
    void removeAllContainerFilters();
    
    CheckBox getPackageFilerCheckbox();

    void adjustHeight();

    Collection<Package> getItems();

  }

  class MyCustomFilter implements Container.Filter {
    /** serialVersionUID  */
    private static final long serialVersionUID = 2238041700478666015L;
    protected String propertyId;
    protected String searchStr;
    
    public MyCustomFilter(String propertyId, String searchStr) {
        this.propertyId = propertyId;
        this.searchStr      = searchStr;
    }

    /** Apply the filter on an item to check if it passes. */
    @Override
    public boolean passesFilter(Object itemId, Item item)  throws UnsupportedOperationException {
        // Acquire the relevant property from the item object
        Property<?> p = item.getItemProperty(propertyId);
        
        // Should always check validity
        if (p == null || !p.getType().equals(String.class))
            return false;
        String value = (String) p.getValue();
        
        if (searchStr.isEmpty()) {
            return true;
        }
        
        return value.toLowerCase().startsWith(searchStr.toLowerCase());
    }

    /** Tells if this filter works on the given property. */
    @Override
    public boolean appliesToProperty(Object propertyId) {
        return propertyId != null &&
               propertyId.equals(this.propertyId);
    }
    
    @Override
   public String toString() {
      return "MyCustomFilter: propertyId=" + propertyId + ", searchStr=" + searchStr;
   }
    
  }  
  
  class PackageVersionFilter implements Container.Filter {
    
    private static final long serialVersionUID = -3709444918449733118L;
    private final List<Package> packages;

    public PackageVersionFilter(Collection<Package> givenPackages) {
      PackageManagerUtils pmu = new PackageManagerUtils();
      packages = pmu.reduceToLatestVersion(givenPackages.stream().collect(Collectors.toList()));
    }

    @Override
    public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
      return packages.stream().filter(p -> p.compareTo((Package) itemId) == 0).findAny().isPresent();
    }

    @Override
    public boolean appliesToProperty(Object propertyId) {
      return true;
    } 
    
    
  }
  
}
