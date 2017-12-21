package org.openthinclient.web.pkgmngr.ui.design;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;

/**
 * !! DO NOT EDIT THIS FILE !!
 *
 * This class is generated by Vaadin Designer and will be overwritten.
 *
 * Please make a subclass with logic and additional interfaces as needed,
 * e.g class LoginView extends LoginDesign implements View { … }
 */
@DesignRoot
@AutoGenerated
@SuppressWarnings("serial")
public class SourcesListDesign extends VerticalLayout {
  protected HorizontalLayout sourcesLayout;
  protected Label sourcesLabel;
  protected Grid<org.openthinclient.pkgmgr.db.Source> sourcesTable;
  protected Button updateButton;
  protected Button addSourceButton;
  protected Button deleteSourceButton;
  protected VerticalLayout sourceDetailsLayout;
  protected Label sourceDetailsLabel;
  protected FormLayout formLayout;
  protected TextField urlText;
  protected CheckBox enabledCheckbox;
  protected TextArea descriptionTextArea;
  protected Button saveButton;

  public SourcesListDesign() {
		Design.read(this);
	}
}
