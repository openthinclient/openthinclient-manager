package org.openthinclient.flow.packagemanager.presenter;

import com.google.common.base.Strings;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

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
