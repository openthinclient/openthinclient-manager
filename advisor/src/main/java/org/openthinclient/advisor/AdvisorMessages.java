package org.openthinclient.advisor;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

@BaseName("i18n/advisor-messages")
@LocaleData(  defaultCharset="UTF8",
              value = { @Locale("de"), @Locale("en") }
)
public enum AdvisorMessages  {
  
  ADVISOR_CHECKFILESYSTEMFREESPACE_FREESPACE_MINIMUM,
  ADVISOR_CHECKFILESYSTEMFREESPACE_SKIPED,
  ADVISOR_CHECKFILESYSTEMFREESPACE_DESCRIPTION,
  
  ADVISOR_CHECKNETWORKINFERFACES_TITLE,
  
  ADVISOR_CHECKINTERNETCONNECTION_TITLE,
  
  ADVISOR_CHECKMANAGERHOMEDIRECTORY_TITLE,
  ADVISOR_CHECKMANAGERHOMEDIRECTORY_DESCRIPTION
  ;
}