package org.openthinclient.web.thinclient;

import com.vaadin.data.Binder;
import com.vaadin.ui.NativeSelect;

/**
 *
 */
public class OptionPropertyPanel<T extends OtcOptionProperty> extends NativeSelect<String> implements PropertyComponent {

  private Binder<T> binder;

  public OptionPropertyPanel(T bean) {

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
