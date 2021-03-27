package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import java.util.Base64;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.license.*;
import org.openthinclient.web.component.Popup;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;


import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = "license")
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_LICENSE_HEADER", order = 30)
public class LicenseView extends Panel implements View {

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private DownloadManager downloadManager;
  @Autowired
  private LicenseManager licenseManager;
  @Autowired
  private LicenseUpdater licenseUpdater;
  @Autowired
  private ClientService clientService;

  private final MessageConveyor mc;
  private final CssLayout root;

  private String serverID;

  private Box overviewBox;
  private Box actionBox;
  private Box errorBox;

  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(UI.getCurrent().getLocale());
  private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(UI.getCurrent().getLocale());

  public LicenseView() {

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();

    root = new CssLayout();
    root.setStyleName("licenseview");
    setContent(root);
  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_LICENSE_HEADER);
  }

  @PostConstruct
  private void init() {
    serverID = managerHome.getMetadata().getServerID();
    buildContent();
  }

  private void buildContent() {
    overviewBox = new OverviewBox();
    actionBox = new ActionBox();
    errorBox = new ErrorBox();

    root.addComponent(overviewBox);
    root.addComponent(actionBox);
    root.addComponent(errorBox);
  }

  private static ConsoleWebMessages licenseErrorMessage(LicenseError.ErrorType type) {
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

  abstract class Box extends CssLayout {
    CssLayout content = new CssLayout();
    Box(ConsoleWebMessages title_key, String styleName) {
      super();
      addStyleName("box");
      Component title = new Label(mc.getMessage(title_key));
      title.addStyleName("title");
      content.addStyleName("content");
      content.addStyleName(styleName);
      addComponents(title, content);
      update();
    }
    void update(){
      content.removeAllComponents();
      build();
    };
    abstract void build();
  }

  class OverviewBox extends Box {
    OverviewBox() {
      super(UI_SUPPORT_LICENSE_OVERVIEW_CAPTION, "overview");
    }
    void build() {
      int clientCount = clientService.count();
      License license = licenseManager.getLicense();
      if(license != null) {
        content.addComponents(
          new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_NAME)),
          new Label(license.getName()),
          new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_COUNT)),
          new Label(clientCount + " / " + license.getCount()),
          new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_EXPIRATION_DATE)),
          new Label(license.getSoftExpiredDate().format(dateFormatter))
        );
      } else {
        Label noLicense = new Label(mc.getMessage(UI_SUPPORT_LICENSE_NOT_INSTALLED));
        noLicense.addStyleName("nolicense");
        content.addComponent(noLicense);
      }
      content.addComponents(new Label("Server ID"), new Label(serverID));
      if(license != null) {
        content.addComponent(
          new Button(mc.getMessage(UI_SUPPORT_LICENSE_OVERVIEW_BUTTON), ev -> {
              (new DetailsPopup()).open();
          })
        );
      }
    }
  }

  class ActionBox extends Box {
    ActionBox() {
      super(UI_SUPPORT_LICENSE_ACTIONS_CAPTION, "actions");
    }
    void build() {
      content.addComponents(
        new Button(mc.getMessage(UI_SUPPORT_LICENSE_UPDATE_BUTTON), ev -> {
            licenseUpdater.updateLicense(serverID);
            overviewBox.update();
            errorBox.update();
        }),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_UPDATE_BUTTON_HINT)),
        new Button(mc.getMessage(UI_SUPPORT_LICENSE_ENTRY_BUTTON), ev ->  {
            (new ManualEntryPopup()).open();
        }),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_ENTRY_BUTTON_HINT)),
        new Button(mc.getMessage(UI_SUPPORT_LICENSE_DELETE_BUTTON), ev -> {
            (new DeletionPopup()).open();
        }),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_DELETE_BUTTON_HINT)),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_SHOP_LINK), ContentMode.HTML),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_SHOP_LINK_HINT))
      );
    }
  }

  class ErrorBox extends Box {
    ErrorBox() {
      super(UI_SUPPORT_LICENSE_ERRORS_CAPTION, "errors");
    }
    void build() {
      for(LicenseError error: licenseManager.getErrors()) {
        content.addComponents(
          new Label(error.datetime.format(dateTimeFormatter)),
          new Label(mc.getMessage(licenseErrorMessage(error.type)))
        );
      }
    }
  }

  class DetailsPopup extends Popup {
    DetailsPopup() {
      super(UI_SUPPORT_LICENSE_DETAILS_CAPTION, "license-details");
      setWidth("642px");
      int clientCount = clientService.count();
      License license = licenseManager.getLicense();
      addContent(
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_NAME)),
        new Label(license.getName()),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_EMAIL)),
        new Label(license.getEmail()),
        spacer(),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_COUNT)),
        new Label(clientCount + " / " + license.getCount()),
        new Label("Server ID"),
        new Label(serverID),
        spacer(),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_EXPIRATION_DATE)),
        new Label(license.getSoftExpiredDate().format(dateFormatter)),
        new Label(mc.getMessage(UI_SUPPORT_LICENSE_FIELD_CREATED_DATE)),
        new Label(license.getCreatedDate().format(dateFormatter))
      );
      Label details = new Label(license.getDetails());
      details.addStyleName("details");
      addContent(details);
    }
  }

  private Component spacer() {
    Component spacer = new Label();
    spacer.addStyleName("spacer");
    return spacer;
  }

  class ManualEntryPopup extends Popup {
    private TextArea manualEntry = new TextArea();
    private Label manualEntryFeedback = new Label();

    ManualEntryPopup() {
      super(UI_SUPPORT_LICENSE_MANUAL_ENTRY_CAPTION, "manual-license-entry");
      setWidth("642px");
      addContent(
        new Label(String.format(mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_TEXT), serverID), ContentMode.HTML),
        manualEntry,
        manualEntryFeedback,
        new Button(mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_BUTTON), ev -> {
          String feedback = updateLicense(manualEntry.getValue());
          manualEntryFeedback.setValue(feedback);
          overviewBox.update();
          errorBox.update();
        })
      );
    }
    public void open() {
      super.open();
      manualEntry.focus();
    }
  }

  class DeletionPopup extends Popup {
    DeletionPopup() {
      super(UI_SUPPORT_LICENSE_CONFIRM_DELETION_CAPTION);
      setWidth("642px");
      addContent(new Label(mc.getMessage(UI_SUPPORT_LICENSE_CONFIRM_DELETION_TEXT), ContentMode.HTML));
      addButton(
        new Button(mc.getMessage(UI_BUTTON_CANCEL), ev -> {
          close();
          overviewBox.update();
        }),
        new Button(mc.getMessage(UI_SUPPORT_LICENSE_CONFIRM_DELETION_BUTTON), ev -> {
          close();
          licenseManager.deleteLicense();
          overviewBox.update();
        })
      );
    }
  }

  private String updateLicense(String licenseString) {
    licenseString = licenseString.replaceAll("\n", "").trim();
    if(licenseString.length() == 0) {
      return "";
    }
    String[] licenseParts = licenseString.split("-");
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
    encryptedLicense.encryption_key = licenseParts[0];
    encryptedLicense.license = licenseParts[1];
    boolean success = licenseManager.setLicense(encryptedLicense);
    if(success) {
      return mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_SUCCESS);
    } else {
      return mc.getMessage(UI_SUPPORT_LICENSE_MANUAL_ENTRY_ERROR);
    }
  }
}
