package org.openthinclient.web.thinclient.component;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.NativeSelect;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;

/**
 * ComboBox to deal with OtcOptionProperty
 */
public class PropertySelect<T extends OtcOptionProperty> extends ComboBox<SelectOption> implements PropertyComponent {

  private Binder<T> binder;
  private T bean;

  public PropertySelect(T bean) {

    super(null, bean.getOptions());
    setEmptySelectionAllowed(false);
    setStyleName("profileItemSelect");
    setItemCaptionGenerator(SelectOption::getLabel);
    setTextInputAllowed(false);
    setEnabled(!bean.getConfiguration().isDisabled());

    this.bean = bean;

    binder = new Binder<>();
    binder.setBean(bean);
    Binder.BindingBuilder<T, SelectOption> field = binder.forField(this);
    if (bean.getConfiguration().isRequired()) {
      field.asRequired("Please select a value");
    }
    field.bind(t -> bean.getSelectOption(t.getValue()), (t, selectOption) -> t.setValue(selectOption != null ? selectOption.getValue() : null));

    // preselect if only one option is present
    if (bean.getOptions().size() == 1) {
      setValue(bean.getSelectOption(bean.getOptions().get(0).getValue()));
    }
  }

  @Override
  public Binder getBinder() {
    return binder;
  }

}
