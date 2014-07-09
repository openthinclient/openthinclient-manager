package org.openthinclient.webconsole.app;

import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

/**
 * A view, which provies a nice looking login screen.
 */
public class LoginView extends VerticalLayout {

  public LoginView() {
    setSizeFull();
    addStyleName("login-layout");

    final CssLayout loginPanel = new CssLayout();
    loginPanel.addStyleName("login-panel");

    HorizontalLayout labels = new HorizontalLayout();
    labels.setWidth("100%");
    labels.setMargin(true);
    labels.addStyleName("labels");
    loginPanel.addComponent(labels);

    Label welcome = new Label("Welcome");
    welcome.setSizeUndefined();
    welcome.addStyleName("h4");
    labels.addComponent(welcome);
    labels.setComponentAlignment(welcome, Alignment.MIDDLE_LEFT);

    Label title = new Label("openthinclient Manager");
    title.setSizeUndefined();
    title.addStyleName("h2");
    title.addStyleName("light");
    labels.addComponent(title);
    labels.setComponentAlignment(title, Alignment.MIDDLE_RIGHT);

    HorizontalLayout fields = new HorizontalLayout();
    fields.setSpacing(true);
    fields.setMargin(true);
    fields.addStyleName("fields");

    final TextField username = new TextField("Username");
    username.focus();
    fields.addComponent(username);

    final PasswordField password = new PasswordField("Password");
    fields.addComponent(password);

    final Button signin = new Button("Sign In");
    signin.addStyleName("default");
    fields.addComponent(signin);
    fields.setComponentAlignment(signin, Alignment.BOTTOM_LEFT);

    final ShortcutListener enter = new ShortcutListener("Sign In", ShortcutAction.KeyCode.ENTER, null) {
      @Override
      public void handleAction(Object sender, Object target) {
        signin.click();
      }
    };

    signin.addClickListener(new Button.ClickListener() {

      public void buttonClick(Button.ClickEvent event) {
        if (username.getValue() != null && username.getValue().equals("") && password.getValue() != null
            && password.getValue().equals("")) {
          signin.removeShortcutListener(enter);

          // FIXME navigate to the actual dashboard

          // buildMainView();
        } else {
          if (loginPanel.getComponentCount() > 2) {
            // Remove the previous error message
            loginPanel.removeComponent(loginPanel.getComponent(2));
          }
          // Add new error message
          Label error = new Label("Wrong username or password.", ContentMode.HTML);
          error.addStyleName("error");
          error.setSizeUndefined();
          error.addStyleName("light");
          // Add animation
          error.addStyleName("v-animate-reveal");
          loginPanel.addComponent(error);
          username.focus();
        }
      }
    });

    signin.addShortcutListener(enter);

    loginPanel.addComponent(fields);

    addComponent(loginPanel);
    setComponentAlignment(loginPanel, Alignment.MIDDLE_CENTER);

  }
}
