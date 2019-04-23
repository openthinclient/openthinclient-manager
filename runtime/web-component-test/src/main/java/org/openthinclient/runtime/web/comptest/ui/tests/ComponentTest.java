package org.openthinclient.runtime.web.comptest.ui.tests;

import com.vaadin.flow.component.Component;

import java.util.function.Supplier;

public interface ComponentTest extends Supplier<Component> {

    String getTitle();

    String getDetails();

}
