package org.openthinclient.web.thinclient;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;

/**
 * Created by Jörg Neumann (jne@mms-dresden.de) on 07.05.2018.
 */
public class BooleanPropertyPanel<T extends OtcBooleanProperty> extends CheckBox implements
    PropertyComponent {

  Binder<T> binder;

  public BooleanPropertyPanel(T bean) {

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this).bind(T::isValue, T::setValue);
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
