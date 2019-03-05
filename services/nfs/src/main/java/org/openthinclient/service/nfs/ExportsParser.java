package org.openthinclient.service.nfs;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportsParser {

  /**
   * Create an export from an exports-style export spec {@see man exports}.
   * There are two differences, however: the local root directory does not
   * necessarily have to be identical to the name under which it is visible.
   * Therefore a new first field is introduced: the local path name. Furthermore
   * the pipe character "|" instead of whitespace is used as delimiter.
   * <p>
   * The full format is thus: <br>
   * <code>local-path-name|name-of-share|host[/network][(options)][|host[/network][(options)]]</code>
   * <p>
   * The following options is recognized (all other options are ignored):
   * <dl>
   * <dt>ro
   * <dd>NFSExport share read-only
   * <dt>rw
   * <dd>NFSExport stare read-write (the default)
   * </dl>
   *
   * @param spec
   * @throws UnknownHostException
   */
  public NFSExport parse(String spec) throws UnknownHostException {
    final String parts[] = spec.split("\\|");
    if (parts.length < 2)
      throw new IllegalArgumentException("Can't parse export spec: " + spec);

    final NFSExport export = new NFSExport();

    export.setName(parts[1]);
    export.setRoot(new File(parts[0]));

    // parse hosts
    final Pattern p = Pattern.compile("([^\\s(]+)\\(([^\\s]+)\\)");

    for (int i = 2; i < parts.length; i++) {
      final Matcher m = p.matcher(parts[i]);
      if (!m.matches())
        throw new IllegalArgumentException("Can't parse export spec: " + spec);

      final NFSExport.Group g = new NFSExport.Group();
      if (null != m.group(1) && m.group(1).length() > 0
              && m.group(1).equals("*"))
        g.setWildcard( true);
      else if (null != m.group(1) && m.group(1).length() > 0
              && !m.group(1).equals("*")) {
        final String[] addrAndMask = m.group(1).split("/");
        switch (addrAndMask.length){
          case 2 :
            g.setAddress(InetAddress.getByName(addrAndMask[0]));
            g.setMask(Integer.parseInt(addrAndMask[1]));
            break;

          case 1 :
            g.setAddress(InetAddress.getByName(addrAndMask[0]));
            g.setMask(0);
            break;

          default :
            break;
        }
      }

      if (null != m.group(2) && m.group(2).length() > 0) {
        final String opts = m.group(2).toLowerCase();
        if (opts.indexOf("ro") >= 0)
          g.setReadOnly(true);
      }
      export.getGroups().add(g);
    }

    return export;
  }

}
