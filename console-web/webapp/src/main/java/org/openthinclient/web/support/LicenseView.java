package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import java.util.Base64;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.license.*;
import org.openthinclient.service.common.license.LicenseData.State.*;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ViewHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;


import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = "license")
public class LicenseView extends Panel implements View {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenseView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private DownloadManager downloadManager;
  @Autowired
  private LicenseManager licenseManager;
  @Autowired
  private LicenseUpdater licenseUpdater;

  final MessageConveyor mc;
  final VerticalLayout root;


  private TextArea manualEntry;
  private Label manualEntryFeedback;
  private CssLayout licenseBox;
  private CssLayout errorBox;

  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(UI.getCurrent().getLocale());
  private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(UI.getCurrent().getLocale());

  public LicenseView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();
    setStyleName("licenseview");
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_SUPPORT_LICENSE_HEADER)));

    root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
    setContent(root);
    Responsive.makeResponsive(root);

  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_LICENSE_HEADER);
  }

  @PostConstruct
  private void init() {
      buildContent();
  }

  private void buildContent() {
    VerticalLayout content = new VerticalLayout();

    licenseBox = new CssLayout();
    licenseBox.addStyleName("license");
    updateLicenseBox();
    content.addComponent(licenseBox);

    content.addComponent(new Button(mc.getMessage(UI_SUPPORT_LICENSE_UPDATE_BUTTON), this::licenseUpdate));
    content.addComponent(new Button(mc.getMessage(UI_SUPPORT_LICENSE_DELETE_BUTTON), this::licenseDeletion));

    CssLayout manualEntryBox = new CssLayout();
    manualEntryBox.addStyleName("manualEntry");
    manualEntry = new TextArea();
    manualEntry.setPlaceholder(mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_HINT));
    manualEntryBox.addComponent(manualEntry);
    manualEntryBox.addComponent(new Button(mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_BUTTON), this::manualLicenseUpdate));
    manualEntryFeedback = new Label();
    manualEntryBox.addComponent(manualEntryFeedback);
    content.addComponent(manualEntryBox);

    errorBox = new CssLayout();
    errorBox.addStyleName("errors");
    updateErrorBox();
    content.addComponent(errorBox);

    root.addComponent(content);
    root.setExpandRatio(content, 1);
  }

  void updateLicenseBox() {
    licenseBox.removeAllComponents();
    LicenseData license = licenseManager.getLicense();
    if(license != null) {
      licenseBox.addComponent(new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_NAME)));
      licenseBox.addComponent(new Label(license.getName()));
      licenseBox.addComponent(new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_COUNT)));
      licenseBox.addComponent(new Label(license.getCount().toString()));
      licenseBox.addComponent(new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_EXPIRATION_DATE)));
      licenseBox.addComponent(new Label(license.getSoftExpiredDate().format(dateFormatter)));
    } else {
      Label noLicense = new Label(mc.getMessage(UI_SUPPORT_LICENSE_NOT_INSTALLED));
      noLicense.addStyleName("nolicense");
      licenseBox.addComponent(noLicense);
    }
    licenseBox.addComponent(new Label("Server ID"));
    licenseBox.addComponent(new Label(managerHome.getMetadata().getServerID()));
  }

  void updateErrorBox() {
    errorBox.removeAllComponents();
    for(LicenseError error: licenseManager.getErrors()) {
      errorBox.addComponent(new Label(error.datetime.format(dateTimeFormatter)));
      errorBox.addComponent(new Label(mc.getMessage(licenseErrorMessage(error.type))));
    }
  }

  static ConsoleWebMessages licenseErrorMessage(LicenseError.ErrorType type) {
    switch(type) {
      case UPDATED:          return UI_SUPPORT_LICENSE_UPDATED;
      case NO_LICENSE:       return UI_SUPPORT_LICENSE_NO_LICENSE;
      case DECRYPTION_ERROR: return UI_SUPPORT_LICENSE_DECRYPTION_ERROR;
      case SERVER_ID_ERROR:  return UI_SUPPORT_LICENSE_SERVER_ID_ERROR;
      case NETWORK_ERROR:    return UI_SUPPORT_LICENSE_NETWORK_ERROR;
      case SERVER_ERROR:     return UI_SUPPORT_LICENSE_SERVER_ERROR;
    }
    return null;
  }

  public void licenseDeletion(Button.ClickEvent event) {
    Window popup = new Window(mc.getMessage(UI_SUPPORT_LICENSE_CONFIRM_DELETION_CAPTION));
    CssLayout layout = new CssLayout();
    layout.addComponent(new Label(mc.getMessage(UI_SUPPORT_LICENSE_CONFIRM_DELETION_TEXT)));
    layout.addComponent(new Button(mc.getMessage(UI_BUTTON_YES), ev -> {
        popup.close();
        licenseManager.deleteLicense();
        updateLicenseBox();
    }));
    layout.addComponent(new Button(mc.getMessage(UI_BUTTON_CANCEL),ev -> {
        popup.close();
        updateLicenseBox();
    }));
    layout.addStyleName("popupLicenseDeletion");
    popup.setContent(layout);
    popup.setHeight("140px");
    popup.setWidth("500px");
    popup.center();
    popup.setModal(true);
    popup.addCloseShortcut(KeyCode.ESCAPE);
    popup.addCloseListener(ev -> {
       UI.getCurrent().removeWindow(popup);
    });
    UI.getCurrent().addWindow(popup);
  }

  public void licenseUpdate(Button.ClickEvent event) {
    licenseUpdater.updateLicense(managerHome.getMetadata().getServerID());
    updateLicenseBox();
    updateErrorBox();
  }

  public void manualLicenseUpdate(Button.ClickEvent event) {
    String feedback = updateLicense(manualEntry.getValue());
    manualEntryFeedback.setValue(feedback);
    updateLicenseBox();
    updateErrorBox();
  }

  public String updateLicense(String licenseString) {
    licenseString = licenseString.replaceAll("\n", "").trim();
    if(licenseString.length() == 0) {
      return "";
    }
    String[] licenseParts = licenseString.split(" ");
    if(licenseParts.length != 2) {
      return mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_INVALID);
    }
    Base64.Decoder base64Decoder = Base64.getDecoder();
    try {
      base64Decoder.decode(licenseParts[0]);
      base64Decoder.decode(licenseParts[1]);
    } catch(IllegalArgumentException ex) {
      return mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_INVALID);
    }
    EncryptedLicense encryptedLicense = new EncryptedLicense();
    encryptedLicense.license = licenseParts[0];
    encryptedLicense.encryption_key = licenseParts[1];
    boolean success = licenseManager.setLicense(encryptedLicense);
    if(success) {
      return mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_SUCCESS);
    } else {
      return mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_ERROR);
    }
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }

}
