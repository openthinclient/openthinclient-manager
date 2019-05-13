package org.openthinclient.web;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.web.sidebar.OTCSideBarUtils;
import org.openthinclient.web.thinclient.ThinclientView;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.sidebar.SideBarItemDescriptor;
import org.vaadin.spring.sidebar.SideBarSectionDescriptor;
import org.vaadin.spring.sidebar.components.ValoSideBar;

import java.util.*;

public class OTCSideBar extends ValoSideBar implements ViewChangeListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(OTCSideBar.class);

  public static final String SIDE_BAR_STYLE = "sideBar";
  public static final String SIDE_BAR_SECTION_ITEM_STYLE = "sideBarSectionItem";
  public static final String SIDE_BAR_SECTION_STYLE = "sideBarSection";
  public static final String SELECTED_STYLE = "selected";

  private OTCSideBarUtils sideBarUtils;
  private Map<SideBarItemDescriptor, Grid<DirectoryObject>> itemsMap = new HashMap<SideBarItemDescriptor, Grid<DirectoryObject>>();

  /**
   * You should not need to create instances of this component directly. Instead, just inject the side bar into
   * your UI.
   *
   * @param sideBarUtils
   */
  public OTCSideBar(OTCSideBarUtils sideBarUtils) {
    super(sideBarUtils);
    this.sideBarUtils = sideBarUtils;
  }

  @Override
  protected SectionComponentFactory<CssLayout> createDefaultSectionComponentFactory() {
    return new DefaultSectionComponentFactory();
  }

  @Override
  protected ItemComponentFactory createDefaultItemComponentFactory() {
    return new DefaultItemComponentFactory();
  }

  @Override
  public boolean beforeViewChange(ViewChangeEvent event) {
    return true;
  }

  @Override
  public void afterViewChange(ViewChangeEvent event) {

    String state = event.getNavigator().getState();
    String viewName = event.getViewName();
    Map<String, String> parameterMap = event.getParameterMap();
    LOGGER.debug("Current view {} {}", state, parameterMap);

    if (state != null && parameterMap.size() == 1) {

      // extract directoryObjectName if only one parameter is in map
      String directoryObjectName = parameterMap.keySet().iterator().next();

      Optional<SideBarItemDescriptor> descriptor = itemsMap.keySet().stream().filter(sideBarItemDescriptor ->
          sideBarItemDescriptor.getItemId().endsWith(viewName.replaceAll("_", "").toLowerCase())
      ).findFirst();

      if (descriptor.isPresent()) {
        Grid<DirectoryObject> grid = itemsMap.get(descriptor.get());
        // load items if not already loaded
        DataProvider<DirectoryObject, ?> dataProvider = grid.getDataProvider();
        int size = dataProvider.size(new Query<>());
        if (size == 0) {
          HashSet<DirectoryObject> directoryObjects = getAllItems(descriptor.get());
          grid.setItems(directoryObjects);
          // TODO: Style festlegen für Anzeige Zeilenzahl
          if (directoryObjects.size() > 0) grid.setHeightByRows(directoryObjects.size());
        }

        // mark item selected
        grid.getDataProvider().fetch(new Query<>())
                              .filter(directoryObject -> directoryObject.getName().equals(directoryObjectName))
                              .findFirst().ifPresent(directoryObject -> {
         // TODO: selcet, aber ohne navigator (durch selectetion-event) auszulösen
          grid.getSelectionModel().select(directoryObject);
        });
        grid.setVisible(true);

        // hide other open items-tables
        itemsMap.values().stream().filter(gridComponent -> !gridComponent.equals(grid))
                                  .forEach(gridComponent -> gridComponent.setVisible(false));
      }

    } else {

      // hide all open items-tables
      itemsMap.values().forEach(gridComponent -> gridComponent.setVisible(false));
    }

  }

  public void selectItem(String viewName, DirectoryObject directoryObject, HashSet<DirectoryObject> directoryObjectSet) {
    Optional<SideBarItemDescriptor> descriptor = itemsMap.keySet().stream().filter(sideBarItemDescriptor ->
        sideBarItemDescriptor.getItemId().endsWith(viewName.replaceAll("_", "").toLowerCase())
    ).findFirst();

    if (descriptor.isPresent()) {
      Grid<DirectoryObject> grid = itemsMap.get(descriptor.get());
      grid.setItems(directoryObjectSet);
      // TODO: Style festlegen für Anzeige Zeilenzahl
      if (directoryObjectSet.size() > 0) grid.setHeightByRows(directoryObjectSet.size());

      grid.getDataProvider().fetch(new Query<>())
          .filter(directoryObject1 -> directoryObject1.getName().equals(directoryObject.getName()))
          .findFirst().ifPresent(directoryObject1 -> {
        grid.getSelectionModel().select(directoryObject1);
      });
    }
  }

  public DirectoryObject getSelectedItem(String viewName) {
    Optional<SideBarItemDescriptor> descriptor = itemsMap.keySet().stream().filter(sideBarItemDescriptor ->
        sideBarItemDescriptor.getItemId().endsWith(viewName.replaceAll("_", "").toLowerCase())
    ).findFirst();

    if (descriptor.isPresent()) {
      Grid<DirectoryObject> grid = itemsMap.get(descriptor.get());
      return grid.getSelectedItems().iterator().next();
    }
    return null;
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
      // we don't need separate section label, only process items
      for (SideBarItemDescriptor item : itemDescriptors) {
        ItemButton itemComponent = (ItemButton) itemComponentFactory.createItemComponent(item);
        itemComponent.setCompositionRoot(compositionRoot);
        compositionRoot.addComponent(itemComponent);

        // find matching view-name for SideBarItemDescriptor
        Optional<Map.Entry<String, Class>> nameType = sideBarUtils.getNameTypeMap().entrySet().stream()
                                                           .filter(entry -> item.getItemId().contains(entry.getKey().toLowerCase()))
                                                           .findFirst();
        if (nameType.isPresent()) {
          Class sideBarItemClass = nameType.get().getValue();
          Object bean = sideBarUtils.getApplicationContext().getBean(sideBarItemClass);
          if (bean instanceof ThinclientView) {

            Grid<DirectoryObject> itemGrid = new Grid<>();
            itemGrid.addStyleNames("profileSelectionGrid");
            itemGrid.addStyleName(item.getItemId().substring(SideBarItemDescriptor.ITEM_ID_PREFIX.length()));
            itemGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
            itemGrid.addColumn(DirectoryObject::getName);
            itemGrid.addSelectionListener(selectionEvent -> showContent(((ThinclientView) bean).getViewName(), selectionEvent));
            itemGrid.removeHeaderRow(0);
            itemGrid.setSizeFull();
            itemGrid.setHeightMode(com.vaadin.shared.ui.grid.HeightMode.UNDEFINED);
            // Profile-Type based style
            itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());

            // add some data
//            List groupedItems = ProfilePropertiesBuilder.createGroupedItems(items);
//            long groupHeader = groupedItems.stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
//            ListDataProvider dataProvider = DataProvider.ofCollection(groupedItems);

            itemGrid.setVisible(false);
            compositionRoot.addComponent(itemGrid);

            itemsMap.put(item, itemGrid);

          } else {

          }
        }

      }
    }
  }

  private void showContent(String viewName, SelectionEvent<DirectoryObject> selectionEvent) {

    // navigate to item
    Optional<DirectoryObject> selectedItem = selectionEvent.getFirstSelectedItem();
    if (selectionEvent.isUserOriginated()) {
      Navigator navigator = UI.getCurrent().getNavigator();
      if (selectedItem.isPresent()) {
        navigator.navigateTo(viewName + "/" + selectedItem.get().getName());
      } else {
        navigator.navigateTo(viewName);

        Optional<SideBarItemDescriptor> descriptor = itemsMap.keySet().stream().filter(sideBarItemDescriptor ->
            sideBarItemDescriptor.getItemId().endsWith(viewName.replaceAll("_", "").toLowerCase())
        ).findFirst();
        if (descriptor.isPresent()) {
          showGridItems(descriptor.get());
        }
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

  private HashSet<DirectoryObject> getAllItems(SideBarItemDescriptor item) {

    Optional<Map.Entry<String, Class>> nameType = sideBarUtils.getNameTypeMap().entrySet().stream()
        .filter(entry -> item.getItemId().contains(entry.getKey().toLowerCase()))
        .findFirst();

    if (nameType.isPresent()) {
      Class sideBarItemClass = nameType.get().getValue();
      LOGGER.debug("Fetch menu-items for {}", sideBarItemClass);
      Object bean = sideBarUtils.getApplicationContext().getBean(sideBarItemClass);
      if (bean instanceof ThinclientView) {
        try {
          return ((ThinclientView) bean).getAllItems();
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

    Grid<DirectoryObject> grid = itemsMap.get(descriptor);
    HashSet<DirectoryObject> allItems = getAllItems(descriptor);
    grid.setItems(allItems);
    // TODO: Style festlegen für Anzeige Zeilenzahl
    if (allItems.size() > 0) grid.setHeightByRows(allItems.size());
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
      if (event.getViewName().equals(viewName)) {
        addStyleName(STYLE_SELECTED);
        // TODO: disable all other visible item lists
        // ...
//        String parameters = event.getParameters();
//
//        Optional<SideBarItemDescriptor> descriptor = itemsMap.keySet().stream().filter(sideBarItemDescriptor ->
//            sideBarItemDescriptor.getItemId().endsWith(viewName.replaceAll("_", "").toLowerCase())
//        ).findFirst();
//
//        if (descriptor.isPresent()) {
//          Grid<DirectoryObject> grid = itemsMap.get(descriptor.get());
//
//        }


      } else {
        removeStyleName(STYLE_SELECTED);
      }
    }
  }

  // AbstratctSideBar
  @Override
  public void attach() {
    super.attach();

    if (getUI().getNavigator() == null) {
      throw new IllegalStateException("Please configure the Navigator before you attach the SideBar to the UI");
    }
    getUI().getNavigator().addViewChangeListener(this);

    CssLayout compositionRoot = createCompositionRoot();
    setCompositionRoot(compositionRoot);
    for (SideBarSectionDescriptor section : sideBarUtils.getSideBarSections(getUI().getClass())) {
      createSection(compositionRoot, section, sideBarUtils.getSideBarItems(section));
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
