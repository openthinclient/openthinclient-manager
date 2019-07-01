package org.openthinclient.web.support.ui;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;

import org.openthinclient.web.ui.ViewHeader;

/**
 * !! DO NOT EDIT THIS FILE !!
 * <p>
 * This class is generated by Vaadin Designer and will be overwritten.
 * <p>
 * Please make a subclass with logic and additional interfaces as needed, e.g class LoginView
 * extends LoginDesign implements View { }
 */
@DesignRoot
@AutoGenerated
@SuppressWarnings("serial")
public class SystemReportDesign extends VerticalLayout {
  protected ViewHeader header;
  protected Label descriptionLabel;
  protected Button generateReportButton;
  protected VerticalLayout resultLayout;
  protected Label reportTransmittedDescriptionLabel;
  protected Label supportIdLabel;

  public SystemReportDesign() {
    Design.read(this);
  }
}
