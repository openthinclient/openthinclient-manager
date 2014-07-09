package org.openthinclient.webconsole.app;


import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.*;

/**
 * Created by francois on 08.07.14.
 */
@Theme("otc-console")
public class ConsoleApp extends UI {

  private static final long serialVersionUID = 1L;
  private final CssLayout root = new CssLayout();

  @Override protected void init(VaadinRequest vaadinRequest) {

    setContent(root);
    root.addStyleName("root");
    root.setSizeFull();

    Label bg = new Label();
    bg.setSizeUndefined();
    bg.addStyleName("login-bg");
    root.addComponent(bg);

    buildLoginView(false);
  }

  private void buildLoginView(boolean exit) {
    if (exit) {
      root.removeAllComponents();
    }

    addStyleName("login");
    root.addComponent(new LoginView());
  }

}
