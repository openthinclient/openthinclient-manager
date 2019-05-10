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
import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringUI;
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
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.security.shared.VaadinSharedSecurity;

/**
 * LoginUI
 */
@SpringUI(path = "/login")
@Theme("openthinclient")
public class LoginUI extends UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginUI.class);

    @Autowired
    VaadinSharedSecurity vaadinSecurity;

    private IMessageConveyor mc;
    private CheckBox rememberMe;

    @Override
    protected void init(VaadinRequest request) {

        setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        Component loginForm = buildLoginForm();
        VerticalLayout rootLayout = new VerticalLayout(loginForm);
        rootLayout.setSizeFull();
        rootLayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
        setContent(rootLayout);
        setSizeFull();

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
                  final Authentication authentication = vaadinSecurity.login(username.getValue(), password.getValue(), rememberMe.getValue());
                  LOGGER.debug("Received UserLoginRequestedEvent for ", authentication.getPrincipal());
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

        return loginPanel;
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
