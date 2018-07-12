package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.NativeSelect;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;

/**
 *
 */
public class PropertySelect<T extends OtcOptionProperty> extends ComboBox<SelectOption> implements
    PropertyComponent {

  private Binder<T> binder;

  public PropertySelect(T bean) {

    super(null, bean.getOptions());
    setEmptySelectionAllowed(false);
    setStyleName("profileItemSelect");
    setItemCaptionGenerator(SelectOption::getLabel);
    setTextInputAllowed(false);

    binder = new Binder<>();
    binder.setBean(bean);
    binder.forField(this)
          .bind(t -> bean.getSelectOption(t.getValue()), (t, selectOption) -> t.setValue(selectOption.getValue()));
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
