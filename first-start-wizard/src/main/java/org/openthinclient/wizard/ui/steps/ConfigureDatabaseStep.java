package org.openthinclient.wizard.ui.steps;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.spring.i18n.I18N;
import org.vaadin.viritin.MBeanFieldGroup;
import org.vaadin.viritin.fields.EnumSelect;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.MethodProperty;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ConfigureDatabaseStep extends AbstractStep {

   private final SystemSetupModel systemSetupModel;
   private final CssLayout configFormContainer;
   private final EnumSelect<DatabaseConfiguration.DatabaseType> databaseTypeField;
   private final MySQLConnectionConfigurationForm mySQLConnectionConfigurationForm;
   private final Label errorLabel;

   public ConfigureDatabaseStep(I18N i18n, SystemSetupModel systemSetupModel) {
      super(i18n);
      
      this.systemSetupModel = systemSetupModel;

      mySQLConnectionConfigurationForm = new MySQLConnectionConfigurationForm(systemSetupModel.getDatabaseModel().getMySQLConfiguration());

      final VerticalLayout contents = new VerticalLayout();
      contents.setMargin(true);
      contents.setSpacing(true);
      contents.addComponent(createLabelH1("Configure Database"));
      contents.addComponent(createLabelLarge("The openthinclient manager requires a database. Configure a database suiting your needs below."));

      final FormLayout mainForm = new FormLayout();


      databaseTypeField = new EnumSelect<>("Database type");
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
         configFormContainer.addComponent(createLabelLarge("Using the H2 database in production is not recommended."));
      }
   }

   @Override
   public String getCaption() {
      return "Configure Database";
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
      throw new IllegalStateException("Unsupported type of database selected");
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
         setErrorMessage("Database connection failed.");

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

         this.fieldGroup = new MBeanFieldGroup<>(DatabaseModel.MySQLConfiguration.class); addComponent(this.fieldGroup.buildAndBind("Hostname", "hostname"));
         addComponent(this.fieldGroup.buildAndBind("Port", "port"));
         addComponent(this.fieldGroup.buildAndBind("Database", "database"));
         addComponent(this.fieldGroup.buildAndBind("Username", "username"));
         final PasswordField passwordField = this.fieldGroup.buildAndBind("Password", "password", PasswordField.class);
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
