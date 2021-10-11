package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import com.kstruct.gethostname4j.Hostname;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.service.common.license.LicenseChangeEvent;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.component.LicenseMessageBar;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.ClientCountChangeEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.PostConstruct;

@Theme("openthinclient")
@Push(PushMode.MANUAL)
@com.vaadin.annotations.JavaScript({"vaadin://js/UIFunctions.js"})
public abstract class AbstractUI extends UI implements ViewDisplay {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUI.class);

  abstract protected OTCSideBar getSideBar();

  @Autowired
  ApplicationContext applicationContext;
  @Autowired
  VaadinSecurity vaadinSecurity;
  @Autowired
  SpringViewProvider viewProvider;
  @Autowired
  SpringViewProvider springViewProvider;
  @Autowired
  private EventBus.SessionEventBus eventBus;
  @Autowired
  private RealmService realmService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private UserService userService;
  @Autowired
  private LicenseManager licenseManager;

  @Value("${application.is-preview}")
  private boolean applicationIsPreview;

  private Panel springViewDisplay;

  protected IMessageConveyor mc;
  private AbstractOrderedLayout root;
  private UserProfileSubWindow userProfileWindow;
  private LicenseMessageBar licenseMessageBar;

  @PostConstruct
  public void init() {
    springViewProvider.setAccessDeniedViewClass(AccessDeniedView.class);
  }

  @Override
  protected void init(VaadinRequest request) {

    setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));
    Locale.setDefault(UI.getCurrent().getLocale()); // necessary for messages read from schemas

    addStyleName(ValoTheme.UI_WITH_MENU);

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    // Some views need to be aware of browser resize events so a
    // BrowserResizeEvent gets fired to the event bus on every occasion.
    Page.getCurrent().addBrowserWindowResizeListener(event ->
      eventBus.publish(this, new BrowserResizeEvent(event.getHeight(), event.getWidth())));

    Page.getCurrent().setTitle(mc.getMessage(ConsoleWebMessages.UI_PAGE_TITLE));

    licenseMessageBar = new LicenseMessageBar(licenseManager, clientService);

    showMainScreen();

    JavaScript.getCurrent().execute("installGridTooltips()");
    JavaScript.getCurrent().execute("installInfoButtonFunction()");

    addClickListener(e -> eventBus.publish(e, new CloseOpenWindowsEvent()));

    userProfileWindow = new UserProfileSubWindow(userService);
  }

  protected void afterNavigatorViewChange(ViewChangeEvent event) {
    JavaScript.getCurrent().execute("disableSpellcheck()");
    JavaScript.getCurrent().execute("installCopyOnClick()");
  }

  /**
   *
   * |-------------|-----------------|
   * |   Logo      |     Header      |
   * |-------------|-----------------|
   * |   Section   |                 |
   * |-------------|    Content      |
   * |    Item     |                 |
   * |    Item     |                 |
   * |    Item     |                 |
   * |             |                 |
   * |-------------|-----------------|
   */
  private void showMainScreen() {
    OTCSideBar sideBar = getSideBar();

    String logoFileName = applicationIsPreview? "logo-beta.svg": "logo.svg";
    Image image = new Image(null, new ThemeResource(logoFileName));
    image.addClickListener(e -> UI.getCurrent().getPage().setLocation("/"));
    image.addStyleName("logo-button");
    image.removeStyleName(ValoTheme.MENU_LOGO);
    sideBar.setLogo(image);

    root = new HorizontalLayout();
    root.setSpacing(false);
    root.setSizeFull();
    sideBar.setId("mainmenu");
    root.addComponent(sideBar);

    VerticalLayout vl = new VerticalLayout();
    vl.setSpacing(false);
    vl.setMargin(false);
    vl.setSizeFull();

    vl.addComponents(buildHeader(), licenseMessageBar);

    ComponentContainer content = new CssLayout();
    content.addStyleName("view-content");
    content.setSizeFull();
    vl.addComponent(content);
    vl.setExpandRatio(content, 1.0f);

    final Navigator navigator = new Navigator(UI.getCurrent(), content);
    navigator.addViewChangeListener(new ViewChangeListener() {
        @Override
        public boolean beforeViewChange(ViewChangeEvent event) {
          return true;
        }

        @Override
        public void afterViewChange(ViewChangeEvent event) {
          afterNavigatorViewChange(event);
        }
    });
    navigator.addProvider(viewProvider);
    if (navigator.getState().isEmpty()) {
      navigator.navigateTo(getInitialView());
    } else {
      navigator.navigateTo(navigator.getState());
    }

    root.addComponents(vl);
    root.setExpandRatio(vl, 1.0f);

    setContent(root);
  }

  protected String getInitialView() {
    return "";
  }

  @EventBusListenerMethod
  public void licenseChange(LicenseChangeEvent ev) {
    this.updateLicenseBar();
  }

  @EventBusListenerMethod
  public void userCountChange(ClientCountChangeEvent ev) {
    this.updateLicenseBar();
  }

  private void updateLicenseBar() {
    if(licenseMessageBar != null) {
      licenseMessageBar.updateContent();
      this.push();
    }
  }

  @Override
  public void attach() {
      super.attach();
      eventBus.subscribe(this);
  }

  @Override
  public void detach() {
      eventBus.unsubscribe(this);
      super.detach();
  }

  abstract protected Component buildHeader();

  protected Component getRealmLabel() {
      Optional<Realm> realm = realmService.findAllRealms().stream().findFirst();
      String description = realm.isPresent()? realm.get().getDescription() : "";
      String hostname = Hostname.getHostname();

      Layout realmLabel = new CssLayout();
      realmLabel.addStyleName("realm-label");
      realmLabel.addComponents(
      new Label(hostname),
      new Label(description)
      );

      return realmLabel;
  }

  protected Component buildLogoutButton() {

    UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    MenuBar menuBar = new MenuBar();
    menuBar.addStyleName(ValoTheme.MENUBAR_SMALL);
    menuBar.addStyleName("header-menu");

    final MenuBar.MenuItem file = menuBar.addItem(principal.getUsername(), null);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_PROFILE), this::showProfileSubWindow);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_LOGOUT), this::onLogoutButton);

    return menuBar;
  }

  private void onLogoutButton(MenuBar.MenuItem menuItem) {
    // When the user logs out, current VaadinSession gets closed and the
    // page gets reloaded on the login screen. Do notice the this doesn't
    // invalidate the current HttpSession.
    VaadinSession.getCurrent().close();
    vaadinSecurity.logout();
  }

  private void showProfileSubWindow(MenuBar.MenuItem menuItem) {

    if (!UI.getCurrent().getWindows().contains(userProfileWindow)) {
      SecurityContext securityContext = (SecurityContext) VaadinSession.getCurrent().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
      Authentication authentication = securityContext.getAuthentication();
      UserDetails principal = (UserDetails) authentication.getPrincipal();
      try {
        userProfileWindow.refresh(userService.findByName(principal.getUsername()));
      } catch (Exception e) {
        LOGGER.warn("Cannot find directory-object: " + e.getMessage());
        userProfileWindow.showError(e);
      }
      UI.getCurrent().addWindow(userProfileWindow);
    } else {
      userProfileWindow.close();
      UI.getCurrent().removeWindow(userProfileWindow);
    }
  }


  @Override
  public void showView(View view) {
    if (springViewDisplay != null) springViewDisplay.setContent((Component)view);
  }
}
