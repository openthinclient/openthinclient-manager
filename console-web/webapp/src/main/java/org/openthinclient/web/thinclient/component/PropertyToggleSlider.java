package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.Slider;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;

/**
 * Currently testing, because of ugly schema format
 */
public class PropertyToggleSlider<T extends OtcBooleanProperty> extends Slider implements PropertyComponent {

  Binder<T> binder;
  T bean;

  public PropertyToggleSlider(T bean) {

    setWidth("75px");
    setResolution(0);
    setMin(0);
    setMax(1);

    setStyleName("profileItemCheckbox");

    this.bean = bean;

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this)
            .bind(t -> bean.isValue() ? 1d : 0, (t, aDouble) -> t.setValue(aDouble == 1));
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
