package org.openthinclient.web.component;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_BUTTON_CLOSE;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import org.openthinclient.web.i18n.ConsoleWebMessages;

import ch.qos.cal10n.MessageConveyor;

public class Popup {
  private Window popup;
  private CssLayout contentLayout = new CssLayout();
  private CssLayout buttonsLayout = new CssLayout();
  private String width;
  private WindowMode windowMode = WindowMode.NORMAL;

  static MessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

  public Popup(ConsoleWebMessages title_key, String... styleNames) {
    this(mc.getMessage(title_key), styleNames);
  }
  public Popup(String title, String... styleNames) {
    contentLayout.addStyleName("content");
    contentLayout.addStyleNames(styleNames);
    buttonsLayout.addStyleName("buttons");
    CssLayout wrapper = new CssLayout(contentLayout, buttonsLayout);
    wrapper.addStyleName("wrapper");
    popup = new Window(title, wrapper);
    popup.addStyleName("otc-popup");
    popup.setModal(true);
    popup.addCloseShortcut(KeyCode.ESCAPE);
    popup.addCloseListener(ev -> UI.getCurrent().removeWindow(popup));
    popup.addWindowModeChangeListener(ev -> {
      if(ev.getWindowMode() == WindowMode.MAXIMIZED) {
        popup.addStyleName("maximized");
      } else {
        popup.removeStyleName("maximized");
      }
    });
  }
  public void open() {
    if(buttonsLayout.getComponentCount() == 0) {
      buttonsLayout.addComponent(new Button(mc.getMessage(UI_BUTTON_CLOSE), ev -> popup.close()));
    }
    popup.setWindowMode(windowMode);
    popup.setSizeUndefined();
    popup.setWidth(width);
    popup.center();
    UI.getCurrent().addWindow(popup);
  }
  public void close() {
    popup.close();
  }
  public void addContent(Component... content) {
    this.contentLayout.addComponents(content);
  }
  public void addButton(Button... buttons) {
    this.buttonsLayout.addComponents(buttons);
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public void setMaximized(boolean maximized) {
    this.windowMode = maximized ? WindowMode.MAXIMIZED : WindowMode.NORMAL;
  }
}
