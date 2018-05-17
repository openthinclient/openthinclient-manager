package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.NativeSelect;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;

/**
 *
 */
public class PropertySelect<T extends OtcOptionProperty> extends NativeSelect<String> implements
    PropertyComponent {

  private Binder<T> binder;

  public PropertySelect(T bean) {

    super(null, bean.getOptions());
    setEmptySelectionAllowed(false);

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this)
          .bind(T::getValue, T::setValue);
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
