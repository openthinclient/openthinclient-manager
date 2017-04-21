package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.springframework.beans.factory.annotation.Value;

import static org.openthinclient.web.WebUtil.getServletMappingRoot;

@Theme("dashboard")
@Title("openthinclient.org")
@SpringUI(path = "/welcome")
public final class WelcomeUI extends UI {

  /** serialVersionUID */
  private static final long serialVersionUID = -5094140681208354084L;
  
  @Value("${vaadin.servlet.urlMapping}")
  private String vaadinServletUrlMapping;
  

  @Override
  protected void init(final VaadinRequest request) {

    setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale())); 
    
    Responsive.makeResponsive(this);
    addStyleName(ValoTheme.UI_WITH_MENU);

    updateContent();
  }

  /**
   * Setup the content
   */
  private void updateContent() {
    
    final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
     
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
    vl.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_WELCOMEUI_WELCOME)));
    vl.addComponent(new Label("&nbsp;", ContentMode.HTML));

    // paragraph
    vl.addComponent(
        new Label(mc.getMessage(ConsoleWebMessages.UI_WELCOMEUI_WEBCONSOLE_DESCRIPTION, getServletMappingRoot(vaadinServletUrlMapping)), ContentMode.HTML)
    );
    
    // space
    vl.addComponent(new Label("&nbsp;", ContentMode.HTML));
    
    // paragraph
    vl.addComponent(
        new Label(mc.getMessage(ConsoleWebMessages.UI_WELCOMEUI_JAVAWEBSTART_DESCRIPTION), ContentMode.HTML)
    );

    setContent(grid);
  }

}
