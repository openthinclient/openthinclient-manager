package org.openthinclient.web.pkgmngr.ui.presenter;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_BUTTON_ADD_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_BUTTON_DELETE_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_BUTTON_SAVE_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_BUTTON_UPDATE_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_DESCIPRIONTEXT_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_ENABLECHECKBOX_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.spring.events.Event;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class SourcesListPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(SourcesListPresenter.class);
  
    public static final String FIELD_URL = "url";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ENABLED = "enabled";
    private final View view;
    private final FieldGroup sourceFormBinder;
    private final BeanItemContainer<Source> container;
    private PackageManager packageManager;
    
    private final IMessageConveyor mc;
    
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
        sourceListCaption.setStyleName("h1");
        this.view.getSourcesListLayout().replaceComponent(this.view.getSourcesLabel(), sourceListCaption);
        Label sourceDetailsCaption = new Label(mc.getMessage(UI_PACKAGESOURCES_DETAILS_CAPTION));
        sourceDetailsCaption.setStyleName("h1");
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

        final ListenableProgressFuture<PackageListUpdateReport> update = packageManager.updateCacheDB();

        final ProgressReceiverDialog dialog = new ProgressReceiverDialog(mc.getMessage(UI_PACKAGESOURCES_PROGRESS_CAPTION));
        dialog.watch(update);
        dialog.open(true);

    }

    private void removeSourceClicked(Button.ClickEvent clickEvent) {

        // FIXME add some kind of confirm dialog
        ConfirmDialog.show(UI.getCurrent(), mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_DELETE_CAPTION), 
                                            mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_DELETE_DESCRIPTION), "Yes", "No", () ->
        {
            Source source = view.getSelectedSource();

            if (source.getId() == null) {
                // this source has not yet been added to the repository.
                return;
            } else {
                try {
                  packageManager.deleteSource(source);
                  sourceSelected(null);
                  container.removeItem(source);
                } catch (SourceIntegrityViolationException exception) {
                  LOG.error("Cannot delete selected source.", exception);
                  
                  // TODO JN: show error message
                  final Notification notification = new Notification(mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_NOTDELETED_CAPTION),
                                                                     mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_NOTDELETED_DESCRIPTION), Notification.Type.HUMANIZED_MESSAGE);
                  notification.setStyleName(ValoTheme.NOTIFICATION_BAR + " " + ValoTheme.NOTIFICATION_FAILURE);
                  notification.show(Page.getCurrent());
                      
                  return;
                }
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
        try {
            sourceFormBinder.commit();
        } catch (FieldGroup.CommitException e) {
            e.printStackTrace();
            return;
        }

        Source source = view.getSelectedSource();
        packageManager.saveSource(source);
        
        // FIXME move that to something centrally and more managed!
        final Notification notification = new Notification(mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_SAVE_CAPTION),
                                                           mc.getMessage(UI_PACKAGESOURCES_NOTIFICATION_SAVE_DESCRIPTION), Notification.Type.HUMANIZED_MESSAGE);
        notification.setStyleName(ValoTheme.NOTIFICATION_BAR + " " + ValoTheme.NOTIFICATION_SUCCESS);
        notification.show(Page.getCurrent());

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
