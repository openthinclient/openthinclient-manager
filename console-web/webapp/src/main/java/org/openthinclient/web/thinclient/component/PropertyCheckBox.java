package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;

/**
 * Currently unused, because of ugly schema format
 */
public class PropertyCheckBox<T extends OtcBooleanProperty> extends CheckBox implements PropertyComponent {

  Binder<T> binder;
  private T bean;

  public PropertyCheckBox(T bean) {

    setStyleName("profileItemCheckbox");

    this.bean = bean;

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this).bind(T::isValue, T::setValue);
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
