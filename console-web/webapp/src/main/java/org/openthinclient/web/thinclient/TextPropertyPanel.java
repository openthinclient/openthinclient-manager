package org.openthinclient.web.thinclient;

import com.vaadin.data.Binder;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;

/**
 *
 */
public class TextPropertyPanel<T extends OtcTextProperty> extends TextField implements PropertyComponent {

  private final String propertyName;
  private Binder<T> binder;

  public TextPropertyPanel(String propertyName, T bean) {

//    super(propertyName);
    this.propertyName = propertyName;

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this)
          .withValidator(new StringLengthValidator("Min!", 1, 255))
          .bind(T::getValue, T::setValue);

  }

  public Binder getBinder() {
    return binder;
  }

  @Override
  public String getLabel() {
    return propertyName;
  }

  @Override
  public Component getComponent() {
    return this;
  }

}
