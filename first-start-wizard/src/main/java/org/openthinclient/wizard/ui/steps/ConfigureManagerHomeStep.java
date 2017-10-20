package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.*;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.wizard.model.CheckStatus;
import org.openthinclient.wizard.model.ManagerHomeModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

public class ConfigureManagerHomeStep extends AbstractCheckExecutingStep {

    private final SystemSetupModel systemSetupModel;
    private final VerticalLayout content;
    private final TextField homeDirectoryTextField;
    private CheckEnvironmentStep.CheckStatusLabel checkStatusLabel;
    private volatile boolean validatedProceed;
    private final Binder<ManagerHomeModel> binder;

    public ConfigureManagerHomeStep(Wizard wizard, SystemSetupModel systemSetupModel) {
        super(wizard);
        
        this.systemSetupModel = systemSetupModel;

        this.binder = new Binder();
        this.binder.setBean(systemSetupModel.getManagerHomeModel());

        homeDirectoryTextField = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_LABEL));
        homeDirectoryTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);
        homeDirectoryTextField.setStyleName(ValoTheme.TEXTFIELD_LARGE);
        homeDirectoryTextField.setEnabled(systemSetupModel.getManagerHomeModel().isManagerHomeChangeable());

        // binder for homeDirectory field
        this.binder.forField(homeDirectoryTextField)
                   .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_VALIDATOR_DIRECTORYNAME), 3, null))
                   .withConverter(new Converter<String, File>() {
                       @Override
                       public Result<File> convertToModel(String value, ValueContext context) {
                           if (value == null || value.trim().length() == 0) {
                               Result.error("manager home directory must not be empty");
                           }

                            // FIXME shall we CHECK STATUS NOW AND: cancel an existing run?
                            // if (systemSetupModel.getManagerHomeModel(). != null) {
                            // checkStatusManagerHomeDirectory = null;
                            // }

                           systemSetupModel.getFactory().setManagerHomeDirectory(new File(value));
                           return null;
                       }

                       @Override
                       public String convertToPresentation(File value, ValueContext context) {
                           return value != null ? value.getAbsolutePath() : ManagerHomeModel.DEFAULT_PATH;
                       }
                   })
                   .bind(ManagerHomeModel::getManagerHomePath, ManagerHomeModel::setManagerHomePath);

        content = new VerticalLayout(

                createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_HEADLINE)),
                createLabelHuge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_TEXT)),

                homeDirectoryTextField

        );

        content.setSpacing(true);
        content.setMargin(true);
        setContent(content);

    }

    @Override
    public String getCaption() {
        return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_TITLE);
    }

    @Override
    public boolean onAdvance() {

        // Once the checks have been executed and the result is valid, immediately proceed to the next wizard step.
        if (validatedProceed) {
            validatedProceed = true;
            return true;
        }

        // FIXME JNE: Reihenfolge beachten: das Model wurde zum Zeitpunkt 'isManagerHomeChangeable' sicher noch nicht aktualisiert!!
        if (systemSetupModel.getManagerHomeModel().isManagerHomeChangeable())
            try {
                this.binder.writeBean(systemSetupModel.getManagerHomeModel());
            } catch (ValidationException e) {
                return false;
            }

        if (systemSetupModel.getManagerHomeModel().isManagerHomeSpecified() && !systemSetupModel.getManagerHomeModel().isManagerHomeValidated()) {
            runChecks();

            return false;
        }

        return systemSetupModel.getManagerHomeModel().isManagerHomeValid();
    }

    @Override
    public boolean onBack() {
        return true;
    }

    @Override
    protected List<CheckStatusLabel> getStatusLabels() {
        if (checkStatusLabel != null) {
            return Collections.singletonList(checkStatusLabel);
        }

        return Collections.emptyList();
    }

    @Override
    protected void onRunChecks() {

        // execute the manager home validation
        final CheckStatus checkStatus = systemSetupModel.getManagerHomeModel().runCheck();

        if (checkStatusLabel != null) {
            content.removeComponent(checkStatusLabel);
        }
        checkStatusLabel = new CheckEnvironmentStep.CheckStatusLabel(checkStatus);
        content.addComponent(checkStatusLabel);
    }

    @Override
    protected void onChecksFinished() {
        if (systemSetupModel.getManagerHomeModel().isManagerHomeValid()) {
            // advance the wizard to the next step
            // specifying validatedProceed to ensure that onAdvance will immediately proceed without any further checks
            validatedProceed = true;
            wizard.next();
        }
    }

    @Override
    protected boolean isChecksFinished() {
        return systemSetupModel.getManagerHomeModel().isManagerHomeValidated();
    }
}
