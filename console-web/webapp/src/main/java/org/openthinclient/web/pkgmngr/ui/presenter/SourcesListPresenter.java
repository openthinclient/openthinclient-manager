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
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.vaadin.spring.events.Event;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

public class SourcesListPresenter {

    private final View view;
    private final FieldGroup sourceFormBinder;
    private final BeanItemContainer<Source> container;
    private PackageManager packageManager;

    public SourcesListPresenter(View view) {
        this.view = view;

        sourceFormBinder = new FieldGroup();
        sourceFormBinder.bind(view.getURLTextField(), "url");
        sourceFormBinder.bind(view.getDescriptionTextArea(), "description");
        sourceFormBinder.bind(view.getEnabledCheckBox(), "enabled");

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
    }

    private void updateSourcesClicked(Button.ClickEvent clickEvent) {
        packageManager.updateCacheDB();
    }

    private void removeSourceClicked(Button.ClickEvent clickEvent) {

        // FIXME add some kind of confirm dialog
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

    }

    private void addSourceClicked(Button.ClickEvent clickEvent) {

        final Source newSource = new Source();
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

        Table getSourcesTable();

        void refreshSourcesList();

        Source getSelectedSource();
    }
}
