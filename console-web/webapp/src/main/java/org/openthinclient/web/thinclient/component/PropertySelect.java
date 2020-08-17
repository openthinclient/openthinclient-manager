package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.UI;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_REQUIRED_VALUE_PLACEHOLDER;

/**
 * ComboBox to deal with OtcOptionProperty
 */
public class PropertySelect<T extends OtcOptionProperty> extends ComboBox<SelectOption> implements PropertyComponent {

  private Binder<T> binder;

  public PropertySelect(T bean) {

    super(null, bean.getOptions());
    setStyleName("profileItemSelect");
    setItemCaptionGenerator(SelectOption::getLabel);
    setStyleGenerator(item -> "value");
    setTextInputAllowed(bean.getOptions().size() > 10);
    setEnabled(!bean.getConfiguration().isDisabled());
    setEmptySelectionAllowed(!bean.getConfiguration().isRequired());
    if (bean.getDefaultSchemaValue() != null) {
      SelectOption selectOption = bean.getSelectOption(bean.getDefaultSchemaValue());
      if (selectOption != null) {
        setPlaceholder(selectOption.getLabel());
      }
    }

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    binder = new Binder<>();
    binder.setBean(bean);
    Binder.BindingBuilder<T, SelectOption> field = binder.forField(this);
    if (bean.getConfiguration().isRequired()) {
      field.asRequired(mc.getMessage(UI_COMMON_REQUIRED_VALUE_PLACEHOLDER));
    }
    field.bind(t -> bean.getSelectOption(t.getValue()), (t, selectOption) -> t.setValue(selectOption != null ? selectOption.getValue() : null));

    // preselect if only one option is present
    if (bean.getOptions().size() == 1) {
      setValue(bean.getSelectOption(bean.getOptions().get(0).getValue()));
    }
  }

  @Override
  public Binder<T> getBinder() {
    return binder;
  }

}
