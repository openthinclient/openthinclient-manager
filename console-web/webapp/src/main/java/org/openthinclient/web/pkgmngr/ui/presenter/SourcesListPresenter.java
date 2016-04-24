package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.spring.events.Event;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import java.net.MalformedURLException;
import java.net.URL;

public class SourcesListPresenter {

    public static final String FIELD_URL = "url";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ENABLED = "enabled";
    private final View view;
    private final FieldGroup sourceFormBinder;
    private final BeanItemContainer<Source> container;
    private PackageManager packageManager;

    public SourcesListPresenter(View view) {
        this.view = view;

        sourceFormBinder = new FieldGroup();
        sourceFormBinder.bind(view.getURLTextField(), FIELD_URL);
        sourceFormBinder.bind(view.getDescriptionTextArea(), FIELD_DESCRIPTION);
        sourceFormBinder.bind(view.getEnabledCheckBox(), FIELD_ENABLED);

        view.getURLTextField().setConverter(new StringToUrlConverter());

        container = new BeanItemContainer<>(Source.class);
        this.view.getSourcesTable().setContainerDataSource(container);
        // FIXME that should be part of the view, not the presenter
        view.getSourcesTable().setVisibleColumns("url");

        this.view.getSourcesTable().addValueChangeListener(this::sourcesListValueChanged);
        this.view.getSaveSourceButton().addClickListener(this::saveSourcesClicked);
        this.view.getAddSourceButton().addClickListener(this::addSourceClicked);
        this.view.getDeleteSourceButton().addClickListener(this::removeSourceClicked);
        this.view.getUpdateButton().addClickListener(this::updateSourcesClicked);
        this.view.getUpdateButtonTop().addClickListener(this::updateSourcesClicked);
    }

    private void updateSourcesClicked(Button.ClickEvent clickEvent) {

        // deselect any currently selected source
        sourceSelected(null);

        final ListenableProgressFuture<PackageListUpdateReport> update = packageManager.updateCacheDB();

        final ProgressReceiverDialog dialog = new ProgressReceiverDialog("Updating package database");
        dialog.watch(update);
        dialog.open(true);

    }

    private void removeSourceClicked(Button.ClickEvent clickEvent) {

        // FIXME add some kind of confirm dialog

        ConfirmDialog.show(UI.getCurrent(), "Delete source?", "Are you sure that you would like to delete this source?", "Yes", "No", () ->
        {
            Source source = view.getSelectedSource();
            sourceSelected(null);
            container.removeItem(source);

            if (source.getId() == null) {
                // this source has not yet been added to the repository.
                return;
            } else {
                // the source has already been persisted. Remove the entity from the database
                packageManager.getSourceRepository().delete(source);
            }

            view.refreshSourcesList();
        });
    }

    private void addSourceClicked(Button.ClickEvent clickEvent) {

        final Source newSource = new Source();
        try {
            newSource.setUrl(new URL("http://archive.openthinclient.org/openthinclient/v2.1/manager-rolling/"));
        } catch (MalformedURLException e) {
            // should never happen, as the URL is hardcoded
        }
        newSource.setEnabled(true);
        newSource.setDescription("Newly created source");
        container.addItem(newSource);
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

        Source source = view.getSelectedSource();
        final int idx = container.indexOfId(source);
        source = packageManager.getSourceRepository().saveAndFlush(source);

        // FIXME move that to something centrally and more managed!
        final Notification notification = new Notification("Package Sources Saved",
                "Your configuration has been successfully saved. Please do not forget to run update to reload the package cache.", Notification.Type.HUMANIZED_MESSAGE);
        notification.setStyleName(ValoTheme.NOTIFICATION_BAR + " " + ValoTheme.NOTIFICATION_SUCCESS);
        notification.show(Page.getCurrent());


        container.removeItem(idx);
        container.addItemAt(idx, source);

        this.view.refreshSourcesList();
    }


    private void sourcesListValueChanged(Property.ValueChangeEvent valueChangeEvent) {

        final Source selectedSource = (Source) view.getSourcesTable().getValue();

        sourceSelected(selectedSource);
    }


    private void sourceSelected(Source source) {

        view.getSourcesTable().setValue(source);

        if (source == null) {
            // reset

            sourceFormBinder.setEnabled(false);
            sourceFormBinder.setItemDataSource(null);

        } else {

            sourceFormBinder.setEnabled(true);

            final Item sourceItem = getSourceItem(source);
            sourceFormBinder.setItemDataSource(sourceItem);

            // if the source has been updated (that means, a package list has been downloaded)
            // no further editing of the URL is allowed
            view.getURLTextField().setEnabled(source.getLastUpdated() == null);

        }

    }

    protected Item getSourceItem(Source source) {
        return container.getItem(source);
    }

    public void setPackageManager(PackageManager packageManager) {
        this.packageManager = packageManager;

        updateSources(packageManager != null ? packageManager.getSourceRepository() : null);

    }

    private void updateSources(SourceRepository repository) {

        container.removeAllItems();
        if (repository != null) {
            container.addAll(repository.findAll());
        }

        sourceSelected(null);

    }

    @EventBusListenerMethod
    public void onPackageManagerTaskActivated(Event<PackageManagerTaskActivatedEvent> event) {
        view.getUpdateButton().setEnabled(false);
    }

    @EventBusListenerMethod
    public void onPackageManagerTaskFinalized(Event<PackageManagerTaskFinalizedEvent> event) {
        view.getUpdateButton().setEnabled(true);
    }

    public interface View {

        Button getUpdateButton();

        Button getAddSourceButton();

        Button getDeleteSourceButton();

        Button getSaveSourceButton();

        TextField getURLTextField();

        CheckBox getEnabledCheckBox();

        TextArea getDescriptionTextArea();

        Button getUpdateButtonTop();

        Table getSourcesTable();

        void refreshSourcesList();

        Source getSelectedSource();
    }
}
