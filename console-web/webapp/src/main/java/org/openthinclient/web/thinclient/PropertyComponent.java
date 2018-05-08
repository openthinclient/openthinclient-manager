package org.openthinclient.web.thinclient;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;

/**
 *
 */
public interface PropertyComponent {

  Binder getBinder();

  String getLabel();

  Component getComponent();
}
