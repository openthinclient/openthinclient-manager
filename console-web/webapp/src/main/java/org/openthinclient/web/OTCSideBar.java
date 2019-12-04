package org.openthinclient.web;

import com.vaadin.data.HasValue;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.sidebar.OTCSideBarUtils;
import org.openthinclient.web.thinclient.AbstractThinclientView;
import org.openthinclient.web.thinclient.ApplicationGroupView;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.openthinclient.web.thinclient.UserGroupView;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.sidebar.SideBarItemDescriptor;
import org.vaadin.spring.sidebar.SideBarSectionDescriptor;
import org.vaadin.spring.sidebar.components.ValoSideBar;

import java.util.*;

public class OTCSideBar extends ValoSideBar {

  private static final Logger LOGGER = LoggerFactory.getLogger(OTCSideBar.class);

  public static final String SIDE_BAR_STYLE = "sideBar";
  public static final String SIDE_BAR_SECTION_ITEM_STYLE = "sideBarSectionItem";
  public static final String SIDE_BAR_SECTION_STYLE = "sideBarSection";
  public static final String SELECTED_STYLE = "selected";

  private OTCSideBarUtils sideBarUtils;
  private Map<SideBarItemDescriptor, FilterGrid> itemsMap = new HashMap<SideBarItemDescriptor, FilterGrid>();
  private Map<String, FilterGrid> filterGridMap = new HashMap<>();
  private Map<String, SideBarItemDescriptor> descriptorMap = new HashMap<>();
  private final String sectionId;

  /**
   * You should not need to create instances of this component directly. Instead, just inject the side bar into
   * your UI.
   *
   * @param sectionId
   * @param sideBarUtils
   */
  public OTCSideBar(String sectionId, OTCSideBarUtils sideBarUtils) {
    super(sideBarUtils);
    this.sideBarUtils = sideBarUtils;
    this.sectionId = sectionId;
  }

  @Override
  protected SectionComponentFactory<CssLayout> createDefaultSectionComponentFactory() {
    return new DefaultSectionComponentFactory();
  }

  @Override
  protected ItemComponentFactory createDefaultItemComponentFactory() {
    return new DefaultItemComponentFactory();
  }

  public void updateFilterGrid(View view, String directoryObjectName) {
    if (!(view instanceof AbstractThinclientView)) {
      return;
    }
    String viewName = ((AbstractThinclientView) view).getParentViewName();
    itemsMap.values().forEach(gridComponent -> gridComponent.setVisible(false));
    FilterGrid filterGrid = filterGridMap.get(viewName);
    if(filterGrid != null) {
      if (filterGrid.getSize() == 0) {
        filterGrid.setItems(getAllItems(descriptorMap.get(viewName)));
      }

      filterGrid.markSelectedItem(directoryObjectName);

      filterGrid.setVisible(true);
    }
  }

  public void selectItem(String viewName, DirectoryObject directoryObject, Set<DirectoryObject> directoryObjectSet) {
    FilterGrid filterGrid = filterGridMap.get(viewName);

    if (filterGrid != null) {
      filterGrid.setItems(directoryObjectSet);
      filterGrid.setVisible(true);

      if(directoryObject == null) {
        filterGrid.markSelectedItem("");
      } else {
        filterGrid.markSelectedItem(directoryObject.getName());
      }
    }
  }

  /**
   * Default implementation of {@link ValoSideBar.SectionComponentFactory} that adds the section header
   * and items directly to the composition root.
   */
  public class DefaultSectionComponentFactory implements SectionComponentFactory<CssLayout> {

    private ItemComponentFactory itemComponentFactory;

    @Override
    public void setItemComponentFactory(ItemComponentFactory itemComponentFactory) {
      this.itemComponentFactory = itemComponentFactory;
    }

    @Override
    public void createSection(CssLayout compositionRoot, SideBarSectionDescriptor descriptor, Collection<SideBarItemDescriptor> itemDescriptors) {
      // we don't need separate section label, only process items, only DEVICE_MANAGEMENT SideBarItems are used
      for (SideBarItemDescriptor item : itemDescriptors) {

        ItemButton itemComponent = (ItemButton) itemComponentFactory.createItemComponent(item);
        itemComponent.setCompositionRoot(compositionRoot);
        compositionRoot.addComponent(itemComponent);

        // skip submenu entry if view.name is RealmSettingsView
        if (item.getItemId().equals("sidebaritem_realmsettingsview")) continue;

        // find matching view-name for SideBarItemDescriptor
        Optional<Map.Entry<String, Class>> nameType = sideBarUtils.getNameTypeMap().entrySet().stream()
                                                           .filter(entry -> item.getItemId().contains(entry.getKey().toLowerCase()))
                                                           .findFirst();
        if (nameType.isPresent()) {
          Class sideBarItemClass = nameType.get().getValue();
          Object bean = sideBarUtils.getApplicationContext().getBean(sideBarItemClass);
          if (bean instanceof AbstractThinclientView) {
            FilterGrid filterGrid = new FilterGrid(item, (AbstractThinclientView) bean);
            compositionRoot.addComponent(filterGrid);
            itemsMap.put(item, filterGrid);
            if(item instanceof SideBarItemDescriptor.ViewItemDescriptor) {
              String viewName = ((SideBarItemDescriptor.ViewItemDescriptor) item).getViewName();
              filterGridMap.put(viewName, filterGrid);
              descriptorMap.put(viewName, item);
            }
          }
        }
      }
    }
  }

  class FilterGrid extends CssLayout {

    private final Label filterStatus;
    private final TextField filter;
    private List<? extends DirectoryObject> groupedItems;
    private Grid<DirectoryObject> itemGrid;

    public FilterGrid(SideBarItemDescriptor item, AbstractThinclientView bean) {
      setVisible(false);
      addStyleNames("filterGrid");

      CssLayout filterRow = new CssLayout();
      filterRow.addStyleNames("filterRow");
      filter = new TextField();
      filter.setPlaceholder("Filter");
//     filter.setIcon(VaadinIcons.FILTER);
//     filter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
      filter.addValueChangeListener(this::onFilterTextChange);
      filterRow.addComponent(filter);
      filterStatus = new Label();
      filterStatus.addStyleName("profileItemFilterStatus");
      filterRow.addComponent(filterStatus);
      addComponent(filterRow);

      itemGrid = new Grid<>();
      itemGrid.addStyleNames("profileSelectionGrid");
      itemGrid.addStyleName(item.getItemId().substring(SideBarItemDescriptor.ITEM_ID_PREFIX.length()));
      itemGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
      SingleSelectionModel<DirectoryObject> singleSelect = (SingleSelectionModel<DirectoryObject>) itemGrid.getSelectionModel();
      singleSelect.setDeselectAllowed(false);
      itemGrid.addColumn(DirectoryObject::getName);
      itemGrid.addSelectionListener(selectionEvent -> showContent(((AbstractThinclientView) bean).getViewName(), selectionEvent));
      itemGrid.removeHeaderRow(0);
      itemGrid.setSizeFull();
      itemGrid.setHeightMode(com.vaadin.shared.ui.grid.HeightMode.UNDEFINED);
      // Profile-Type based style
      itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());

      addComponent(itemGrid);

    }

    private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
      ListDataProvider<DirectoryObject> dataProvider = (ListDataProvider<DirectoryObject>) itemGrid.getDataProvider();
      long groupHeader = dataProvider.getItems().stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
      dataProvider.setFilter(directoryObject -> {
        if (directoryObject instanceof ProfilePropertiesBuilder.MenuGroupProfile) {
          return true;
        } else {
          return caseInsensitiveContains(directoryObject.getName(), event.getValue());
        }
      });
      long filteredGroupHeader = dataProvider.fetch(new Query<>()).filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
      filterStatus.setCaption((dataProvider.size(new Query<>())-filteredGroupHeader) + "/" + (dataProvider.getItems().size()-groupHeader));
    }

    private Boolean caseInsensitiveContains(String where, String what) {
      return where.toLowerCase().contains(what.toLowerCase());
    }


    public int getSize() {
      DataProvider<DirectoryObject, ?> dataProvider = this.itemGrid.getDataProvider();
      return dataProvider.size(new Query<>());
    }

    public void setItems(Set<DirectoryObject> items) {
      groupedItems = ProfilePropertiesBuilder.createGroupedItems(items);
      long groupHeader = groupedItems.stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
      ListDataProvider dataProvider = DataProvider.ofCollection(groupedItems);
      itemGrid.setDataProvider(dataProvider);
      long visibleItems = dataProvider.getItems().size() - groupHeader;
      filterStatus.setCaption(visibleItems + "/" + items.size());

      // TODO: Style festlegen für Anzeige Zeilenzahl
      if (visibleItems > 0) itemGrid.setHeightByRows(visibleItems);

      filter.setValue("");
    }

    public void markSelectedItem(String directoryObjectName) {
      if(directoryObjectName == null || directoryObjectName.isEmpty()) {
        itemGrid.deselectAll();
      } else {
        int pos = 0;
        for(DirectoryObject directoryObject: groupedItems) {
          if(directoryObject.getName().equals(directoryObjectName)) {
            // TODO: select, aber ohne navigator (durch selectetion-event) auszulösen
            itemGrid.getSelectionModel().select(directoryObject);
            itemGrid.scrollTo(pos);
            break;
          }
          pos++;
        }
      }
    }

    public DirectoryObject getSelectedItem() {
      return itemGrid.getSelectedItems().iterator().next();
    }
  }

  private void showContent(String viewName, SelectionEvent<DirectoryObject> selectionEvent) {
    // navigate to item
    Optional<DirectoryObject> selectedItem = selectionEvent.getFirstSelectedItem();
    if (selectionEvent.isUserOriginated()) {
      Navigator navigator = UI.getCurrent().getNavigator();
      if (selectedItem.isPresent()) {
        String navigationState = viewName + "/edit/" + selectedItem.get().getName();
        LOGGER.info("Navigate to " + navigationState);
        navigator.navigateTo(navigationState);
      }
    }
  }

  /**
   * Default implementation of {@link ItemComponentFactory} that creates
   * {@link com.vaadin.ui.Button}s.
   */
  public class DefaultItemComponentFactory implements ItemComponentFactory {

    @Override
    public Component createItemComponent(SideBarItemDescriptor descriptor) {
      if (descriptor instanceof SideBarItemDescriptor.ViewItemDescriptor) {
        return new ViewItemButton((SideBarItemDescriptor.ViewItemDescriptor) descriptor);
      } else {
        return new ItemButton(descriptor);
      }
    }
  }

  private Set<DirectoryObject> getAllItems(SideBarItemDescriptor item) {

    Optional<Map.Entry<String, Class>> nameType = sideBarUtils.getNameTypeMap().entrySet().stream()
        .filter(entry -> item.getItemId().contains(entry.getKey().toLowerCase()))
        .findFirst();

    if (nameType.isPresent()) {
      Class sideBarItemClass = nameType.get().getValue();
      LOGGER.debug("Fetch menu-items for {}", sideBarItemClass);
      Object bean = sideBarUtils.getApplicationContext().getBean(sideBarItemClass);
      if (bean instanceof AbstractThinclientView) {
        try {
          return ((AbstractThinclientView) bean).getAllItems();
        } catch (AllItemsListException e) {
          LOGGER.error("Cannot fetch all items for " + item.getItemId() + ": " + e.getMessage());
          return new HashSet<>();
        }
      }
    }
    return new HashSet<>();
  }

  /**
   * Extended version of {@link com.vaadin.ui.Button} that is used by the {@link ValoSideBar.DefaultItemComponentFactory}.
   */
  class ItemButton extends Button {

    CssLayout compositionRoot;

    ItemButton(final SideBarItemDescriptor descriptor) {
      setPrimaryStyleName(ValoTheme.MENU_ITEM);
      addStyleName(descriptor.getItemId().substring(SideBarItemDescriptor.ITEM_ID_PREFIX.length()));
      if(itemsMap.containsKey(descriptor)) {
        addStyleName("has-items");
      }
      setCaption(descriptor.getCaption());
      setIcon(descriptor.getIcon());
      setId(descriptor.getItemId());
      setDisableOnClick(true);
      addClickListener(event -> {
        try {
          descriptor.itemInvoked(getUI());
          if (compositionRoot != null && itemsMap.containsKey(descriptor)) {
            showGridItems(descriptor);
          } else {
            // cannot find sidbarItem to attach subItems, maybe no thinclientView
          }

        } finally {
          setEnabled(true);
        }
      });
    }

    public void setCompositionRoot(CssLayout compositionRoot) {
      this.compositionRoot = compositionRoot;
    }
  }

  private void showGridItems(SideBarItemDescriptor descriptor) {

    FilterGrid grid = itemsMap.get(descriptor);
    grid.setItems(getAllItems(descriptor));
    grid.setVisible(true);

    itemsMap.entrySet().stream().filter(e -> !e.getKey().equals(descriptor))
                                .forEach(e -> e.getValue().setVisible(false));
  }

  /**
   * Extended version of {@link ItemButton} that is used for view items. This
   * button keeps track of the currently selected view in the current UI's {@link com.vaadin.navigator.Navigator} and
   * updates its style so that the button of the currently visible view can be highlighted.
   */
  class ViewItemButton extends ItemButton implements ViewChangeListener {

    private final String viewName;
    private static final String STYLE_SELECTED = "selected";
    // hack: 'unselected' contains default-styles; without this, the browsers sticks in selection-mode at last selected item
    private static final String STYLE_UNSELECTED = "unselected";

    ViewItemButton(SideBarItemDescriptor.ViewItemDescriptor descriptor) {
      super(descriptor);
      viewName = descriptor.getViewName();
    }

    @Override
    public void attach() {
      super.attach();
      if (getUI().getNavigator() == null) {
        throw new IllegalStateException("Please configure the Navigator before you attach the SideBar to the UI");
      }
      getUI().getNavigator().addViewChangeListener(this);
    }

    @Override
    public void detach() {
      getUI().getNavigator().removeViewChangeListener(this);
      super.detach();
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {
      return true;
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
      View newView = event.getNewView();

      if (this.viewName.equals(event.getViewName()) || // settings section
          (newView instanceof AbstractThinclientView && viewName.equals(((AbstractThinclientView) newView).getParentViewName())) // managing section
         ) {
          removeStyleName(STYLE_UNSELECTED);
          addStyleName(STYLE_SELECTED);

          // beware ApplicationGroupView and UserGroupView
          final boolean isGroupView = (newView instanceof ApplicationGroupView || newView instanceof UserGroupView);

          // find the grid-items in selected view an do 'select-item' form highlighting
          String[] params = Optional.ofNullable(event.getParameters()).orElse("").split("/", 2);
          String currentObjectName = (params.length == 2 && "edit".equals(params[0]))? params[1] : "";
          Optional<SideBarItemDescriptor> descriptor = itemsMap.keySet()
              .stream()
              .filter(sbid -> sbid.getItemId().endsWith(viewName.replaceAll("_", "").toLowerCase()))
              .findFirst();
          if (descriptor.isPresent()) {
            Grid<DirectoryObject> grid = itemsMap.get(descriptor.get()).itemGrid;
            Optional<DirectoryObject> directoryObjectOptional = grid.getDataProvider()
                .fetch(new Query<>())
                .filter(d -> currentObjectName.equals(d.getName()))
                .findFirst();
            if (directoryObjectOptional.isPresent() && !isGroupView) {
              grid.select(directoryObjectOptional.get());
            } else {
              grid.deselectAll();
            }
          }

        } else {
          removeStyleName(STYLE_SELECTED);
          addStyleName(STYLE_UNSELECTED);

          // disable all other visible item lists
          FilterGrid filterGrid = filterGridMap.get(viewName);
          if (filterGrid != null) filterGrid.setVisible(false);
        }

    }
  }

  // AbstractSideBar
  @Override
  public void attach() {
    super.attach();

    if (getUI().getNavigator() == null) {
      throw new IllegalStateException("Please configure the Navigator before you attach the SideBar to the UI");
    }

    CssLayout compositionRoot = createCompositionRoot();
    setCompositionRoot(compositionRoot);
    for (SideBarSectionDescriptor section : sideBarUtils.getSideBarSections(getUI().getClass())) {
      // sectionId only
      if (section.getId().equals(this.sectionId)) {
        createSection(compositionRoot, section, sideBarUtils.getSideBarItems(section));
      }
    }
  }

  private void createSection(CssLayout compositionRoot, SideBarSectionDescriptor section, Collection<SideBarItemDescriptor> items) {

    if (getItemFilter() == null) {
      getSectionComponentFactory().createSection(compositionRoot, section, items);
    } else {
      List<SideBarItemDescriptor> passedItems = new ArrayList<SideBarItemDescriptor>();
      for (SideBarItemDescriptor candidate : items) {
        if (getItemFilter() .passesFilter(candidate)) {
          passedItems.add(candidate);
        }
      }
      if (!passedItems.isEmpty()) {
        getSectionComponentFactory().createSection(compositionRoot, section, passedItems);
      }
    }
  }

}
