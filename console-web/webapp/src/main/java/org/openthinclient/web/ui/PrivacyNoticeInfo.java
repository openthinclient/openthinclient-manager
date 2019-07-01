package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class PrivacyNoticeInfo extends CssLayout {
  Label infoText;
  Button ackButton;
  Button showButton;
  IMessageConveyor mc;
  PrivacyNotice privacyNotice;
  Window popupWindow;

  public PrivacyNoticeInfo() {
    privacyNotice = PrivacyNotice.load();
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    infoText = new Label(mc.getMessage(UI_DASHBOARDVIEW_PRIVACY_NOTICE_INFO));
    ackButton = new Button(VaadinIcons.CLOSE_CIRCLE_O, ev -> this.collapse());
    showButton = new Button(mc.getMessage(UI_DASHBOARDVIEW_PRIVACY_NOTICE_CAPTION),
                            ev -> this.popup());
    showButton.setStyleName("link");
    popupWindow = buildPopupWindow();

    addStyleName("privacy-notice-info");

    if(privacyNotice.isAcknowledged()) {
      collapse();
    } else {
      expand();
    }
  };

  private Window buildPopupWindow() {
    String lang = UI.getCurrent().getLocale().getLanguage();
    Component text = new Label(privacyNotice.get(lang), ContentMode.HTML);
    Button closeButton = new Button("OK");
    AbstractOrderedLayout layout = new VerticalLayout(text, closeButton);
    layout.setSpacing(false);
    layout.addStyleName("privacy-notice-content");
    Window win = new Window(mc.getMessage(UI_DASHBOARDVIEW_PRIVACY_NOTICE_CAPTION), layout);
    win.setResizable(false);
    win.addCloseListener(event -> { UI.getCurrent().removeWindow(popupWindow); });
    closeButton.addClickListener(ev -> win.close());
    return win;
  }

  public void expand() {
    addStyleName("expanded");
    removeAllComponents();
    addComponents(new HorizontalLayout(infoText, showButton), ackButton);
  }

  public void collapse() {
    privacyNotice.setAcknowledged();
    removeStyleName("expanded");
    removeAllComponents();
    addComponents(showButton);
  }



  public void popup() {
    UI.getCurrent().removeWindow(popupWindow);
    UI.getCurrent().addWindow(popupWindow);
    popupWindow.center();
    collapse();
  }
}
