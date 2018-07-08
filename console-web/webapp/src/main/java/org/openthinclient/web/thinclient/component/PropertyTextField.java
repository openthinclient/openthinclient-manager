package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextField;
import org.openthinclient.web.thinclient.property.OtcTextProperty;

/**
 *
 */
public class PropertyTextField<T extends OtcTextProperty> extends TextField implements
    PropertyComponent {

  private Binder<T> binder;

  public PropertyTextField(T bean) {

    setStyleName("profileItemTextfield");

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this)
          .withNullRepresentation("")
//          .withValidator(new StringLengthValidator("Min!", 1, 255))
          .bind(T::getValue, T::setValue);
  }

  public Binder getBinder() {
    return binder;
  }

}
