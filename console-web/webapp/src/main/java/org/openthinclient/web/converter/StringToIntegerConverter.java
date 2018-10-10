package org.openthinclient.web.converter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * StringToIntegerConverter converts String to Integer
 */
public class StringToIntegerConverter extends com.vaadin.data.converter.StringToIntegerConverter{

  private static final long serialVersionUID = -6464686484330572080L;

  public StringToIntegerConverter(String message) {
    super(null, message);
  }

  @Override
  protected NumberFormat getFormat(Locale locale) {
    // do not use a thousands separator, as HTML5 input type
    // number expects a fixed wire/DOM number format regardless
    // of how the browser presents it to the user (which could
    // depend on the browser locale)
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(0);
    format.setDecimalSeparatorAlwaysShown(false);
    format.setParseIntegerOnly(true);
    format.setGroupingUsed(false);
    return format;
  }

}
