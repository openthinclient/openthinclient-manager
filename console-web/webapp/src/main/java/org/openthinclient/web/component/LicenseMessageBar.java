package org.openthinclient.web.component;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.service.common.license.License;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.web.i18n.ConsoleWebMessages;

import java.util.EnumMap;

import static org.openthinclient.service.common.license.License.State.*;
import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class LicenseMessageBar extends Label {

  private LicenseManager licenseManager;
  private ClientService clientService;

  private MessageConveyor mc;
  private EnumMap<License.State, String> licenseStateMessage;

  public LicenseMessageBar(LicenseManager licenseManager, ClientService clientService) {
    super("", ContentMode.HTML);
    this.licenseManager = licenseManager;
    this.clientService = clientService;
    mc = new MessageConveyor(UI.getCurrent().getLocale());
    initLicenseStateMessages();
    updateContent();
  }

  private void initLicenseStateMessages() {
    licenseStateMessage = new EnumMap<>(License.State.class);
    licenseStateMessage.put(REQUIRED_TOO_OLD, buildMessageHTML(UI_SUPPORT_LICENSE_STATE_REQUIRED_TOO_OLD));
    licenseStateMessage.put(REQUIRED_OLD,     buildMessageHTML(UI_SUPPORT_LICENSE_STATE_OLD));
    licenseStateMessage.put(REQUIRED_EXPIRED, buildMessageHTML(UI_SUPPORT_LICENSE_STATE_REQUIRED_EXPIRED));
    licenseStateMessage.put(SOFT_EXPIRED,     buildMessageHTML(UI_SUPPORT_LICENSE_STATE_SOFT_EXPIRED));
    licenseStateMessage.put(TOO_MANY,         buildMessageHTML(UI_SUPPORT_LICENSE_STATE_TOO_MANY));
    licenseStateMessage.put(INVALID,          buildMessageHTML(UI_SUPPORT_LICENSE_STATE_INVALID));
    licenseStateMessage.put(REQUIRED_MISSING, buildMessageHTML(UI_SUPPORT_LICENSE_STATE_REQUIRED_MISSING));
    licenseStateMessage.put(TOO_OLD,          buildMessageHTML(UI_SUPPORT_LICENSE_STATE_TOO_OLD));
    licenseStateMessage.put(OLD,              buildMessageHTML(UI_SUPPORT_LICENSE_STATE_OLD));
    licenseStateMessage.put(EXPIRED,          buildMessageHTML(UI_SUPPORT_LICENSE_STATE_EXPIRED));
    licenseStateMessage.put(COMMUNITY,        buildMessageHTML(UI_SUPPORT_LICENSE_STATE_COMMUNITY));
  }

  private String buildMessageHTML(ConsoleWebMessages... keys) {
    StringBuilder sb = new StringBuilder();
    for(ConsoleWebMessages key: keys) {
      sb.append("<p>");
      sb.append(mc.getMessage(key).replace("\n", "</p><p>"));
      sb.append("</p>");
    }
    return sb.toString();
  }

  public void updateContent() {
    License.State licenseState = licenseManager.getLicenseState(clientService.count());
    if(licenseState == OK) {
      this.setVisible(false);
    } else {
      this.setVisible(true);

      setValue(licenseStateMessage.get(licenseState));

      this.removeStyleNames("community", "warning", "error");
      addStyleName("license-messagebar");
      switch(licenseState) {
        case REQUIRED_TOO_OLD:
        case REQUIRED_EXPIRED:
        case TOO_MANY:
        case INVALID:
        case REQUIRED_MISSING:
          this.addStyleName("error");
          break;
        case COMMUNITY:
          this.addStyleName("community");
          break;
        default:
          this.addStyleName("warning");
      }
    }
  }
}
