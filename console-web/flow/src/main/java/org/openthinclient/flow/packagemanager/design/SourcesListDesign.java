package org.openthinclient.flow.packagemanager.design;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.openthinclient.pkgmgr.db.Source;

public class SourcesListDesign extends VerticalLayout {
  protected HorizontalLayout sourcesLayout;
  protected Label sourcesLabel;
  protected Grid<Source> sourcesTable;
  protected Button updateButton;
  protected Button addSourceButton;
  protected Button deleteSourceButton;
  protected VerticalLayout sourceDetailsLayout;
  protected Label sourceDetailsLabel;
  protected FormLayout formLayout;
  protected TextField urlText;
  protected Checkbox enabledCheckbox;
  protected TextArea descriptionTextArea;
  protected Button saveButton;

}
