package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import java.util.Optional;
import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.UI;
import org.openthinclient.web.thinclient.property.OtcMacProperty;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_REQUIRED_VALUE_PLACEHOLDER;

public class PropertyMacSelect<T extends OtcMacProperty> extends ComboBox<String> implements PropertyComponent {

  private Binder<T> binder;

  public PropertyMacSelect(T bean) {

    super(null, bean.getOptions());
    addStyleNames("profileItemSelect", "macselect");
    setTextInputAllowed(true);
    setEnabled(!bean.getConfiguration().isDisabled());
    setPlaceholder(bean.getInitialValue());
    setEmptySelectionAllowed(bean.getInitialValue() != null);
    setEmptySelectionCaption(bean.getInitialValue());

    setNewItemProvider(item -> {
        if(item == null) {
          return Optional.empty();
        }
        item = item.trim();
        if(item.matches("[0-9a-fA-F]{12}")) {
          item = String.join(":", item.split("(?<=\\G..)"));
        }
        return Optional.of(item);
    });

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    binder = new Binder<>();
    binder.setBean(bean);
    Binder.BindingBuilder<T, String> field = binder.forField(this);
    bean.getConfiguration().getValidators().forEach(field::withValidator);
    if (bean.getConfiguration().isRequired()) {
      field.asRequired(mc.getMessage(UI_COMMON_REQUIRED_VALUE_PLACEHOLDER));
    }
    field.bind(t -> t.getValue(), (t, selectOption) -> t.setValue(selectOption != null ? selectOption : null));
  }

  @Override
  public Binder getBinder() {
    return binder;
  }
}
