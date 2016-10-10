package org.openthinclient.i18n;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

@BaseName("i18n/test-messages")
@LocaleData(  defaultCharset="UTF8",
              value = { @Locale("de"), @Locale("en") }
)
public enum Messages {
  FOO,
  BAR;
}
