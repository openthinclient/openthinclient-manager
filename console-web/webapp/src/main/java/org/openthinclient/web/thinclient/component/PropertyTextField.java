package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.OtcTextProperty;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_REQUIRED_VALUE_PLACEHOLDER;

/**
 * TextField for OtcTextProperty
 */
public class PropertyTextField<T extends OtcTextProperty> extends TextField implements PropertyComponent {

  private Binder<T> binder;
  private T bean;

  public PropertyTextField(T bean) {

    setStyleName("profileItemTextfield");
    addStyleName("key-" + bean.getKey());
    setReadOnly(bean.getConfiguration().isDisabled());

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.bean = bean;

    binder = new Binder<>();
    binder.setBean(bean);

    Binder.BindingBuilder<T, String> field = binder.forField(this);
//    field.withNullRepresentation("");
    bean.getConfiguration().getValidators().forEach(field::withValidator);
    if (bean.getConfiguration().isRequired()) {
      field.asRequired(mc.getMessage(UI_COMMON_REQUIRED_VALUE_PLACEHOLDER));
    }
    field.bind(T::getValue, T::setValue);

  }

  public Binder getBinder() {
    return binder;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("bean", getBinder().getBean())
        .append("binder", getBinder())
        .append("hasChanges", getBinder().hasChanges())
        .toString();
  }
}
