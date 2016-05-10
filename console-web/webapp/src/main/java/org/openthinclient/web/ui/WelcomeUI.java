package org.openthinclient.web.ui;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.security.VaadinSecurity;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Theme("dashboard")
@Title("openthinclient.org")
@SpringUI(path = "/welcome")
public final class WelcomeUI extends UI {

  /** serialVersionUID */
  private static final long serialVersionUID = -5094140681208354084L;
  
  @Autowired
  VaadinSecurity vaadinSecurity;
  @Autowired
  SpringViewProvider viewProvider;

  @Override
  protected void init(final VaadinRequest request) {
    setLocale(Locale.US);

    Responsive.makeResponsive(this);
    addStyleName(ValoTheme.UI_WITH_MENU);

    updateContent();
  }

  /**
   * Setup the content
   */
  private void updateContent() {

    GridLayout grid = new GridLayout(3, 3);

    grid.setWidth(100, Unit.PERCENTAGE);
    grid.setHeight(100, Unit.PERCENTAGE);

    VerticalLayout vl = new VerticalLayout();
    vl.setWidth(400, Unit.PIXELS);
    grid.addComponent(vl, 1, 1); 
    
    // logo
    final Image logoImage = new Image();
    logoImage.setSource(new ThemeResource("img/OpenThinClient-logo.svg.png"));
    logoImage.setWidth(400, Unit.PIXELS);
    vl.addComponent(logoImage);
    
    // ruler
    vl.addComponent(new Label("<hr/>", ContentMode.HTML));

    // headline
    vl.addComponent(new Label("WELCOME"));
    vl.addComponent(new Label("&nbsp;", ContentMode.HTML));

    // paragraph
    vl.addComponent(
        new Label("The new openthinclient WebConsole frontend is currently build to manage packages and files. <br/>" +
                  "<a href=\"/ui/#!dashboard\"><span>WebConsole</span></a>", ContentMode.HTML)
    );
    
    // space
    vl.addComponent(new Label("&nbsp;", ContentMode.HTML));
    
    // paragraph
    vl.addComponent(
        new Label("The well known openthinclient JavaWebStart management console goes here: <br/>" +
                  "<a href=\"/WebstartLink\"><span>Webstart Management</span></a>", ContentMode.HTML)
    );

    setContent(grid);
  }

}
