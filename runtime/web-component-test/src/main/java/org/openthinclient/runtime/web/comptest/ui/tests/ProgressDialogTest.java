package org.openthinclient.runtime.web.comptest.ui.tests;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import org.openthinclient.pkgmgr.progress.ProgressReceiver;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.button.MButton;

public class ProgressDialogTest extends VerticalLayout implements ComponentTest {

    private final ProgressReceiverDialog dialog;
    public ProgressDialogTest() {
        setSpacing(true);

        dialog = new ProgressReceiverDialog("Test Progress Dialog");

        addComponent(new MButton("Open").withListener(e -> dialog.open(false)));
        addComponent(new MButton("Close").withListener(e -> dialog.close()));

        final ProgressReceiver receiver = dialog.createProgressReceiver();

        final FormLayout fl = new FormLayout();

        final TextField message = new TextField("Message");
        message.setValue("Simple Progress Message");
        fl.addComponent(message);
        final TextField value = new TextField("Value");
        value.setValue("0.2");
        value.setConverter(Double.class);
        fl.addComponent(value);

        fl.addComponent(new MButton("Message & Value").withListener(e -> receiver.progress(message.getValue(), (Double) value.getConvertedValue())));
        fl.addComponent(new MButton("Message").withListener(e -> receiver.progress(message.getValue())));
        fl.addComponent(new MButton("Value").withListener(e -> receiver.progress((Double) value.getConvertedValue())));
        fl.addComponent(new MButton("Success").withListener(e -> dialog.onSuccess()));
        fl.addComponent(new MButton("Error").withListener(e -> dialog.onError(new Exception("With some message"))));


        addComponent(fl);
    }

    @Override
    public String getTitle() {
        return "Progress Dialog";
    }

    @Override
    public String getDetails() {
        return "The Dialog providing a integration with the ProgressReceiver.";
    }

    @Override
    public Component get() {
        return this;
    }
}
