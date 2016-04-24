package org.openthinclient.runtime.web.comptest.ui.tests;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

public class ProgressDialogTest extends VerticalLayout implements ComponentTest {
    public ProgressDialogTest() {
        setSpacing(true);

        addComponent(new Button("Open"));
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
