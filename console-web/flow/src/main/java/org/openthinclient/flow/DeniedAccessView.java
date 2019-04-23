package org.openthinclient.flow;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "accessDenied")
public class DeniedAccessView extends VerticalLayout {

    // unbenutzt
    DeniedAccessView() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(new Label("Access denied!"));
        add(formLayout);
    }
}