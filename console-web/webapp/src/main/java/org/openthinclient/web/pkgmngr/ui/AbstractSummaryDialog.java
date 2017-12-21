package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.server.Sizeable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public abstract class AbstractSummaryDialog {
  protected final Window window;
  protected final MHorizontalLayout footer;
  protected final MButton cancelButton;
  protected final MButton proceedButton;
  protected final IMessageConveyor mc;
  protected volatile boolean initialized;
  private MVerticalLayout content;

  public AbstractSummaryDialog() {
    window = new Window();

    window.setWidth(60, Sizeable.Unit.PERCENTAGE);
    window.setHeight(null);
    window.center();
    this.mc = new MessageConveyor(UI.getCurrent().getLocale());

    proceedButton = new MButton(mc.getMessage(ConsoleWebMessages.UI_BUTTON_YES)).withStyleName(ValoTheme.BUTTON_PRIMARY).withListener((Button.ClickListener) e -> onProceed());
    cancelButton = new MButton(mc.getMessage(ConsoleWebMessages.UI_BUTTON_CANCEL)).withListener((Button.ClickListener) e -> onCancel());
    footer = new MHorizontalLayout()
            .withFullWidth()
            .withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR)
            .with(proceedButton, cancelButton);
    footer.setComponentAlignment(proceedButton, Alignment.TOP_RIGHT);
    footer.setExpandRatio(proceedButton, 1);

  }

  protected abstract void onCancel();

  protected abstract void createContent(MVerticalLayout content);

  protected void initialize() {
    this.content = new MVerticalLayout()
            .withMargin(true)
            .withSpacing(true);
    createContent(this.content);

    content.addComponent(footer);

    window.setContent(content);
  }

  @SuppressWarnings("unchecked")
  public abstract void update();

  public void open(boolean modal) {
    window.setModal(modal);
    if (!isOpen()) {

      if (!initialized) {
        synchronized (this) {
          if (!initialized) {
            initialize();
            initialized = true;
          }
        }
      }

      update();

      UI.getCurrent().addWindow(window);
    }
  }

  public boolean isOpen() {
    return UI.getCurrent().getWindows().contains(window);
  }

  public void close() {
    UI.getCurrent().removeWindow(window);
  }

  protected abstract void onProceed();
}
