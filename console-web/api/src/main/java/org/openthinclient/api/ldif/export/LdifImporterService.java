package org.openthinclient.api.ldif.export;

import org.apache.directory.server.tools.commands.importcmd.ImportCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LdifImporterService {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  static final String BASEDN_REPLACE = "#%BASEDN%#";
  private LDAPConnectionDescriptor lcd;

  public LdifImporterService(LDAPConnectionDescriptor ldapConnectionDescriptor) {
    this.lcd = ldapConnectionDescriptor;
  }

  public void importAction(File importFile) {

    try {
      if (logger.isDebugEnabled())
        logger.debug("import following temporary file: " + importFile);

      final NameCallback nc = new NameCallback("Bind DN");
      final PasswordCallback pc = new PasswordCallback("Password", false);

      lcd.getCallbackHandler().handle(new Callback[]{nc, pc});

      // Preparing the call to the Import Command
      final List<Parameter> params = new ArrayList<Parameter>();
      final ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();

      params.add(new Parameter(ImportCommandExecutor.HOST_PARAMETER, lcd
          .getHostname()));
      params.add(new Parameter(ImportCommandExecutor.PORT_PARAMETER,
          new Integer(lcd.getPortNumber())));

      switch (lcd.getAuthenticationMethod()){
        case SIMPLE :

          params.add(new Parameter(ImportCommandExecutor.AUTH_PARAMETER,
              "simple"));
          params.add(new Parameter(ImportCommandExecutor.USER_PARAMETER, nc
              .getName()));
          params.add(new Parameter(ImportCommandExecutor.PASSWORD_PARAMETER,
              new String(pc.getPassword())));
      }
      params
          .add(new Parameter(ImportCommandExecutor.FILE_PARAMETER, importFile));
      params.add(new Parameter(ImportCommandExecutor.IGNOREERRORS_PARAMETER,
          new Boolean(true)));
      params.add(new Parameter(ImportCommandExecutor.DEBUG_PARAMETER,
          new Boolean(false)));
      params.add(new Parameter(ImportCommandExecutor.VERBOSE_PARAMETER,
          new Boolean(false)));
      params.add(new Parameter(ImportCommandExecutor.QUIET_PARAMETER,
          new Boolean(false)));

      // Calling the import command
      importCommandExecutor.execute(
          params.toArray(new Parameter[params.size()]),
          new ListenerParameter[0]);

      // FIXME: implement exception/error Listener
      // see e.g.:
      // http://svn.apache.org/repos/asf/directory/sandbox/pamarcelot/trunks/ldapstudio-importexport-plugin/src/main/java/org/apache/directory/ldapstudio/importexport/controller/actions/ImportAction.java
//      bar.finished(Messages.getString("LdifImportPanel.name"),
//          Messages.getString("LdifImportPanel.text"));

    } catch (final Throwable t) {
//      bar.finished();
      logger.error("Could not import", t);
//      ErrorManager.getDefault().annotate(t, "Could not import");
//      ErrorManager.getDefault().notify(t);
    }
  }
}
