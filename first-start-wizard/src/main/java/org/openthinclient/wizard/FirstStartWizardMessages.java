package org.openthinclient.wizard;
import ch.qos.cal10n.LocaleData;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.BaseName;

@BaseName("i18n/firststart-messages")
@LocaleData(  defaultCharset="UTF8",
              value = { @Locale("de"), @Locale("en") }
)
public enum FirstStartWizardMessages  {
  
  UI_PAGE_TITLE,
  
  UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_TEXT,
  UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_CLEAN_MANAGERHOME_NOTE,

  UI_FIRSTSTART_RESUME_TITLE,
  UI_FIRSTSTART_RESUME_TEXT,
  UI_FIRSTSTART_RESUME_BUTTON_INSTALLATION,
  UI_FIRSTSTART_RESUME_BUTTON_SKIP,
  UI_FIRSTSTART_RESUME_BUTTON_START,
  UI_FIRSTSTART_RESUME_RESULT,
  UI_FIRSTSTART_RESUME_RESULT_PROXY_RESTORED,
  UI_FIRSTSTART_RESUME_RESULT_PROXY_DEFAULT,
  UI_FIRSTSTART_RESUME_RESULT_PROXY_FAIL,
  UI_FIRSTSTART_RESUME_RESULT_DB_RESTORED,
  UI_FIRSTSTART_RESUME_RESULT_DB_DEFAULT,
  UI_FIRSTSTART_RESUME_RESULT_DB_FAIL,
  UI_FIRSTSTART_RESUME_RESULT_DIR_RESTORED,
  UI_FIRSTSTART_RESUME_RESULT_DIR_DEFAULT,
  UI_FIRSTSTART_RESUME_RESULT_DIR_FAIL,

  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_DIRECT_CONNECTION,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_AUTH,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_MISSING,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_INVALID,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_NO_CONNECTION,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_CHECK_NETWORK,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_CHECK_NETWORK_SUCCEED,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_CHECK_NETWORK_FAILED,
  
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_TEXT,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_VALIDATOR_DIRECTORYNAME,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_LABEL,
  
  UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_TEXT,
  UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_BUTTON_RUN,
  UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_BUTTON_RERUN,
  
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_TEXT,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_TYPE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_HOSTNAME,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_PORT,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_SCHEMA,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_TIMEZONE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_USER,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_LABEL_DB_PASSWD,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_VALUE_SYSTEM,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_VALUE_AUTODETECT,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_EXECPTION_DB_CONNECTION_FAILED,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_EXECPTION_DB_TYPE_UNSUPPORTED,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_INFO_H2,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_INFO_DERBY,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDATABASESTEP_INFO_MYSQL,
  
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_DESCRIPTION,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_ADMINISTRATOR,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_USERNAME,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_LASTNAME,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_FIRSTNAME,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_DESCRIPTION,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD_REPEAT,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEM,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMNAME,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMDESCRIPTION,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED,
  UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS,
  
  UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_TITLE,
  UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_HEADLINE,
  UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_HEAD_TEXT,
  UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_TEXT,
  UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_BUTTON_INSTALL,
  
  UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_TITLE,
  UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_DESCRIPTION,
  UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_FINISHED_MESSAGE,
  
  UI_FIRSTSTART_INSTALL_BOOTSTRAPLDAPINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_CONFIGURENFSINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_CONFIGURESYSLOGINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_CONFIGURETFTPINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_FINALIZEINSTALLATIONSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_HOMETEMPLATEINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_PACKAGEMANAGERUPDATEDPACKAGELISTINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_PREPAREDATABASEINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_PREPAREMANAGERHOMEINSTALLSTEP_LABEL,
  UI_FIRSTSTART_INSTALL_REQUIREDPACKAGESINSTALLSTEP_LABEL,
  
  UI_FIRSTSTART_INSTALL_BUTTON_RESTART,
  UI_FIRSTSTART_INSTALL_STATE_PENDING,
  UI_FIRSTSTART_INSTALL_STATE_EXECUTING,
  UI_FIRSTSTART_INSTALL_STATE_FAILED,
  UI_FIRSTSTART_INSTALL_STATE_FAILED_DESCRIPTION,
  UI_FIRSTSTART_INSTALL_STATE_SUCCEED,
  
  ;
}