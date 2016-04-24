package org.openthinclient.runtime.web.comptest.ui;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import java.util.function.Supplier;

public class ComponentTestTile extends Panel {

    public ComponentTestTile(ComponentTestUI ui, String title, String description, Supplier<Component> componentSupplier) {

        setCaption(title);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        Label content = new Label(description);
        content.setWidth("10em");
        layout.addComponent(content);
        Button button = new Button("Open");
        button.addClickListener(e -> ui.show(title, componentSupplier));
        button.setSizeFull();
        layout.addComponent(button);
        setContent(layout);

    }
}
