package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;

/**
 * PropertyCheckBox for OtcBooleanProperty
 */
public class PropertyCheckBox<T extends OtcBooleanProperty> extends CheckBox implements PropertyComponent {

  Binder<T> binder;

  public PropertyCheckBox(T bean) {

    setStyleName("profileItemCheckbox");
    setReadOnly(bean.getConfiguration().isDisabled());

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this).bind(T::isValue, T::setValue);

    setDescription(bean.getLabelFor(bean.isValue()));

    addValueChangeListener(ev -> setDescription(bean.getLabelFor(ev.getValue())));
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
