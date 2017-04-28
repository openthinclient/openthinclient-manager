package org.openthinclient.web.pkgmngr.ui.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.ui.*;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.v7.ui.Table;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.web.component.NotificationDialog;
import org.openthinclient.web.component.NotificationDialog.NotificationDialogType;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.spring.events.Event;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import java.net.MalformedURLException;
import java.net.URL;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class SourcesListPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(SourcesListPresenter.class);
  
    public static final String FIELD_URL = "url";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ENABLED = "enabled";
    private final View view;
    private final Binder<Source> sourceFormBinder;
    private final BeanItemContainer<Source> container;
    private PackageManager packageManager;
    
    private final IMessageConveyor mc;
    
    public SourcesListPresenter(View view) {
        this.view = view;

//        sourceFormBinder = new FieldGroup();
//        sourceFormBinder.bind(view.getURLTextField(), FIELD_URL);
//        sourceFormBinder.bind(view.getDescriptionTextArea(), FIELD_DESCRIPTION);
//        sourceFormBinder.bind(view.getEnabledCheckBox(), FIELD_ENABLED);
        sourceFormBinder = new Binder<>();
        sourceFormBinder.forField(view.getURLTextField())
                        .withConverter(new StringToUrlConverter())
                        .bind(Source::getUrl, Source::setUrl);
        sourceFormBinder.bind(view.getDescriptionTextArea(), FIELD_DESCRIPTION);
        sourceFormBinder.bind(view.getEnabledCheckBox(), FIELD_ENABLED);


//        view.getURLTextField().setConverter(new StringToUrlConverter());

        container = new BeanItemContainer<>(Source.class);
        this.view.getSourcesTable().setContainerDataSource(container);
        // FIXME that should be part of the view, not the presenter
        view.getSourcesTable().setVisibleColumns("url");
        
        mc = new MessageConveyor(UI.getCurrent().getLocale());
        
        this.view.getUpdateButton().setCaption(mc.getMessage(UI_PACKAGESOURCES_BUTTON_UPDATE_CAPTION));
        this.view.getUpdateButtonTop().setCaption(mc.getMessage(UI_PACKAGESOURCES_BUTTON_UPDATE_CAPTION));
        this.view.getSaveSourceButton().setCaption(mc.getMessage(UI_PACKAGESOURCES_BUTTON_SAVE_CAPTION));
        this.view.getAddSourceButton().setCaption(mc.getMessage(UI_PACKAGESOURCES_BUTTON_ADD_CAPTION));
        this.view.getDeleteSourceButton().setCaption(mc.getMessage(UI_PACKAGESOURCES_BUTTON_DELETE_CAPTION));
        this.view.getURLTextField().setCaption(mc.getMessage(UI_PACKAGESOURCES_URLTEXTFIELD_CAPTION));
        this.view.getEnabledCheckBox().setCaption(mc.getMessage(UI_PACKAGESOURCES_ENABLECHECKBOX_CAPTION));
        this.view.getDescriptionTextArea().setCaption(mc.getMessage(UI_PACKAGESOURCES_DESCIPRIONTEXT_CAPTION));
        
        // Vaadin declarative design cannot handle i18n
        Label sourceListCaption = new Label(mc.getMessage(UI_PACKAGESOURCES_SOURCELIST_CAPTION));
        sourceListCaption.setStyleName("h3");
        this.view.getSourcesListLayout().replaceComponent(this.view.getSourcesLabel(), sourceListCaption);
        Label sourceDetailsCaption = new Label(mc.getMessage(UI_PACKAGESOURCES_DETAILS_CAPTION));
        sourceDetailsCaption.setStyleName("h3");
        this.view.getSourceDetailsLayout().replaceComponent(this.view.getSourceDetailsLabel(), sourceDetailsCaption);
        
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
        updatePackages();
    }

    /**
     * Updates the package-list based on current sources
     */
    private void updatePackages() {
      final ListenableProgressFuture<PackageListUpdateReport> update = packageManager.updateCacheDB();
      final ProgressReceiverDialog dialog = new ProgressReceiverDialog(mc.getMessage(UI_PACKAGESOURCES_PROGRESS_CAPTION));
      dialog.watch(update);
      dialog.open(true);
    }

    /**
     * Delete the package-list based on given source
     */
    private void deletePackages(Source source) {
      final ListenableProgressFuture<PackageListUpdateReport> update = packageManager.deleteSourcePackagesFromCacheDB(source);
      update.addCallback(
            (success) -> removeSource(source),
            (error)   -> showSourceNotDeletedError()
      );
      final ProgressReceiverDialog dialog = new ProgressReceiverDialog(mc.getMessage(UI_PACKAGESOURCES_PROGRESS_CAPTION));
      dialog.watch(update);
      dialog.open(true);
    }

    /**
     * Removes the source from database
     * @param source
     */
    private void removeSource(Source source) {
        try {
           packageManager.deleteSource(source);
           sourceSelected(null);
           container.removeItem(source);
         } catch (SourceIntegrityViolationException exception) {
           LOG.error("Cannot delete selected source.", exception);
           showSourceNotDeletedError();
           return;
         }
    }

    /**
     * Shows source-not deleted message
     */
    private void showSourceNotDeletedError() {
        final NotificationDialog notification = new NotificationDialog(mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_NOTDELETED_CAPTION),
                                                                       mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_NOTDELETED_DESCRIPTION),
                                                                       NotificationDialogType.ERROR);
        notification.open(false);
    }

    private void removeSourceClicked(Button.ClickEvent clickEvent) {

        // FIXME add some kind of confirm dialog
        ConfirmDialog.show(UI.getCurrent(), mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_DELETE_CAPTION), 
                                            mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_DELETE_DESCRIPTION), 
                                            mc.getMessage(UI_BUTTON_YES), 
                                            mc.getMessage(UI_BUTTON_NO),
                                            () ->
        {
            Source source = view.getSelectedSource();
            if (source.getId() == null) {
                // this source has not yet been added to the repository.
                return;
            } else {
                  deletePackages(source);
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
        newSource.setDescription(mc.getMessage(UI_PACKAGESOURCES_FORM_DESCRIPTION));
        container.addItem(newSource);
        sourceSelected(newSource);

    }

    private void saveSourcesClicked(Button.ClickEvent clickEvent) {

        // validate the current source
//        try {
//            sourceFormBinder.commit();
//        } catch (FieldGroup.CommitException e) {
//            e.printStackTrace();
//            return;
//        }
        BinderValidationStatus<Source> validationStatus = sourceFormBinder.validate();
        if (validationStatus.hasErrors()) {
            return;
        }
        sourceFormBinder.writeBeanIfValid(view.getSelectedSource());

        Source source = view.getSelectedSource();
        packageManager.saveSource(source);
        
        final NotificationDialog notification = new NotificationDialog(mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_SAVE_CAPTION),
                                                                       mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_SAVE_DESCRIPTION),
                                                                       NotificationDialogType.SUCCESS);
        notification.addCloseListener(e -> updatePackages());
        notification.open(false);
        
        updateSources();
    }


    private void sourcesListValueChanged(Property.ValueChangeEvent valueChangeEvent) {

        final Source selectedSource = (Source) view.getSourcesTable().getValue();

        sourceSelected(selectedSource);
    }


    private void sourceSelected(Source source) {

        view.getSourcesTable().setValue(source);

        if (source == null) {
            // reset
//            sourceFormBinder.setEnabled(false);
//            sourceFormBinder.setItemDataSource(null);
            sourceFormBinder.removeBean();
        } else {

//            sourceFormBinder.setEnabled(true);

//            final Item sourceItem = getSourceItem(source);
//            sourceFormBinder.setItemDataSource(sourceItem);
            sourceFormBinder.setBean(source);

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
        updateSources();
    }

    private void updateSources() {

        container.removeAllItems();
        if (this.packageManager != null) {
            container.addAll(this.packageManager.findAllSources());
        } else {
          LOG.error("Cannt update source because package-manager is null!");
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
        
        Label getSourceDetailsLabel() ;
        
        Label getSourcesLabel() ;
        
        HorizontalLayout getSourcesListLayout();
        
        VerticalLayout getSourceDetailsLayout();
    }
}
