package org.openthinclient.web.i18n;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

@BaseName("i18n/console-web-messages")
@LocaleData(defaultCharset = "UTF8", value = {@Locale("de"), @Locale("en")})
public enum ConsoleWebMessages {

  UI_DATE_FORMAT,
  UI_PAGE_TITLE,

  UI_BUTTON_YES,
  UI_BUTTON_NO,
  UI_BUTTON_CANCEL,
  UI_BUTTON_SAVE,
  UI_BUTTON_CLOSE,
  UI_BUTTON_RESET,

  UI_CAPTION_SUCCESS,
  UI_CAPTION_FAILED,

  UI_UNEXPECTED_ERROR,

  UI_LOGIN_WELCOME,
  UI_LOGIN_USERNAME,
  UI_LOGIN_PASSWORD,
  UI_LOGIN_LOGIN,
  UI_LOGIN_REMEMBERME,
  UI_LOGIN_NOTIFICATION_TITLE,
  UI_LOGIN_NOTIFICATION_DESCRIPTION,
  UI_LOGIN_NOTIFICATION_REMEMBERME_TITLE,
  UI_LOGIN_NOTIFICATION_REMEMBERME_DESCRIPTION,

  UI_WELCOMEUI_WELCOME,
  UI_WELCOMEUI_WEBCONSOLE_DESCRIPTION,
  UI_WELCOMEUI_JAVAWEBSTART_DESCRIPTION,

  UI_MENUSECTIONS_DEVICE_MANAGEMENT,
  UI_MENUSECTIONS_SERVER_MANAGEMENT,

  UI_DASHBOARDUI_LOGIN_FAILED,
  UI_DASHBOARDUI_LOGIN_UNEXPECTED_ERROR,

  UI_FILEBROWSER_HEADER,
  UI_FILEBROWSER_COLUMN_NAME,
  UI_FILEBROWSER_COLUMN_SIZE,
  UI_FILEBROWSER_COLUMN_MODIFIED,
  UI_FILEBROWSER_BUTTON_VIEWCONTENT,
  UI_FILEBROWSER_BUTTON_MKDIR,
  UI_FILEBROWSER_BUTTON_RMDIR,
  UI_FILEBROWSER_BUTTON_DOWNLOAD,
  UI_FILEBROWSER_BUTTON_UPLOAD,
  UI_FILEBROWSER_SUBWINDOW_VIEWFILE_CAPTION,
  UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_CAPTION,
  UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_REGEX,
  UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_EMPTY,
  UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_FAILED,
  UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_SAVE,
  UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_PROMPT,
  UI_FILEBROWSER_SUBWINDOW_UPLOAD_CAPTION,
  UI_FILEBROWSER_SUBWINDOW_UPLOAD_FAIL,
  UI_FILEBROWSER_SUBWINDOW_UPLOAD_SUCCESS,
  UI_FILEBROWSER_SUBWINDOW_REMOVE_CAPTION,
  UI_FILEBROWSER_SUBWINDOW_REMOVE_FOLDERNOTEMPTY,
  UI_FILEBROWSER_SUBWINDOW_REMOVE_FAIL,
  UI_FILEBROWSER_BOOKMARKS,

  UI_PRINTER_HEADER,
  UI_APPLICATION_HEADER,
  UI_APPLICATION_GROUP_HEADER,
  UI_HWTYPE_HEADER,
  UI_DEVICE_HEADER,
  UI_SETTINGS_HEADER,
  UI_CLIENT_HEADER,
  UI_LOCATION_HEADER,
  UI_USER_HEADER,
  UI_APPLICATIONGROUP_HEADER,
  UI_CLIENTGROUP_HEADER,

  UI_DASHBOARDVIEW_HEADER,
  UI_DASHBOARDVIEW_PANEL_HELP_TITLE,
  UI_DASHBOARDVIEW_PANEL_HELP_CONTENT,
  UI_DASHBOARDVIEW_PANEL_OTC_TITLE,
  UI_DASHBOARDVIEW_PANEL_OTC_CONTENT,
  UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE,
  UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT,
  UI_DASHBOARDVIEW_NOT_IMPLEMENTED,
  UI_DASHBOARDVIEW_NOTIFOCATIONS_CAPTION,
  UI_DASHBOARDVIEW_NOTIFOCATIONS_VIEWALL,

  UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION,
  UI_PACKAGEMANAGER_TAB_AVAILABLEPACKAGES,
  UI_PACKAGEMANAGER_TAB_UPDATEABLEPACKAGES,
  UI_PACKAGEMANAGER_TAB_INSTALLEDPACKAGES,
  UI_PACKAGEMANAGER_PACKAGE_NAME,
  UI_PACKAGEMANAGER_PACKAGE_VERSION,
  UI_PACKAGEMANAGER_PACKAGE_LICENSE,
  UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_SINGLE,
  UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_MULTI,
  UI_PACKAGEMANAGER_BUTTON_INSTALL_CAPTION,
  UI_PACKAGEMANAGER_ACTION_INSTALL_DESCRIPTION,
  UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_SINGLE,
  UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_MULTI,
  UI_PACKAGEMANAGER_BUTTON_UNINSTALL_CAPTION,
  UI_PACKAGEMANAGER_ACTION_UNINSTALL_DESCRIPTION,
  UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMT,
  UI_PACKAGEMANAGER_SHOW_ALL_VERSIONS,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_INSTALL_HEADLINE,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_INSTALL_BUTTON_CAPTION,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNINSTALL_HEADLINE,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNINSTALL_BUTTON_CAPTION,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_ITEMS,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_CONFLICTS,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_DEPENDING_PACKAGE,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNRESOLVED,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_SUGGESTED,
  UI_PACKAGEMANAGER_LIST_DETAILS_CAPTION,
  UI_PACKAGEMANAGER_LASTUPDATE_LABEL,
  UI_PACKAGEMANAGER_SOURCE_UPDATE_NOW_BUTTON,
  UI_PACKAGEMANAGER_INSTALLATIONPLAN_LICENSE_CAPTION,
  UI_PACKAGEMANAGER_LICENSE_VALUE,
  UI_PACKAGEMANAGER_CONFIRM_LICENCE_INFO,
  UI_PACKAGEMANAGER_PACKAGE_LICENSE_HIDE,
  UI_PACKAGEMANAGER_PACKAGE_LICENSE_SHOW,

  UI_PACKAGEMANAGER_DETAILS_COMMON_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_RELATIONS_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_CHANGELOG_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_LICENSE_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_RELATIONS_DEPENDENCIES_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_RELATIONS_PROVIDES_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_RELATIONS_CONFLICTS_CAPTION,
  UI_PACKAGEMANAGER_DETAILS_LICENSE_CHECKBOX_CAPTION,

  UI_SOURCESLISTNAVIGATORVIEW_CAPTION,
  UI_PACKAGESOURCES_BUTTON_UPDATE_CAPTION,
  UI_PACKAGESOURCES_BUTTON_SAVE_CAPTION,
  UI_PACKAGESOURCES_BUTTON_ADD_CAPTION,
  UI_PACKAGESOURCES_BUTTON_DELETE_CAPTION,
  UI_PACKAGESOURCES_URLTEXTFIELD_CAPTION,
  UI_PACKAGESOURCES_ENABLECHECKBOX_CAPTION,
  UI_PACKAGESOURCES_DESCIPRIONTEXT_CAPTION,
  UI_PACKAGESOURCES_DETAILS_CAPTION,
  UI_PACKAGESOURCES_FORM_DESCRIPTION,
  UI_PACKAGESOURCES_SOURCELIST_CAPTION,
  UI_PACKAGESOURCES_PROGRESS_CAPTION,
  UI_PACKAGESOURCES_NOTIFICATION_SAVE_CAPTION,
  UI_PACKAGESOURCES_NOTIFICATION_SAVE_DESCRIPTION,
  UI_PACKAGESOURCES_NOTIFICATION_NOTDELETED_CAPTION,
  UI_PACKAGESOURCES_NOTIFICATION_NOTDELETED_DESCRIPTION,
  UI_PACKAGESOURCES_NOTIFICATION_DELETE_CAPTION,
  UI_PACKAGESOURCES_NOTIFICATION_DELETE_DESCRIPTION,

  UI_PACKAGESOURCES_UPDATE_PROGRESS_CAPTION,
  UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_ADDED,
  UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_REMOVED,
  UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_UPDATED,
  UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_SKIPPED,
  UI_PACKAGESOURCES_UPDATE_PROGRESS_ERROR,
  UI_PACKAGESOURCES_UPDATE_AT_SOURCE_ERROR,

  // messages related to the affected applications during the uninstall of packages
  UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_MESSAGE,
  UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_HEADLINE,
  UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_TABLE_NAME,
  UI_PACKAGEMANAGER_AFFECTED_APPLICATIONS_TABLE_SCHEMANAME,

  UI_DEVICEMANAGEMENT_HEADER,
  UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_HEADER,
  UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_DESCRIPTION,
  UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_LINK,
  UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_JNLP_LINK,

  UI_SYSTEMMANAGEMENT_CONSOLE_ABOUT_HEADER,
  UI_SYSTEMMANAGEMENT_HEADER,

  UI_SERVICES_CAPTION,

  UI_SERVICE_START,
  UI_SERVICE_STOP,

  UI_SERVICE_DHCP_CONF_CAPTION,
  UI_SERVICE_DHCP_CONF_TRACK_CLIENTS,
  UI_SERVICE_DHCP_CONF_PXETYPE,
  UI_SERVICE_DHCP_CONF_PXETYPE_AUTO,
  UI_SERVICE_DHCP_CONF_PXETYPE_BIND_TO_ADDRESS,
  UI_SERVICE_DHCP_CONF_PXETYPE_EAVESDROPPING,
  UI_SERVICE_DHCP_CONF_PXETYPE_SINGLE_HOMED,
  UI_SERVICE_DHCP_CONF_PXETYPE_SINGLE_HOMED_BROADCAST,

  UI_SERVICE_DHCP_CONF_PXEPOLICY,
  UI_SERVICE_DHCP_CONF_PXEPOLICY_ONLY_CONFIGURED,
  UI_SERVICE_DHCP_CONF_PXEPOLICY_ANY_CLIENT,

  UI_SUPPORT_APPLICATION_HEADER,
  UI_SUPPORT_CONSOLE_ABOUT_HEADER,
  UI_SUPPORT_CURRENT_APPLICATION_VERSION,
  UI_SUPPORT_CURRENT_AND_NEW_APPLICATION_VERSION,
  UI_SUPPORT_CHECK_APPLICATION_VERSION_BUTTON,
  UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_CAPTION,
  UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_FAIL,
  UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_UPDATE,
  UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_OK,

  UI_SUPPORT_APPLICATION_UPDATE_RUNNING,
  UI_SUPPORT_APPLICATION_UPDATE_EXIT,
  UI_SUPPORT_APPLICATION_UPDATE_SUCCESS,

  UI_THINCLIENTS_HINT_SELECT,
  UI_THINCLIENTS_HINT_SEARCH,
  UI_THINCLIENTS_HINT_ERROR,
  UI_ASSOCIATED_DEVICES_HEADER,
  UI_THINCLIENTS_HINT_SAVE_SUCCESS,
  UI_THINCLIENTS_HINT_SAVE_ERROR,
  UI_THINCLIENTS_HINT_FILTER,
  UI_THINCLIENTS_HINT_ASSOCIATION,
  UI_THINCLIENTS_SCHEMA_NOT_LOADED,
  UI_THINCLIENTS_UNEXPECTED_ERROR,


  UI_PROFILE,
  UI_LOGOUT,

  UI_THINCLIENT_CONFIG,
  UI_THINCLIENT_IP_HOST,
  UI_THINCLIENT_MAC,
  UI_THINCLIENT_SETTINGS,
  UI_THINCLIENT_SCHEMA_VALUES,
  UI_THINCLIENT_CUSTOM_VALUES,
  UI_THINCLIENT_MAC_TIP,

  UI_SUPPORT_PROXY_CONFIGURATION_HEADER,
  UI_SUPPORT_PROXY_CONFIGURATION_DESCRIPTION,

  UI_CONFIGURATION_PROXY_CONNECTION_AUTH,
  UI_CONFIGURATION_PROXY_CONNECTION_HOST_MISSING,
  UI_CONFIGURATION_PROXY_CONNECTION_HOST_INVALID,
  UI_CONFIGURATION_PROXY_CONNECTION_PORT_INVALID,
  UI_CONFIGURATION_PROXY_ENABLED,
  UI_CONFIGURATION_PROXY_USERNAME,
  UI_CONFIGURATION_PROXY_PASSWORD,
  UI_CONFIGURATION_PROXY_HOSTNAME,
  UI_CONFIGURATION_PROXY_PORT,
  UI_SUPPORT_PROXY_CONFIGURATION_SUCCESS,
  UI_SUPPORT_PROXY_CONFIGURATION_ERROR,

  UI_PACKAGEMANAGER_MANAGER_TOO_OLD,
  UI_PACKAGEMANAGER_MANAGER_TOO_OLD_CHECK_BUTTON,

  UI_COMMON_PLEASE_SELECT,
  UI_COMMON_DELETE,
  UI_COMMON_CONFIRM_DELETE,
  UI_COMMON_CONFIRM_DELETE_OBJECT_TEXT,

  UI_COMMON_DELETE_NOT_POSSIBLE_HEADER,
  UI_COMMON_DELETE_LOCATION_DENIED,
  UI_COMMON_DELETE_HWTYPE_DENIED,
  UI_COMMON_DESCRIPTION_LABEL,
  UI_COMMON_PASSWORD_LABEL,
  UI_COMMON_PASSWORD_RETYPE_LABEL,
  UI_COMMON_NAME_LABEL,
  UI_COMMON_TYPE_LABEL,
  UI_COMMON_TYPE_TIP,
  UI_COMMON_VNC_LABEL,
  UI_COMMON_NOT_SAVED,

  UI_PROFILE_PANEL_COPY_TARGET_NAME,
  UI_PROFILE_PANEL_COPY_TARGET_NAME_WITH_NUMBER,

  UI_PROFILE_PANEL_NEW_PROFILE_HEADER,
  UI_PROFILE_PANEL_NEW_CLIENT_HEADER,
  UI_THINCLIENT_ADD_APPLICATION_LABEL,
  UI_THINCLIENT_ADD_GROUP_LABEL,
  UI_THINCLIENT_ADD_CLIENT_LABEL,
  UI_THINCLIENT_ADD_DEVICE_LABEL,
  UI_THINCLIENT_ADD_HWTYPE_LABEL,
  UI_THINCLIENT_ADD_LOCATION_LABEL,
  UI_THINCLIENT_ADD_PRINTER_LABEL,
  UI_THINCLIENT_ADD_USER_LABEL,

  UI_APPLICATIONGROUP_TIP,
  UI_USERGROUP_HEADER,
  UI_PROFILE_PANEL_NEW_APPLICATIONGROUP_HEADER,

  UI_USERS_USERNAME_TIP,
  UI_USERS_USERNAME_VALIDATOR_LENGTH,
  UI_USERS_USERNAME_VALIDATOR_NAME_EXISTS,
  UI_USERS_PASSWORD_VALIDATOR_LENGTH,
  UI_USERS_PASSWORD_RETYPE_VALIDATOR,
  UI_PROFILE_NAME_VALIDATOR,
  UI_THINCLIENT_MAC_VALIDATOR_ADDRESS,
  UI_PROFILE_NAME_REGEXP,

  UI_CLIENT_META_INFORMATION_LABEL,
  UI_CLIENT_META_INFORMATION_LABEL2,

  UI_COMMON_REQUIRED_VALUE_PLACEHOLDER,


  ;

}
