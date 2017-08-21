package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewHeader extends VerticalLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ViewHeader.class);

  public static final String TITLE_ID = "dashboard-title";
  private final HorizontalLayout tools;
  private final Label titleLabel;

  public ViewHeader(boolean showSparklines) {

    setMargin(false);

    final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    HorizontalLayout head = new HorizontalLayout();
    head.setMargin(false);
    head.setSpacing(false);
    head.addStyleName("viewheader");

    titleLabel = new Label();
    titleLabel.setId(TITLE_ID);
    titleLabel.setSizeUndefined();
    titleLabel.addStyleName(ValoTheme.LABEL_H1);
    titleLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
    head.addComponent(titleLabel);

    this.tools = new HorizontalLayout();
    this.tools.setSpacing(true);
    this.tools.addStyleName("toolbar");
    head.addComponent(this.tools);

    addComponent(head);

    if (showSparklines) {
      addComponent(new Sparklines());
    }

    // Configure the error handler for the UI
    UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
      @Override
      public void error(com.vaadin.server.ErrorEvent event) {
        LOGGER.error("Caught unexpected error.", event.getThrowable());
        // Display the error message in a custom fashion
        Label errorMessage = new Label(mc.getMessage(ConsoleWebMessages.UI_UNEXPECTED_ERROR), ContentMode.HTML);
        errorMessage.addStyleName("unexpected_error");
        addComponent(errorMessage);
      }
    });
  }

  public ViewHeader(String title) {
    this(true);
    setTitle(title);
  }

  public ViewHeader(String title, boolean showSparklines) {
    this(showSparklines);
    setTitle(title);
  }

  public void addTool(Component component) {
    tools.addComponent(component);
  }
  public void addTools(Component... components) {
    tools.addComponents(components);
  }

  public void removeTool(Component component) {
    tools.removeComponent(component);
  }

  public String getTitle() {
    return titleLabel.getValue();
  }

  public void setTitle(String title) {
    titleLabel.setValue(title);
  }
}
