
package org.openthinclient.web.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_LOGIN;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_NOTIFICATION_REMEMBERME_DESCRIPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_NOTIFICATION_REMEMBERME_TITLE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_PASSWORD;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_REMEMBERME;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_USERNAME;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_WELCOME;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.vaadin.spring.annotation.PrototypeScope;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.security.VaadinSecurity;
import org.vaadin.spring.security.util.SuccessfulLoginEvent;

/**
 * Full-screen UI component that allows the user to login.
 * 
 */
@SuppressWarnings("serial")
@PrototypeScope
@SpringComponent
public class LoginView extends VerticalLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoginView.class);

    private CheckBox rememberMe;
    private final IMessageConveyor mc;

    VaadinSecurity vaadinSecurity;;
  EventBus.SessionEventBus eventBus;
  RememberMeServices rememberMeServices;

    @Autowired
    public LoginView(VaadinSecurity vaadinSecurity, EventBus.SessionEventBus eventBus, RememberMeServices rememberMeServices) {

      this.vaadinSecurity = vaadinSecurity;
      this.eventBus = eventBus;
      this.rememberMeServices = rememberMeServices;
        mc = new MessageConveyor(UI.getCurrent().getLocale());

              setSizeFull();

        Component loginForm = buildLoginForm();
        addComponent(loginForm);
        setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);

    }

    private Component buildLoginForm() {
        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setSizeUndefined();
        Responsive.makeResponsive(loginPanel);
        loginPanel.addStyleName("login-panel");
        loginPanel.addComponent(buildLabels());

        Label loginFailed = new Label();
        loginFailed.setStyleName("login-failed");
        loginFailed.setVisible(false);
        loginPanel.addComponents(loginFailed);

        HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addStyleName("fields");

        final TextField username = new TextField(mc.getMessage(UI_LOGIN_USERNAME));
        username.setIcon(FontAwesome.USER);
        username.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        final PasswordField password = new PasswordField(mc.getMessage(UI_LOGIN_PASSWORD));
        password.setIcon(FontAwesome.LOCK);
        password.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        final Button signin = new Button(mc.getMessage(UI_LOGIN_LOGIN));
        signin.addStyleName(ValoTheme.BUTTON_PRIMARY);
        signin.setClickShortcut(KeyCode.ENTER);
        signin.focus();
        signin.addClickListener(new Button.ClickListener() {
          @Override
          public void buttonClick(final Button.ClickEvent event) {

              final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
              try {
                  final Authentication authentication = vaadinSecurity.login(username.getValue().toLowerCase(), password.getValue());
                  LOGGER.debug("Received UserLoginRequestedEvent for ", authentication.getPrincipal());

                  if (!userHasAuthorities()) {
                      throw new AccessDeniedException("User has insufficient rights.");
                  }

                  if (rememberMe.getValue()) {
                      VaadinServletRequest vaadinServletRequest = (VaadinServletRequest) VaadinService.getCurrentRequest();
                      VaadinServletResponse vaadinServletResponse = (VaadinServletResponse) VaadinService.getCurrentResponse();
                      vaadinServletRequest.getHttpServletRequest().setAttribute(AbstractRememberMeServices.DEFAULT_PARAMETER, Boolean.TRUE);
                      rememberMeServices.loginSuccess(vaadinServletRequest.getHttpServletRequest(), vaadinServletResponse.getHttpServletResponse(), authentication);
                  }
                eventBus.publish(this, new SuccessfulLoginEvent(getUI(), authentication));
              } catch (AuthenticationException | AccessDeniedException ex) {
                loginFailed.getParent().addStyleName("failed");
                loginFailed.setValue(mc.getMessage(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_FAILED));
                loginFailed.setVisible(true);
              } catch (Exception ex) {
                loginFailed.getParent().getParent().addStyleName("error");
                loginFailed.setValue(mc.getMessage(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_UNEXPECTED_ERROR));
                loginFailed.setVisible(true);
                  LOGGER.error("Unexpected error while logging in", ex);
              }
          }
        });

        fields.addComponents(username, password, signin);
        fields.setComponentAlignment(signin, Alignment.BOTTOM_LEFT);
        loginPanel.addComponent(fields);

        loginPanel.addComponent(rememberMe = new CheckBox(mc.getMessage(UI_LOGIN_REMEMBERME), false));
        rememberMe.addValueChangeListener(event -> {
          if (rememberMe.getValue()) {
            Notification notification = new Notification(mc.getMessage(UI_LOGIN_NOTIFICATION_REMEMBERME_TITLE));
            notification.setDescription(mc.getMessage(UI_LOGIN_NOTIFICATION_REMEMBERME_DESCRIPTION));
            notification.setHtmlContentAllowed(true);
            notification.setStyleName("tray dark small closable login-help");
            notification.setPosition(Position.BOTTOM_CENTER);
            notification.setDelayMsec(10000);
            notification.show(Page.getCurrent());
          }
        });
        return loginPanel;
    }


    private boolean userHasAuthorities() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean hasAuthorities = vaadinSecurity.hasAuthorities("ROLE_ADMINISTRATORS");
        return principal instanceof UserDetails && hasAuthorities;
    }

    private Component buildLabels() {
        CssLayout labels = new CssLayout();
        labels.addStyleName("labels");

        Label welcome = new Label(mc.getMessage(UI_LOGIN_WELCOME));
        welcome.setSizeUndefined();
        welcome.addStyleName(ValoTheme.LABEL_H4);
        welcome.addStyleName(ValoTheme.LABEL_COLORED);
        labels.addComponent(welcome);

        Label title = new Label("openthinclient.org");
        title.setSizeUndefined();
        title.addStyleName(ValoTheme.LABEL_H3);
        title.addStyleName(ValoTheme.LABEL_LIGHT);
        labels.addComponent(title);
        return labels;
    }


}
