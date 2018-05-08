package org.openthinclient.web.thinclient;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;

/**
 * Created by Jörg Neumann (jne@mms-dresden.de) on 07.05.2018.
 */
public class BooleanPropertyPanel<T extends OtcBooleanProperty> extends CheckBox implements
    PropertyComponent {

  Binder<T> binder;
  String propertyName;

  public BooleanPropertyPanel(String propertyName, T bean) {

//    super(propertyName);
    this.propertyName = propertyName;

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this).bind(T::isValue, T::setValue);
  }

  @Override
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
