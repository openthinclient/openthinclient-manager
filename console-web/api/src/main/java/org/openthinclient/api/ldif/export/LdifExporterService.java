package org.openthinclient.api.ldif.export;

import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.commands.exportcmd.ExportCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LdifExporterService {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String BASEDN_REPLACE = "#%BASEDN%#";
  LDAPConnectionDescriptor lcd;

  public LdifExporterService(LDAPConnectionDescriptor ldapConnectionDescriptor) {
    this.lcd = ldapConnectionDescriptor;
  }

  public byte[] performAction(Set<String> directoryObjectsDN, Consumer<LdifExporterService.State> result) {

    try {
      final NameCallback nc = new NameCallback("Bind DN");
      final PasswordCallback pc = new PasswordCallback("Password", false);

      lcd.getCallbackHandler().handle(new Callback[]{nc, pc});

      final List<Parameter> params = new ArrayList<Parameter>();
      params.add(new Parameter(ExportCommandExecutor.HOST_PARAMETER, lcd.getHostname()));
      params.add(new Parameter(ExportCommandExecutor.PORT_PARAMETER, (int) lcd.getPortNumber()));

      switch (lcd.getAuthenticationMethod()){
        case SIMPLE :
          params.add(new Parameter(ExportCommandExecutor.AUTH_PARAMETER, "simple"));
          params.add(new Parameter(ExportCommandExecutor.USER_PARAMETER, nc.getName()));
          params.add(new Parameter(ExportCommandExecutor.PASSWORD_PARAMETER, new String(pc.getPassword())));
      }

      params.add(new Parameter(ExportCommandExecutor.BASEDN_PARAMETER, lcd.getBaseDN()));
      params.add(new Parameter(ExportCommandExecutor.SCOPE_PARAMETER, ExportCommandExecutor.SCOPE_SUBTREE));
      params.add(new Parameter(ExportCommandExecutor.EXPORTPOINT_PARAMETER, directoryObjectsDN));

      final File temp = File.createTempFile("openthinclient-export-",".ldif");
      params.add(new Parameter(ExportCommandExecutor.FILE_PARAMETER, temp.getPath()));
      params.add(new Parameter(ExportCommandExecutor.DEBUG_PARAMETER, true));
      params.add(new Parameter(ExportCommandExecutor.VERBOSE_PARAMETER,true));

//      Set<String> result = new HashSet<>();
        final ListenerParameter listeners[] = new ListenerParameter[]{
            new ListenerParameter(
                ExportCommandExecutor.EXCEPTIONLISTENER_PARAMETER,
                new ToolCommandListener() {
                  public void notify(Serializable o) {
                    log.error("Exception occurred while exporting to ldif-file.", o);
                    result.accept(LdifExporterService.State.EXCEPTION);
                  }
                }),
            new ListenerParameter(
                ExportCommandExecutor.OUTPUTLISTENER_PARAMETER,
                new ToolCommandListener() {
                  public void notify(Serializable o) {
                    result.accept(LdifExporterService.State.SUCCESS);
                  }
                }),
            new ListenerParameter(
                ExportCommandExecutor.ERRORLISTENER_PARAMETER,
                new ToolCommandListener() {
                  public void notify(Serializable o) {
                    log.error("Error occurred while exporting to ldif-file.", o);
                    result.accept(LdifExporterService.State.ERROR);
                  }
                })};
      try {
        final ExportCommandExecutor ex = new ExportCommandExecutor();
        ex.execute(params.toArray(new Parameter[params.size()]), listeners);
      } finally {
//        if (result.contains("ERROR") || result.contains("EXCEPTION")) {
//          return null;
//        } else {
          return createExportFile(temp, lcd.getBaseDN());
//        }
      }
    } catch (final Throwable t) {

        t.printStackTrace();
//        logger.error("Could not export", t);
//        bar.finished("LdifExportPanel.name", t.toString());
    }

    return null;
  }

  private static byte[] createExportFile(File tempFile, String dn) throws Exception {
    final FileInputStream fstream = new FileInputStream(tempFile);
    final DataInputStream in = new DataInputStream(fstream);
    final BufferedReader br = new BufferedReader(new InputStreamReader(in));
    final StringBuffer content = new StringBuffer().append("version: 1").append(System.getProperty("line.separator"));
    String strLine;

    // replace last occurrence of dn with "#%BASEDN%#" on relevant entries
    final Pattern toReplace = Pattern.compile("((^dn:)|(^uniquemember:)|(^l:)) .*" + dn + "$", Pattern.CASE_INSENSITIVE);
    while ((strLine = br.readLine()) != null) {
      final Matcher m = toReplace.matcher(strLine);
      if (m.matches()) {
        final int pos = strLine.lastIndexOf(dn);
        content.append(strLine.substring(0, pos) + BASEDN_REPLACE).append(System.getProperty("line.separator"));
      } else
        content.append(strLine).append(System.getProperty("line.separator"));
    }
    in.close();
    tempFile.delete();
    return content.toString().getBytes();
  }

  public String getBaseDN() {
    return lcd.getBaseDN();
  }

  public enum State {
    EXCEPTION,
    ERROR,
    SUCCESS;
  }
}
