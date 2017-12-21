package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.ui.*;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.web.pkgmngr.ui.design.SourcesListDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;

public class SourcesListView extends SourcesListDesign implements SourcesListPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = -2382414564875409740L;

  public SourcesListView() {
    sourcesTable.setSelectionMode(Grid.SelectionMode.SINGLE);
    sourcesTable.addColumn(Source::getUrl);
  }

  @Override
  public Button getUpdateButton() {
    return updateButton;
  }

  @Override
  public Grid<Source> getSourcesTable() {
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
  public Source getSelectedSource() {
    return (Source) this.sourcesTable.getSelectedItems().stream().findFirst().get();
  }

  @Override
  public Label getSourcesLabel() {
     return sourcesLabel;
  }
  
  @Override
  public Label getSourceDetailsLabel() {
     return sourceDetailsLabel;
  }

   @Override
   public HorizontalLayout getSourcesListLayout() {
      return sourcesLayout;
   }

   @Override
   public VerticalLayout getSourceDetailsLayout() {
      return sourceDetailsLayout;
   }

   @Override
   public void disableForm() {
    formLayout.setEnabled(false);
   }
}
