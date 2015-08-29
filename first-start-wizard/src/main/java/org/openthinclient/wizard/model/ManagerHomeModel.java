package org.openthinclient.wizard.model;

import com.vaadin.data.util.AbstractProperty;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckManagerHomeDirectory;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;

import java.io.File;

public class ManagerHomeModel {

  public static final String DEFAULT_PATH = "/opt/openthinclient-home";

  private final ManagerHomeFactory factory;
  private final CheckExecutionEngine checkExecutionEngine;
  private final AbstractProperty<String> managerHomePath;
  private CheckStatus checkStatusManagerHomeDirectory;

  public ManagerHomeModel(CheckExecutionEngine checkExecutionEngine) {
    this.factory = new ManagerHomeFactory();
    this.checkExecutionEngine = checkExecutionEngine;

    managerHomePath = new AbstractProperty<String>() {
      @Override
      public String getValue() {
        final File home = factory.getManagerHomeDirectory();

        if (home != null) {
          return home.getAbsolutePath();
        }

        return DEFAULT_PATH;
      }

      @Override
      public void setValue(String newValue) throws ReadOnlyException {
        if (newValue == null || newValue.trim().length() == 0)
          throw new IllegalArgumentException("manager home directory must not be empty");
        if (checkStatusManagerHomeDirectory != null) {
          // FIXME shall we cancel an existing run?
          checkStatusManagerHomeDirectory = null;
        }

        factory.setManagerHomeDirectory(new File(newValue));
      }

      @Override
      public Class<? extends String> getType() {
        return String.class;
      }
    };

  }

  public boolean isManagerHomeSpecified() {
    return factory.getManagerHomeDirectory() != null;
  }

  /**
   * Check if the validation of the manager home directory has already been done. This method doesn't mandate whether the result was successful or not.
   *
   * @return <code>true</code> if a validation check has been done.
   */
  public boolean isManagerHomeValidated() {
    return isManagerHomeSpecified() && checkStatusManagerHomeDirectory != null && checkStatusManagerHomeDirectory.isFinished();
  }

  /**
   * Check whether the manager home has been validated and that the result is successful.
   *
   * @return
   */
  public boolean isManagerHomeValid() {
    return isManagerHomeValidated() &&
            (checkStatusManagerHomeDirectory.getResultType() == CheckExecutionResult.CheckResultType.SUCCESS
                    || checkStatusManagerHomeDirectory.getResultType() == CheckExecutionResult.CheckResultType.WARNING);
  }

  public CheckStatus runCheck() {

    if (!isManagerHomeSpecified())
      throw new IllegalStateException("No manager home directory has been specified");

    checkStatusManagerHomeDirectory = new CheckStatus(new CheckManagerHomeDirectory(factory.getManagerHomeDirectory()));

    checkStatusManagerHomeDirectory.executeOn(checkExecutionEngine);

    return checkStatusManagerHomeDirectory;
  }

  public AbstractProperty<String> getManagerHomePathProperty() {
    return managerHomePath;
  }
}
