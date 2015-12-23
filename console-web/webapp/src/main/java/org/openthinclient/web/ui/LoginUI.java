
package org.openthinclient.web.ui;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.security.VaadinSecurity;
import org.vaadin.spring.security.util.SuccessfulLoginEvent;

import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Full-screen UI component that allows the user to login.
 * 
 * @author Jörg Neumann (jne@mms-dresden.de)
 */
@SpringUI(path = "/login")
@Theme("dashboard")
public class LoginUI extends UI {

    @Autowired
    VaadinSecurity vaadinSecurity;

    @Autowired
    private EventBus.SessionEventBus eventBus;
    
    private TextField userName;

    private PasswordField passwordField;

    private CheckBox rememberMe;

    private Button login;

    private Label loginFailedLabel;
    private Label loggedOutLabel;

    @Override
    protected void init(VaadinRequest request) {

        setSizeFull();
    	addStyleName("loginview");
        
        VerticalLayout vl =  new VerticalLayout();
        
        Component loginForm = buildLoginForm();
        setContent(vl);
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
//        		DashboardEventBus.post(new UserLoginRequestedEvent(username.getValue(), password.getValue()));
        		try {
	        		final Authentication authentication = vaadinSecurity.login(username.getValue(), password.getValue(), /* rememberMe.getValue() */ false);
	                eventBus.publish(this, new SuccessfulLoginEvent(getUI(), authentication));
        		} catch (AuthenticationException ex) {
//        			userName.focus();
//        			userName.selectAll();
//        			passwordField.setValue("");
//        			loginFailedLabel.setValue(String.format("Login failed: %s",
//        					ex.getMessage()));
//        			loginFailedLabel.setVisible(true);
//        			if (loggedOutLabel != null) {
//        				loggedOutLabel.setVisible(false);
//        			}
        			Notification.show("Login failed", ex.getMessage(), Notification.Type.ERROR_MESSAGE);
        		} catch (Exception ex) {
        			Notification.show("An unexpected error occurred", ex.getMessage(),
        					Notification.Type.ERROR_MESSAGE);
        			LoggerFactory.getLogger(getClass()).error(
        					"Unexpected error while logging in", ex);
        		}
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
