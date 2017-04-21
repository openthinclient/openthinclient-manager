package org.openthinclient.runtime.web.comptest.ui.tests;

import com.vaadin.ui.*;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ProgressReceiver;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.button.MButton;

public class ProgressDialogTest extends VerticalLayout implements ComponentTest {

    private final ProgressReceiverDialog dialog;
    public ProgressDialogTest() {
        setSpacing(true);

        dialog = new ProgressReceiverDialog("Test Progress Dialog");

        addComponent(new MButton("Open").withListener((Button.ClickListener) e -> dialog.open(false)));
        addComponent(new MButton("Close").withListener((Button.ClickListener) e -> dialog.close()));

        final ProgressReceiver receiver = dialog.createProgressReceiver();

        final FormLayout fl = new FormLayout();

        final TextField message = new TextField("Message");
        message.setValue("Simple Progress Message");
        fl.addComponent(message);
        final TextField value = new TextField("Value");
        value.setValue("0.2");
//        value.setConverter(Double.class);
        fl.addComponent(value);

        fl.addComponent(new MButton("Message & Value").withListener((Button.ClickListener) e -> receiver.progress(message.getValue(), (Double) value.getData())));
        fl.addComponent(new MButton("Message").withListener((Button.ClickListener) e -> receiver.progress(message.getValue())));
        fl.addComponent(new MButton("Value").withListener((Button.ClickListener) e -> receiver.progress((Double) value.getData())));
        fl.addComponent(new MButton("Success").withListener((Button.ClickListener) e -> dialog.onSuccess(new PackageManagerOperationReport())));
        fl.addComponent(new MButton("Error").withListener((Button.ClickListener) e -> dialog.onError(new Exception("With some message"))));


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
