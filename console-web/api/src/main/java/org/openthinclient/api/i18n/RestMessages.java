package org.openthinclient.api.i18n;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

@BaseName("i18n/rest-messages")
@LocaleData(defaultCharset = "UTF8", value = {@Locale("de"), @Locale("en")})
public enum RestMessages {

  REST_LICENSE_THINCLIENT_COMMUNICATION_ERROR,
  REST_LICENSE_THINCLIENT_LICENSE_EXPIRED,
  REST_LICENSE_THINCLIENT_TOO_MANY,
  REST_LICENSE_THINCLIENT_CRITICAL_ERROR,
  REST_LICENSE_THINCLIENT_BUY_LICENSE,

  ;

}
