package org.openthinclient.wizard.ui.steps;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_HOSTNAME;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_TYPE;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TEXT;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TITLE;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.viritin.MBeanFieldGroup;
import org.vaadin.viritin.fields.EnumSelect;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.MethodProperty;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class ConfigureDatabaseStep extends AbstractStep {

   private final SystemSetupModel systemSetupModel;
   private final CssLayout configFormContainer;
   private final EnumSelect<DatabaseConfiguration.DatabaseType> databaseTypeField;
   private final MySQLConnectionConfigurationForm mySQLConnectionConfigurationForm;
   private final Label errorLabel;

   public ConfigureDatabaseStep(SystemSetupModel systemSetupModel) {
      this.systemSetupModel = systemSetupModel;

      mySQLConnectionConfigurationForm = new MySQLConnectionConfigurationForm(systemSetupModel.getDatabaseModel().getMySQLConfiguration());

      final VerticalLayout contents = new VerticalLayout();
      contents.setMargin(true);
      contents.setSpacing(true);
      contents.addComponent(createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_HEADLINE)));
      contents.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TEXT)));

      final FormLayout mainForm = new FormLayout();


      databaseTypeField = new EnumSelect<>(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_TYPE));
      databaseTypeField.setImmediate(true);
      databaseTypeField.setBuffered(false);
      databaseTypeField.setRequired(true);

      final MethodProperty<DatabaseConfiguration.DatabaseType> typeProperty = new MethodProperty<DatabaseConfiguration.DatabaseType>(
            systemSetupModel.getDatabaseModel(), "type");
      databaseTypeField.setPropertyDataSource(typeProperty);

      databaseTypeField.addMValueChangeListener(e -> {
         DatabaseConfiguration.DatabaseType type = (DatabaseConfiguration.DatabaseType) e.getValue();
         onDatabaseTypeChanged(type);
      });
      mainForm.addComponent(databaseTypeField);
      contents.addComponent(mainForm);

      errorLabel = new Label();
      errorLabel.setStyleName(ValoTheme.LABEL_FAILURE);
      errorLabel.setVisible(false);
      contents.addComponent(errorLabel);

      this.configFormContainer = new CssLayout();
      contents.addComponent(this.configFormContainer);

      // initialize the main form
      onDatabaseTypeChanged(systemSetupModel.getDatabaseModel().getType());

      setContent(contents);

   }

   private void onDatabaseTypeChanged(DatabaseConfiguration.DatabaseType type) {
      configFormContainer.removeAllComponents();

      if (type == DatabaseConfiguration.DatabaseType.MYSQL) {
         configFormContainer.addComponent(mySQLConnectionConfigurationForm);
      } else {
         configFormContainer.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_WARNING_H2)));
      }
   }

   @Override
   public String getCaption() {
      return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TITLE);
   }

   @Override
   public boolean onAdvance() {

      setErrorMessage(null);
      final DatabaseConfiguration.DatabaseType databaseType = databaseTypeField.getValue();
      systemSetupModel.getDatabaseModel().setType(databaseType);

      switch (databaseType) {

      case MYSQL:
         return validateMySQLConnection();
      case H2:
         return true;
      }

      // there are no other database types. This code should never be reached.
      throw new IllegalStateException(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_EXECPTION_DB_TYPE_UNSUPPORTED));
   }

   private boolean validateMySQLConnection() {

      try {
         mySQLConnectionConfigurationForm.getFieldGroup().commit();
      } catch (FieldGroup.CommitException e) {
         // do we need to do additional work here?
         return false;
      }

      final DatabaseConfiguration configuration = new DatabaseConfiguration();
      configuration.setType(DatabaseConfiguration.DatabaseType.MYSQL);
      DatabaseModel.apply(systemSetupModel.getDatabaseModel(), configuration);

      final DataSource source = DataSourceConfiguration.createDataSource(configuration, configuration.getUrl());

      try {
         DataSourceConfiguration.validateDataSource(source);
      } catch (SQLException e) {
         setErrorMessage(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_EXECPTION_DB_CONNECTION_FAILED));

         return false;
      }

      return true;
   }

   private void setErrorMessage(final String message) {
      errorLabel.setValue(message);
      errorLabel.setVisible(message != null);
   }

   @Override
   public boolean onBack() {
      // no special conditions on whether or not backwards navigation is possible.
      return true;
   }

   protected static class MySQLConnectionConfigurationForm extends FormLayout {

      private final MBeanFieldGroup<DatabaseModel.MySQLConfiguration> fieldGroup;

      public MySQLConnectionConfigurationForm(DatabaseModel.MySQLConfiguration configuration) {

         IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
        
         this.fieldGroup = new MBeanFieldGroup<>(DatabaseModel.MySQLConfiguration.class); 
         addComponent(this.fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_HOSTNAME), "hostname"));
         addComponent(this.fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_PORT), "port"));
         addComponent(this.fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_SCHEMA), "database"));
         addComponent(this.fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_USER), "username"));
         final PasswordField passwordField = this.fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_PASSWD), "password", PasswordField.class);
         passwordField.setNullRepresentation("");
         addComponent(passwordField);

         this.fieldGroup.configureMaddonDefaults();

         this.fieldGroup.setItemDataSource(configuration);

      }

      public MBeanFieldGroup<DatabaseModel.MySQLConfiguration> getFieldGroup() {
         return fieldGroup;
      }
   }

}
