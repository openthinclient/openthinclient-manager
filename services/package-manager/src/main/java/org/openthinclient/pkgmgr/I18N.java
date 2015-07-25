package org.openthinclient.pkgmgr;

import java.util.ResourceBundle;

public class I18N {

  private static final ResourceBundle resourceBundle;

  static {

    resourceBundle = ResourceBundle.getBundle("org/openthinclient/pkgmgr/messages");

  }

  public static String getMessage(String key) {

    try {
      return resourceBundle.getString(key);
    } catch (Exception e) {
      return "### " + key + " ### no translation found ###";
    }

  }

}
