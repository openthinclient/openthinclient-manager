package org.openthinclient.web.pkgmngr.ui.presenter;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;

import org.openthinclient.pkgmgr.db.Package;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;

public class PackageListMasterDetailsPresenter {

  private final View view;
  private final PackageDetailsListPresenter detailsPresenter;

  private MyCustomFilter myCustomFilter;
  
  public PackageListMasterDetailsPresenter(View view, PackageDetailsListPresenter detailsPresenter) {
    this.view = view;
    this.detailsPresenter = detailsPresenter;

    // basic wiring.
    view.onPackageSelected(detailsPresenter::setPackages);
    
    // search
    this.view.getSearchButton().addClickListener(e -> {
      handleSearchInput(view);
    });
    
    this.view.getSearchField().addValueChangeListener(e -> {
      handleSearchInput(view);
    });
    
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
      view.removeAllContainerFilters();
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
  }

  public interface View {

    void clearPackageList();

    void removeContainerFilter(Filter filter);

    Button getSearchButton();

    TextField getSearchField();

    void addPackage(Package otcPackage);

    void onPackageSelected(Consumer<Collection<Package>> consumer);
//    void onPackageSelected(Consumer<Package> consumer);

    void addContainerFilter(Filter filter);
    
    void removeAllContainerFilters();
  }

  class MyCustomFilter implements Container.Filter {
    /** serialVersionUID  */
    private static final long serialVersionUID = 2238041700478666015L;
    protected String propertyId;
    protected String regex;
    
    public MyCustomFilter(String propertyId, String regex) {
        this.propertyId = propertyId;
        this.regex      = regex;
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
        
        // Pass all if regex not given
        if (regex.isEmpty()) {
            return true;
        }
        
        // The actual filter logic + error handling
        try {
            boolean result = value.matches(regex);
            return result;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /** Tells if this filter works on the given property. */
    @Override
    public boolean appliesToProperty(Object propertyId) {
        return propertyId != null &&
               propertyId.equals(this.propertyId);
    }
}  
  
}
