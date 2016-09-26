package org.openthinclient.wizard;
import ch.qos.cal10n.LocaleData;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.BaseName;

@BaseName("i18n/messages")
@LocaleData(  defaultCharset="UTF8",
              value = { @Locale("en") }
)
public enum FirstStartWizardMessages  {
  UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_TITLE
  ;
}