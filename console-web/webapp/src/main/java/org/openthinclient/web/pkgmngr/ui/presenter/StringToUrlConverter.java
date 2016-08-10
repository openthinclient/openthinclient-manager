package org.openthinclient.web.pkgmngr.ui.presenter;

import com.google.common.base.Strings;
import com.vaadin.data.util.converter.Converter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class StringToUrlConverter implements Converter<String, URL> {
  
  /** serialVersionUID */
  private static final long serialVersionUID = -7087450829844830031L;

  @Override
  public URL convertToModel(String value, Class<? extends URL> targetType, Locale locale) throws ConversionException {

    if (Strings.isNullOrEmpty(value))
      return null;

    try {
      return new URL(value);
    } catch (MalformedURLException e) {
      throw new ConversionException(e);
    }
  }

  @Override
  public String convertToPresentation(URL value, Class<? extends String> targetType, Locale locale) throws ConversionException {
    if (value == null)
      return new String();
    return value.toString();
  }

  @Override
  public Class<URL> getModelType() {
    return URL.class;
  }

  @Override
  public Class<String> getPresentationType() {
    return String.class;
  }
}
