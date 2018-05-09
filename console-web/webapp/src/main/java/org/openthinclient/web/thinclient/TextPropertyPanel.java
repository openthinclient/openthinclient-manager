package org.openthinclient.web.thinclient;

import com.vaadin.data.Binder;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.TextField;

/**
 *
 */
public class TextPropertyPanel<T extends OtcTextProperty> extends TextField implements PropertyComponent {

  private Binder<T> binder;

  public TextPropertyPanel(T bean) {

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this)
          .withValidator(new StringLengthValidator("Min!", 1, 255))
          .bind(T::getValue, T::setValue);
  }

  public Binder getBinder() {
    return binder;
  }

}
