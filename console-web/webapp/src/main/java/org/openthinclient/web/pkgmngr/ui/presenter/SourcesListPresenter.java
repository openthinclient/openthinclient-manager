package org.openthinclient.web.pkgmngr.ui.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.ui.*;
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
import java.util.Collection;
import java.util.Optional;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class SourcesListPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(SourcesListPresenter.class);
  
    private final View view;
    private final Binder<Source> sourceFormBinder;
    private PackageManager packageManager;
    
    private final IMessageConveyor mc;
    
    public SourcesListPresenter(View view) {
        this.view = view;

        sourceFormBinder = new Binder<>();
        sourceFormBinder.setBean(new Source());
        sourceFormBinder.forField(view.getURLTextField())
                        .withConverter(new StringToUrlConverter())
                        .bind(Source::getUrl, Source::setUrl);
        sourceFormBinder.forField(view.getDescriptionTextArea()).bind(Source::getDescription, Source::setDescription);
        sourceFormBinder.forField(view.getEnabledCheckBox()).bind(Source::isEnabled, Source::setEnabled);

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        this.view.getUpdateButton().setCaption(mc.getMessage(UI_PACKAGESOURCES_BUTTON_UPDATE_CAPTION));
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

        this.view.getSourcesTable().addSelectionListener(this::sourcesListValueChanged);
        this.view.getSaveSourceButton().addClickListener(this::saveSourcesClicked);
        this.view.getAddSourceButton().addClickListener(this::addSourceClicked);
        this.view.getDeleteSourceButton().addClickListener(this::removeSourceClicked);
        this.view.getUpdateButton().addClickListener(this::updateSourcesClicked);
    }

    private void updateSourcesClicked(Button.ClickEvent clickEvent) {
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
            this.view.getSourcesTable().setItems(packageManager.findAllSources());
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
                this.view.getSourcesTable().setItems(packageManager.findAllSources());
                return;
            } else {
                  deletePackages(source);
            }
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

        Collection<Source> sources = packageManager.findAllSources();
        sources.add(newSource);
        this.view.getSourcesTable().setItems(sources);
        view.getSourcesTable().select(newSource);
    }

    private void saveSourcesClicked(Button.ClickEvent clickEvent) {

        // validate the current source
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

    private void sourcesListValueChanged(SelectionEvent<Source> sourceSelectionEvent) {
        Optional<Source> selectedItem = sourceSelectionEvent.getFirstSelectedItem();
        if (selectedItem.isPresent()) {
            sourceSelected(selectedItem.get());
        } else {
            sourceFormBinder.setBean(new Source());
        }
    }

    private void sourceSelected(Source source) {
        if (source == null) {
            sourceFormBinder.setBean(new Source());
        } else {
            sourceFormBinder.setBean(source);
            // if the source has been updated (that means, a package list has been downloaded)
            // no further editing of the URL is allowed
            view.getURLTextField().setEnabled(source.getLastUpdated() == null);
        }

    }
    public void setPackageManager(PackageManager packageManager) {
        this.packageManager = packageManager;
        updateSources();
    }

    private void updateSources() {

        this.view.getSourcesTable().setItems(packageManager.findAllSources());
    }

    @EventBusListenerMethod
    public void onPackageManagerTaskActivated(Event<PackageManagerTaskActivatedEvent> event) {
//        view.getUpdateButton().setEnabled(false);
    }

    @EventBusListenerMethod
    public void onPackageManagerTaskFinalized(Event<PackageManagerTaskFinalizedEvent> event) {
//        view.getUpdateButton().setEnabled(true);
    }

    public interface View {

        Button getUpdateButton();

        Button getAddSourceButton();

        Button getDeleteSourceButton();

        Button getSaveSourceButton();

        TextField getURLTextField();

        CheckBox getEnabledCheckBox();

        TextArea getDescriptionTextArea();

        Grid<Source> getSourcesTable();

        Source getSelectedSource();
        
        Label getSourceDetailsLabel() ;
        
        Label getSourcesLabel() ;
        
        HorizontalLayout getSourcesListLayout();
        
        VerticalLayout getSourceDetailsLayout();

        void disableForm();
    }
}
