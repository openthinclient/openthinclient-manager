package org.openthinclient.web.i18n;

import ch.qos.cal10n.LocaleData;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.BaseName;

@BaseName("i18n/console-web-messages")
@LocaleData(defaultCharset = "UTF8", value = { @Locale("de") })
public enum ConsoleWebMessages {

   UI_LOGIN_WELCOME,
   UI_LOGIN_USERNAME, 
   UI_LOGIN_PASSWORD, 
   UI_LOGIN_LOGIN,
   UI_LOGIN_REMEMBERME,
   UI_LOGIN_NOTIFICATION_TITLE,
   UI_LOGIN_NOTIFICATION_DESCRIPTION,
   UI_LOGIN_NOTIFICATION_REMEMBERME_TITLE,
   UI_LOGIN_NOTIFICATION_REMEMBERME_DESCRIPTION,
   
   

   ;
}
