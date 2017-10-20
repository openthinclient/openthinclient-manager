package org.openthinclient.wizard.model;

import com.vaadin.ui.UI;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckManagerHomeDirectory;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;

import java.io.File;

public class ManagerHomeModel {

    public static final String DEFAULT_PATH = "/opt/openthinclient-home";

    private final ManagerHomeFactory factory;
    private final CheckExecutionEngine checkExecutionEngine;
    private File managerHomePath;
    private CheckStatus checkStatusManagerHomeDirectory;

    public ManagerHomeModel(ManagerHomeFactory factory, CheckExecutionEngine checkExecutionEngine) {
        this.factory = factory;
        this.checkExecutionEngine = checkExecutionEngine;

        this.managerHomePath = factory.getManagerHomeDirectory();
    }

    /**
     * Whether or not the manager home is changeable during the first start wizard.
     */
    public boolean isManagerHomeChangeable() {
        return !factory.isManagerHomeDefinedAsSystemProperty();
    }

    public boolean isManagerHomeSpecified() {
        return factory.getManagerHomeDirectory() != null;
    }

    /**
     * Check if the validation of the manager home directory has already been done. This method
     * doesn't mandate whether the result was successful or not.
     *
     * @return <code>true</code> if a validation check has been done.
     */
    public boolean isManagerHomeValidated() {
        return isManagerHomeSpecified() && checkStatusManagerHomeDirectory != null && checkStatusManagerHomeDirectory.isFinished();
    }

    /**
     * Check whether the manager home has been validated and that the result is successful.
     */
    public boolean isManagerHomeValid() {
        return isManagerHomeValidated() &&
                (checkStatusManagerHomeDirectory.getResultType() == CheckExecutionResult.CheckResultType.SUCCESS
                        || checkStatusManagerHomeDirectory.getResultType() == CheckExecutionResult.CheckResultType.WARNING);
    }

    public CheckStatus runCheck() {

        if (!isManagerHomeSpecified())
            throw new IllegalStateException("No manager home directory has been specified");

        checkStatusManagerHomeDirectory = new CheckStatus(new CheckManagerHomeDirectory(UI.getCurrent().getLocale(), factory.getManagerHomeDirectory()));

        checkStatusManagerHomeDirectory.executeOn(checkExecutionEngine);

        return checkStatusManagerHomeDirectory;
    }

    public File getManagerHomePath() {
        return managerHomePath;
    }

    public void setManagerHomePath(File managerHomePath) {
        this.managerHomePath = managerHomePath;
    }

}
