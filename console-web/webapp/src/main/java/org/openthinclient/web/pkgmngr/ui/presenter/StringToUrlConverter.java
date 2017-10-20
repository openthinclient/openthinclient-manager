package org.openthinclient.web.pkgmngr.ui.presenter;

import com.google.common.base.Strings;
import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;

import java.net.MalformedURLException;
import java.net.URL;

public class StringToUrlConverter implements Converter<String, URL> {
  
  /** serialVersionUID */
  private static final long serialVersionUID = -7087450829844830031L;

  @Override
  public Result<URL> convertToModel(String value, ValueContext context) {
    if (Strings.isNullOrEmpty(value)) {
      return null;
    }
    try {
      return Result.ok(new URL(value));
    } catch (MalformedURLException e) {
      return Result.error(e.getMessage());
    }
  }

  @Override
  public String convertToPresentation(URL value, ValueContext context) {
    if (value == null) {
      return new String();
    }
    return value.toString();
  }

}
