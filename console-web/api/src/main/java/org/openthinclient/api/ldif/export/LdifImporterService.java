package org.openthinclient.api.ldif.export;

import org.apache.directory.server.tools.commands.importcmd.ImportCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.ldap.LdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LdifImporterService {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  static final String BASEDN_REPLACE = "#%BASEDN%#";
  private LDAPConnectionDescriptor lcd;

  public LdifImporterService(LDAPConnectionDescriptor ldapConnectionDescriptor) {
    this.lcd = ldapConnectionDescriptor;
  }

  private void importAction(File importFile) throws Exception {

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
  }

  // Following lines are copied from DirectoryEntryNode!!
  public void importTempFile(File importFile) throws Exception {
    final FileInputStream fstream = new FileInputStream(importFile);
    final DataInputStream in = new DataInputStream(fstream);
    final BufferedReader br = new BufferedReader(new InputStreamReader(in));
    final StringBuffer content = new StringBuffer();

    String strLine;
    final String baseDn = lcd.getBaseDN();

    final LdapContext ctx = lcd.createDirectoryFacade().createDirContext();
    final Name targetName = ctx.getNameParser("").parse("");

    // check if we got a root ldif file to import
    // if true: delete root tree and skip administrator entries
    // (importAction/importCommandExecutor is only able to add entries!)
    // FIXME: notice user about deleting current entries!
    if (isRootImportLdifFile(importFile))
      Util.deleteRecursively(ctx, targetName, "^cn=administrator[s]?$");

    final Pattern toReplace = Pattern.compile(".*" + BASEDN_REPLACE + "$");
    while ((strLine = br.readLine()) != null) {
      final Matcher m = toReplace.matcher(strLine);
      if (m.matches()) {
        final int pos = strLine.lastIndexOf(BASEDN_REPLACE);
        content.append(strLine.substring(0, pos) + baseDn).append(System.getProperty("line.separator"));
      } else
        content.append(strLine).append(System.getProperty("line.separator"));
    }
    in.close();

    final File tempFile = File.createTempFile("openthinclient-import-", ".ldif");
    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(tempFile.getAbsolutePath()));
      os.write(content.toString().getBytes());
    } finally {
      if (null != os) {
        os.flush();
        os.close();
      }
    }

    importAction(tempFile);
    tempFile.delete();
  }

  private static boolean isRootImportLdifFile(File importFile) throws IOException {

    final FileInputStream fstream = new FileInputStream(importFile);
    final DataInputStream in = new DataInputStream(fstream);
    final BufferedReader br = new BufferedReader(new InputStreamReader(in));

    // if second line matches "dn: BASEDN_REPLACE" it's a root import
    br.readLine();
    String s = br.readLine();
    return s != null && s.matches("^dn:[ ]+" + BASEDN_REPLACE + "$");
  }
}
