package org.openthinclient.web;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.spring.sidebar.SideBarItemDescriptor;
import org.vaadin.spring.sidebar.SideBarSectionDescriptor;
import org.vaadin.spring.sidebar.SideBarUtils;
import org.vaadin.spring.sidebar.components.ValoSideBar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OTCSideBar extends ValoSideBar {

  public static final String SIDE_BAR_STYLE = "sideBar";
  public static final String SIDE_BAR_SECTION_ITEM_STYLE = "sideBarSectionItem";
  public static final String SIDE_BAR_SECTION_STYLE = "sideBarSection";
  public static final String SELECTED_STYLE = "selected";

  SideBarUtils sideBarUtils;

  /**
   * You should not need to create instances of this component directly. Instead, just inject the side bar into
   * your UI.
   *
   * @param sideBarUtils
   */
  public OTCSideBar(SideBarUtils sideBarUtils) {
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
      // we don't need separate section label
//      Label header = new Label();
//      header.setValue(descriptor.getCaption());
//      header.setSizeUndefined();
//      header.setPrimaryStyleName(ValoTheme.MENU_SUBTITLE);
//      compositionRoot.addComponent(header);
      for (SideBarItemDescriptor item : itemDescriptors) {
        ItemButton itemComponent = (ItemButton) itemComponentFactory.createItemComponent(item);
        itemComponent.setCompositionRoot(compositionRoot);
        compositionRoot.addComponent(itemComponent);
        // TODO: kann map mit Button/ItemSubList innerhalb der SideBar sein
        // Panel oder VerticalLayout
        VerticalLayout itemList = new VerticalLayout();
        itemList.setMargin(false);
        itemList.setStyleName("subliste-mit-items");
        compositionRoot.addComponent(itemList);
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

  /**
   * Extended version of {@link com.vaadin.ui.Button} that is used by the {@link ValoSideBar.DefaultItemComponentFactory}.
   */
  static class ItemButton extends Button {

    CssLayout compositionRoot;

    ItemButton(final SideBarItemDescriptor descriptor) {
      setPrimaryStyleName(ValoTheme.MENU_ITEM);
      setCaption(descriptor.getCaption());
      setIcon(descriptor.getIcon());
      setId(descriptor.getItemId());
      setDisableOnClick(true);
      addClickListener(event -> {
        try {
          descriptor.itemInvoked(getUI());
          if (compositionRoot != null) {
            for (int i=0;i<compositionRoot.getComponentCount(); i++) {
              if (compositionRoot.getComponent(i).equals(this)) {
                VerticalLayout itemList = (VerticalLayout) compositionRoot.getComponent(i + 1);
                itemList.addComponent(new Label("detail"));
                break;
              }
            }
          } else {
            // cannot find sidbarItem to attach subItems
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

  /**
   * Extended version of {@link ItemButton} that is used for view items. This
   * button keeps track of the currently selected view in the current UI's {@link com.vaadin.navigator.Navigator} and
   * updates its style so that the button of the currently visible view can be highlighted.
   */
  static class ViewItemButton extends ItemButton implements ViewChangeListener {

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

      } else {
        removeStyleName(STYLE_SELECTED);
      }
    }
  }

  // AbstratctSideBar
  @Override
  public void attach() {
    super.attach();
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
