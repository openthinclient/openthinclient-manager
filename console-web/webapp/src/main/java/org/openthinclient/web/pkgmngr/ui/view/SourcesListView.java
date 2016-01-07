package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.ui.*;
import org.openthinclient.pkgmgr.Source;
import org.openthinclient.web.pkgmngr.ui.design.SourcesListDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;

public class SourcesListView extends SourcesListDesign implements SourcesListPresenter.View {


  public SourcesListView() {
    sourcesTable.setSelectable(true);
    sourcesTable.setMultiSelect(false);
  }

  @Override
  public Button getUpdateButton() {
    return updateButton;

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
