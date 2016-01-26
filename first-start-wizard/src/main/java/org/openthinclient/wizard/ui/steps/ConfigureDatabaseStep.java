package org.openthinclient.wizard.ui.steps;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.viritin.MBeanFieldGroup;
import org.vaadin.viritin.fields.EnumSelect;

public class ConfigureDatabaseStep extends AbstractStep {

   private final SystemSetupModel systemSetupModel;
   private final CssLayout configFormContainer;
   private final EnumSelect databaseTypeField;
   private final MySQLConnectionConfigurationForm mySQLConnectionConfigurationForm;

   public ConfigureDatabaseStep(SystemSetupModel systemSetupModel) {
      this.systemSetupModel = systemSetupModel;

      mySQLConnectionConfigurationForm = new MySQLConnectionConfigurationForm(systemSetupModel.getDatabaseModel().getMySQLConfiguration());

      final VerticalLayout contents = new VerticalLayout();
      contents.setMargin(true);
      contents.setSpacing(true);
      contents.addComponent(createLabelH1("Configure Database"));
      contents.addComponent(createLabelLarge("The openthinclient manager requires a database. Configure a database suiting your needs below."));

      final FormLayout mainForm = new FormLayout();

      MBeanFieldGroup<DatabaseModel> fieldGroup = new MBeanFieldGroup<>(DatabaseModel.class);
      fieldGroup.setItemDataSource(systemSetupModel.getDatabaseModel());
      databaseTypeField = new EnumSelect<DatabaseConfiguration.DatabaseType>("Database type");
      databaseTypeField.setImmediate(true);
      fieldGroup.bind(databaseTypeField, "type");

      databaseTypeField.addMValueChangeListener(e -> {
         DatabaseConfiguration.DatabaseType type = (DatabaseConfiguration.DatabaseType) e.getValue();
         onDatabaseTypeChanged(type);
      });
      mainForm.addComponent(databaseTypeField);
      contents.addComponent(mainForm);

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
      return false;
   }

   @Override
   public boolean onBack() {
      // no special conditions on whether or not backwards navigation is possible.
      return true;
   }

   protected static class MySQLConnectionConfigurationForm extends FormLayout {

      public MySQLConnectionConfigurationForm(DatabaseModel.MySQLConfiguration configuration) {
         MBeanFieldGroup<DatabaseModel.MySQLConfiguration> fieldGroup = new MBeanFieldGroup<>(DatabaseModel.MySQLConfiguration.class);

         addComponent(fieldGroup.buildAndBind("Hostname", "hostname"));
         addComponent(fieldGroup.buildAndBind("Port", "port"));
         addComponent(fieldGroup.buildAndBind("Database", "database"));
         addComponent(fieldGroup.buildAndBind("Username", "username"));
         final PasswordField passwordField = fieldGroup.buildAndBind("Password", "password", PasswordField.class);
         passwordField.setNullRepresentation("");
         addComponent(passwordField);

         fieldGroup.configureMaddonDefaults();

         fieldGroup.setItemDataSource(configuration);

      }
   }

}
