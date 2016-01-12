
package org.openthinclient.web.ui;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.shared.Position;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.event.DashboardEvent;
import org.vaadin.spring.events.EventBus;

/**
 * Full-screen UI component that allows the user to login.
 * 
 */
public class LoginView extends CustomComponent {

    private final EventBus.SessionEventBus eventBus;
    
    private TextField userName;

    private PasswordField passwordField;

    private CheckBox rememberMe;

    private Button login;

    private Label loginFailedLabel;
    private Label loggedOutLabel;

    public LoginView(EventBus.SessionEventBus eventBus) {
        this.eventBus = eventBus;

        setSizeFull();

        VerticalLayout vl =  new VerticalLayout();
        
        Component loginForm = buildLoginForm();
        setCompositionRoot(vl);
        vl.addComponent(loginForm);
        vl.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);


        Notification notification = new Notification("Welcome to Dashboard Demo");
        notification.setDescription("<span>This application is not real, it only demonstrates an application built with the <a href=\"https://vaadin.com\">Vaadin framework</a>.</span> <span>User 'admin', pwd 'admin', click the <b>Sign In</b> button to continue.</span>");
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("tray dark small closable login-help");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.setDelayMsec(20000);
        notification.show(Page.getCurrent());

    }

    private Component buildLoginForm() {
        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setSizeUndefined();
        loginPanel.setSpacing(true);
        Responsive.makeResponsive(loginPanel);
        loginPanel.addStyleName("login-panel");

        loginPanel.addComponent(buildLabels());
        loginPanel.addComponent(buildFields());
        loginPanel.addComponent(new CheckBox("Remember me", true));
        return loginPanel;
    }

    private Component buildFields() {
        HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addStyleName("fields");

        final TextField username = new TextField("Username");
        username.setIcon(FontAwesome.USER);
        username.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        final PasswordField password = new PasswordField("Password");
        password.setIcon(FontAwesome.LOCK);
        password.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        final Button signin = new Button("Sign In --");
        signin.addStyleName(ValoTheme.BUTTON_PRIMARY);
        signin.setClickShortcut(KeyCode.ENTER);
        signin.focus();
        signin.addClickListener(new Button.ClickListener() {
        	@Override
        	public void buttonClick(final Button.ClickEvent event) {
             eventBus.publish(this, new DashboardEvent.UserLoginRequestedEvent(username.getValue(), password.getValue()));
        	}
        });

        fields.addComponents(username, password, signin);
        fields.setComponentAlignment(signin, Alignment.BOTTOM_LEFT);

        return fields;
    }

    private Component buildLabels() {
        CssLayout labels = new CssLayout();
        labels.addStyleName("labels");

        Label welcome = new Label("Welcome");
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
