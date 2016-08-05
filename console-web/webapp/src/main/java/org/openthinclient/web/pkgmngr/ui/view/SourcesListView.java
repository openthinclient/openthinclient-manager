package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.web.pkgmngr.ui.design.SourcesListDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;

public class SourcesListView extends SourcesListDesign implements SourcesListPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = -2382414564875409740L;

  public SourcesListView() {
    sourcesTable.setSelectable(true);
    sourcesTable.setMultiSelect(false);
  }

  @Override
  public Button getUpdateButton() {
    return updateButton;
  }

  @Override
  public Button getUpdateButtonTop() {
    return updateButtonTop;
  }

  @Override
  public Table getSourcesTable() {
    return sourcesTable;
  }

  @Override
  public Button getAddSourceButton() {
    return addSourceButton;
  }

  @Override
  public Button getDeleteSourceButton() {
    return deleteSourceButton;
  }

  @Override
  public Button getSaveSourceButton() {
    return saveButton;
  }

  @Override
  public TextField getURLTextField() {
    return urlText;
  }

  @Override
  public CheckBox getEnabledCheckBox() {
    return enabledCheckbox;
  }

  @Override
  public TextArea getDescriptionTextArea() {
    return descriptionTextArea;
  }

  @Override
  public void refreshSourcesList() {
    sourcesTable.refreshRowCache();
  }

  @Override
  public Source getSelectedSource() {
    return (Source) sourcesTable.getValue();
  }

}
