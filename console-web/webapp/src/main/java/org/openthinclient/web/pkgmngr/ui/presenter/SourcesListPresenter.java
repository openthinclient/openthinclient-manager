package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.server.Page;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.pkgmgr.Source;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.vaadin.viritin.ListContainer;

import java.util.ArrayList;

public class SourcesListPresenter {

  private final View view;
  private final ArrayList<Source> sourcesList;
  private final ListContainer<Source> sourcesListContainer;
  private final FieldGroup sourceFormBinder;
  private DPKGPackageManager packageManager;

  public SourcesListPresenter(View view) {
    this.view = view;

    sourcesList = new ArrayList<>();
    sourcesListContainer = new ListContainer<>(Source.class, sourcesList);

    sourceFormBinder = new FieldGroup();
    sourceFormBinder.bind(view.getURLTextField(), "url");
    sourceFormBinder.bind(view.getDescriptionTextArea(), "description");
    sourceFormBinder.bind(view.getEnabledCheckBox(), "enabled");

    view.getURLTextField().setConverter(new StringToUrlConverter());

    this.view.getSourcesTable().setContainerDataSource(sourcesListContainer);
    // FIXME that should be part of the view, not the presenter
    view.getSourcesTable().setVisibleColumns("url");

    this.view.getSourcesTable().addValueChangeListener(this::sourcesListValueChanged);
    this.view.getSaveSourceButton().addClickListener(this::saveSourcesClicked);
    this.view.getAddSourceButton().addClickListener(this::addSourceClicked);
    this.view.getDeleteSourceButton().addClickListener(this::removeSourceClicked);
  }

  private void removeSourceClicked(Button.ClickEvent clickEvent) {

    // FIXME add some kind of confirm dialog
    Source source = view.getSelectedSource();
    sourceSelected(null);

    final SourcesList sourcesList = new SourcesList();
    sourcesList.getSources().addAll(this.sourcesList);
    sourcesList.getSources().remove(source);

    doSave(sourcesList);

    view.refreshSourcesList();

  }

  private void addSourceClicked(Button.ClickEvent clickEvent) {

    final Source newSource = new Source();
    newSource.setEnabled(true);
    newSource.setDescription("Newly created source");
    sourcesListContainer.addItem(newSource);
    sourceSelected(newSource);

  }

  private void saveSourcesClicked(Button.ClickEvent clickEvent) {

    // validate the current source
    try {
      sourceFormBinder.commit();
    } catch (FieldGroup.CommitException e) {
      e.printStackTrace();
      return;
    }

    final SourcesList sourcesList = new SourcesList();
    sourcesList.getSources().addAll(this.sourcesList);

    doSave(sourcesList);

    this.view.refreshSourcesList();
  }

  private void doSave(SourcesList sourcesList) {
    packageManager.saveSourcesList(sourcesList);

    // FIXME move that to something centrally and more managed!
    final Notification notification = new Notification("Package Sources Saved",
            "Your configuration has been successfully saved. Please do not forget to run update to reload the package cache.", Notification.Type.HUMANIZED_MESSAGE);
    notification.setStyleName(ValoTheme.NOTIFICATION_BAR + " " + ValoTheme.NOTIFICATION_SUCCESS);
    notification.show(Page.getCurrent());
  }

  private void sourcesListValueChanged(Property.ValueChangeEvent valueChangeEvent) {

    view.getSourcesTable().getValue();

  }


  private void sourceSelected(Source source) {


    if (source == null) {
      // reset

      sourceFormBinder.setEnabled(false);
      sourceFormBinder.setItemDataSource(null);

    } else {

      sourceFormBinder.setEnabled(true);

      final Item sourceItem = getSourceItem(source);
      sourceFormBinder.setItemDataSource(sourceItem);

    }

  }

  protected Item getSourceItem(Source source) {
    return sourcesListContainer.getItem(source);
  }

  public void setPackageManager(DPKGPackageManager packageManager) {
    this.packageManager = packageManager;

    updateSources(packageManager != null ? packageManager.getSourcesList() : null);

  }

  private void updateSources(SourcesList sourcesList) {

    sourcesListContainer.removeAllItems();
    if (sourcesList != null) {
      sourcesListContainer.addAll(sourcesList.getSources());
    }

    sourceSelected(null);

  }

  public interface View {

    Button getUpdateButton();

    Button getAddSourceButton();

    Button getDeleteSourceButton();

    Button getSaveSourceButton();

    TextField getURLTextField();

    CheckBox getEnabledCheckBox();

    TextArea getDescriptionTextArea();

    Table getSourcesTable();

    void refreshSourcesList();

    Source getSelectedSource();
  }
}
