package org.openthinclient.webconsole.app;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;

/**
 * Created by francois on 20.07.14.
 */
public class MainApplicationView extends HorizontalLayout {

  CssLayout menu = new CssLayout();
  CssLayout content = new CssLayout();
  private final Navigator nav;

  public MainApplicationView(UI ui) {
    nav = new Navigator(ui, content);

    //    for (String route : routes.keySet()) {
    //      nav.addView(route, routes.get(route));
    //    }

    setSizeFull();
    addStyleName("main-view");
    addComponent(new Sidebar());
    // Content
    addComponent(content);
    content.setSizeFull();
    content.addStyleName("view-content");
    setExpandRatio(content, 1);

    menu.removeAllComponents();

    for (final String view : new String[]{"dashboard", "sales", "transactions", "reports", "schedule"}) {
      Button b = new NativeButton(view.substring(0, 1).toUpperCase() + view.substring(1).replace('-', ' '));
      b.addStyleName("icon-" + view);
      b.addClickListener(new Button.ClickListener() {
        @Override
        public void buttonClick(Button.ClickEvent event) {
          clearMenuSelection();
          event.getButton().addStyleName("selected");
          if (!nav.getState().equals("/" + view))
            nav.navigateTo("/" + view);
        }
      });

      if (view.equals("reports")) {
        // Add drop target to reports button
        DragAndDropWrapper reports = new DragAndDropWrapper(b);
        reports.setDragStartMode(DragAndDropWrapper.DragStartMode.NONE);
        reports.setDropHandler(new DropHandler() {

          @Override
          public void drop(DragAndDropEvent event) {
            clearMenuSelection();
            viewNameToMenuButton.get("/reports").addStyleName("selected");
            autoCreateReport = true;
            items = event.getTransferable();
            nav.navigateTo("/reports");
          }

          @Override
          public AcceptCriterion getAcceptCriterion() {
            return AbstractSelect.AcceptItem.ALL;
          }

        });
        menu.addComponent(reports);
        menu.addStyleName("no-vertical-drag-hints");
        menu.addStyleName("no-horizontal-drag-hints");
      } else {
        menu.addComponent(b);
      }

      viewNameToMenuButton.put("/" + view, b);
    }
    menu.addStyleName("menu");
    menu.setHeight("100%");

    viewNameToMenuButton.get("/dashboard").setHtmlContentAllowed(true);
    viewNameToMenuButton.get("/dashboard").setCaption("Dashboard<span class=\"badge\">2</span>");

    String f = Page.getCurrent().getUriFragment();
    if (f != null && f.startsWith("!")) {
      f = f.substring(1);
    }
    if (f == null || f.equals("") || f.equals("/")) {
      nav.navigateTo("/dashboard");
      menu.getComponent(0).addStyleName("selected");
      helpManager.showHelpFor(DashboardView.class);
    } else {
      nav.navigateTo(f);
      helpManager.showHelpFor(routes.get(f));
      viewNameToMenuButton.get(f).addStyleName("selected");
    }

    nav.addViewChangeListener(new ViewChangeListener() {

      @Override
      public boolean beforeViewChange(ViewChangeListener.ViewChangeEvent event) {
        helpManager.closeAll();
        return true;
      }

      @Override
      public void afterViewChange(ViewChangeListener.ViewChangeEvent event) {
        View newView = event.getNewView();
        helpManager.showHelpFor(newView);
        if (autoCreateReport && newView instanceof ReportsView) {
          ((ReportsView) newView).autoCreate(2, items, transactions);
        }
        autoCreateReport = false;
      }
    });

  }

  private class Sidebar extends VerticalLayout {
    // Sidebar
    {
      addStyleName("sidebar");
      setWidth(null);
      setHeight("100%");

      // Branding element
      addComponent(new BrandingComponent());

      // Main menu
      addComponent(menu);
      setExpandRatio(menu, 1);

      // User menu
      MenuBar.Command cmd = new MenuBar.Command() {
        public void menuSelected(MenuBar.MenuItem selectedItem) {
          Notification.show("Not implemented in this demo");
        }
      };

      UserMenuComponent userMenuComponent = new UserMenuComponent();
      userMenuComponent.addSettingsSubmenuItem("Settings", cmd);
      userMenuComponent.addSettingsSubmenuItem("Preferences", cmd);
      userMenuComponent.addSettingsSubmenuSeparator();
      userMenuComponent.addSettingsSubmenuItem("My Account", cmd);

      addComponent(userMenuComponent);
    }

    private class UserMenuComponent extends VerticalLayout {

      private final MenuBar.MenuItem settingsMenu;

      public UserMenuComponent(String userName) {
        setSizeUndefined();
        addStyleName("user");
        Image profilePic = new Image(null, new ThemeResource("img/profile-pic.png"));
        profilePic.setWidth("34px");
        addComponent(profilePic);
        Label userNameLabel = new Label(userName);
        userNameLabel.setSizeUndefined();
        addComponent(userNameLabel);

        MenuBar settings = new MenuBar();
        settingsMenu = settings.addItem("", null);
        settingsMenu.setStyleName("icon-cog");
        addComponent(settings);

        Button exit = new NativeButton("Exit");
        exit.addStyleName("icon-cancel");
        exit.setDescription("Sign Out");
        addComponent(exit);
        exit.addClickListener(new Button.ClickListener() {
          @Override
          public void buttonClick(Button.ClickEvent event) {
            buildLoginView(true);
          }
        });
      }

      private void addSettingsSubmenuSeparator() {settingsMenu.addSeparator();}

      private void addSettingsSubmenuItem(String title, MenuBar.Command cmd) {settingsMenu.addItem(title, cmd);}
    }

  }

}
