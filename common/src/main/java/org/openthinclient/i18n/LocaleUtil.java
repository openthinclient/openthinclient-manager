package org.openthinclient.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import ch.qos.cal10n.util.AnnotationExtractorViaEnumClass;

/**
 * Helper class for CAL10N related message handling
 */
public class LocaleUtil {

  /**
   * Returns the current {@link Locale} if exists in '@LocaleData'-annotation of {@link Class} enumClazz,
   * otherwise it returns {@link Locale.ENGLISH}
   * 
   * @param enumClazz the Enum-Class-type, must contain @LocaleData-annotation of CAL10N
   * @param current Locale
   * @return a Locale, defaults to Locale.ENGLISH
   */
  public static Locale getLocaleForMessages(Class<?> enumClazz, Locale current) {
    
    if (enumClazz != null && current != null) {
      AnnotationExtractorViaEnumClass annotationExtractorViaEnumClass = new AnnotationExtractorViaEnumClass(enumClazz);
      ch.qos.cal10n.Locale[] extractLocales = annotationExtractorViaEnumClass.extractLocales();
      List<ch.qos.cal10n.Locale> list = Arrays.asList(extractLocales);
      for (ch.qos.cal10n.Locale cal10nLocale : list) {
          if (cal10nLocale.value().equals(current.getLanguage())) {
            return current;
          }
      }
    }
    
    return Locale.ENGLISH;
  }
  
}
