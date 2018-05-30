
package org.openthinclient.web.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_LOGIN;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_NOTIFICATION_DESCRIPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_NOTIFICATION_REMEMBERME_DESCRIPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_NOTIFICATION_REMEMBERME_TITLE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOGIN_NOTIFICATION_TITLE;
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
import com.vaadin.shared.Position;
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
import org.openthinclient.web.event.DashboardEvent;
import org.vaadin.spring.events.EventBus;

/**
 * Full-screen UI component that allows the user to login.
 * 
 */
@SuppressWarnings("serial")
public class LoginView extends VerticalLayout {

    private final EventBus.SessionEventBus eventBus;
    private CheckBox rememberMe;
    private final IMessageConveyor mc;
    
    public LoginView(EventBus.SessionEventBus eventBus) {
        this.eventBus = eventBus;
        mc = new MessageConveyor(UI.getCurrent().getLocale());
        
        setSizeFull();

        Component loginForm = buildLoginForm();
        addComponent(loginForm);
        setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);

        Notification notification = new Notification(mc.getMessage(UI_LOGIN_NOTIFICATION_TITLE));
        notification.setDescription(mc.getMessage(UI_LOGIN_NOTIFICATION_DESCRIPTION));
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("tray dark small closable login-help");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.setDelayMsec(10000);
        notification.show(Page.getCurrent());

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
            eventBus.publish(this, new DashboardEvent.UserLoginRequestedEvent(username.getValue().toLowerCase(), password.getValue(), rememberMe.getValue(), loginFailed));
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
