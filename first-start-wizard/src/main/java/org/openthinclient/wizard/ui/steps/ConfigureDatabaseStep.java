package org.openthinclient.wizard.ui.steps;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.SystemSetupModel;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

public class ConfigureDatabaseStep extends AbstractStep {

   private final SystemSetupModel systemSetupModel;
   private final CssLayout configFormContainer;
//   private final EnumSelect<DatabaseConfiguration.DatabaseType> databaseTypeField;
   private final MySQLConnectionConfigurationForm mySQLConnectionConfigurationForm;
   private final Label errorLabel;
   private NativeSelect<DatabaseConfiguration.DatabaseType> select;

   private Binder<DatabaseModel> databaseTypeBinder;

   public ConfigureDatabaseStep(SystemSetupModel systemSetupModel) {
      this.systemSetupModel = systemSetupModel;

      mySQLConnectionConfigurationForm = new MySQLConnectionConfigurationForm(systemSetupModel.getDatabaseModel().getMySQLConfiguration());

      final VerticalLayout contents = new VerticalLayout();
      contents.setMargin(true);
      contents.setSpacing(true);
      contents.addComponent(createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_HEADLINE)));
      contents.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TEXT)));

      final FormLayout mainForm = new FormLayout();

      select = new NativeSelect<DatabaseConfiguration.DatabaseType>(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_TYPE));
      select.setItems(DatabaseConfiguration.DatabaseType.values());
      select.setEmptySelectionAllowed(false);

      databaseTypeBinder = new Binder();
      databaseTypeBinder.setBean(systemSetupModel.getDatabaseModel());
      databaseTypeBinder.forField(select)
                        .bind(DatabaseModel::getType, DatabaseModel::setType);

      databaseTypeBinder.addValueChangeListener(event -> {
         DatabaseConfiguration.DatabaseType type = (DatabaseConfiguration.DatabaseType) event.getValue();
         onDatabaseTypeChanged(type);
      });
      mainForm.addComponent(select);
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
         configFormContainer.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_INFO_MYSQL), ContentMode.HTML));
         configFormContainer.addComponent(mySQLConnectionConfigurationForm);
      } else if (type == DatabaseConfiguration.DatabaseType.APACHE_DERBY) {
         configFormContainer.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_INFO_DERBY), ContentMode.HTML));
      } else {
        configFormContainer.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_INFO_H2), ContentMode.HTML));
      }
   }

   @Override
   public String getCaption() {
      return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TITLE);
   }

   @Override
   public boolean onAdvance() {

      setErrorMessage(null);
      databaseTypeBinder.writeBeanIfValid(this.systemSetupModel.getDatabaseModel());

      switch (this.systemSetupModel.getDatabaseModel().getType()) {
        case APACHE_DERBY:
          return true;
        case MYSQL:
           return validateMySQLConnection();
        case H2:
           return true;
      }

      // there are no other database types. This code should never be reached.
      throw new IllegalStateException(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_EXECPTION_DB_TYPE_UNSUPPORTED));
   }

   private boolean validateMySQLConnection() {

      mySQLConnectionConfigurationForm.writeBeanIfValid();

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

//      private final MBeanFieldGroup<DatabaseModel.MySQLConfiguration> fieldGroup;
      private Binder<DatabaseModel.MySQLConfiguration> mySQLConnectionConfigurationBinder;
      private DatabaseModel.MySQLConfiguration configuration;

      public MySQLConnectionConfigurationForm(DatabaseModel.MySQLConfiguration configuration) {

         IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

         this.configuration = configuration;

         mySQLConnectionConfigurationBinder = new Binder<>();
         mySQLConnectionConfigurationBinder.setBean(this.configuration);
         
         TextField hostName = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_HOSTNAME), "hostname");
         mySQLConnectionConfigurationBinder.bind(hostName, DatabaseModel.MySQLConfiguration::getHostname, DatabaseModel.MySQLConfiguration::setHostname);
         addComponent(hostName);

         // portField
         TextField portField = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_PORT), "3306");
         this.mySQLConnectionConfigurationBinder.forField(portField)
                 .withConverter(new StringToIntegerConverter(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID)) {
                    @Override
                    protected NumberFormat getFormat(Locale locale) {
                       // do not use a thousands separator, as HTML5 input type
                       // number expects a fixed wire/DOM number format regardless
                       // of how the browser presents it to the user (which could
                       // depend on the browser locale)
                       DecimalFormat format = new DecimalFormat();
                       format.setMaximumFractionDigits(0);
                       format.setDecimalSeparatorAlwaysShown(false);
                       format.setParseIntegerOnly(true);
                       format.setGroupingUsed(false);
                       return format;
                    }
                 })
                 .withValidator(new IntegerRangeValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID), 1, 65535))
                 .bind(DatabaseModel.MySQLConfiguration::getPort, DatabaseModel.MySQLConfiguration::setPort);
         addComponent(portField);

         TextField databaseField = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_SCHEMA), "database");
         this.mySQLConnectionConfigurationBinder.bind(databaseField, DatabaseModel.MySQLConfiguration::getDatabase, DatabaseModel.MySQLConfiguration::setDatabase);
         addComponent(databaseField);

         TextField usernameField = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_USER), "username");
         this.mySQLConnectionConfigurationBinder.bind(usernameField, DatabaseModel.MySQLConfiguration::getUsername, DatabaseModel.MySQLConfiguration::setUsername);
         addComponent(usernameField);

         PasswordField passwordField = new PasswordField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_PASSWD), "password");
         this.mySQLConnectionConfigurationBinder.bind(passwordField, DatabaseModel.MySQLConfiguration::getPassword, DatabaseModel.MySQLConfiguration::setPassword);
         addComponent(passwordField);


//         this.fieldGroup.configureMaddonDefaults();
//         this.fieldGroup.setItemDataSource(configuration);

      }

      public void writeBeanIfValid() {
         mySQLConnectionConfigurationBinder.writeBeanIfValid(configuration);
      }

   }

}
