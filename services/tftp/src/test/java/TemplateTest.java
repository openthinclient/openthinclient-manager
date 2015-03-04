import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/

/**
 * @author levigo
 */
public class TemplateTest extends TestCase {
  public void testFillTemplate() throws Exception {
    String file = streamAsString(getClass()
        .getResourceAsStream("/template.txt"));

    Map<String, String> client = new HashMap<String, String>();
    client.put("BootOptions.KernelName", "yada-kernel-name");
    client.put("BootOptions.InitrdName", "yada-initrd-name");
    client.put("BootOptions.NFSRootserver", "yada-rootserver");
    client.put("BootOptions.NFSRootPath", "yada-rootpath");

    System.out.println("before: " + file);

    Pattern p = Pattern.compile("\\$\\{([^\\}]+)\\}", Pattern.MULTILINE);

    StringBuffer result = new StringBuffer();
    Matcher m = p.matcher(file);
    while (m.find()) {
      String group = m.group(1);
      System.out.println("matches. group: " + group);
      String value = client.get(group);
      if (null == value)
        System.out
            .println("Pattern refers to undefined variable " + m.group(1));
      m.appendReplacement(result, null != value ? value : "");
    }
    m.appendTail(result);

    String processed = result.toString().replaceAll("\\r", "");

    // join continuation lines
    processed = processed.replaceAll("\\\\[\\t ]*\\n", "");

    // save space by collapsing all spaces
    processed = processed.replaceAll("[\\t ]+", " ");

    
    System.out.println("result: " + processed);
  }

  public void testReplace() throws Exception {
    System.out.println("bla ${foo} bla".replaceAll("\\$", "\\\\\\$"));
  }
  
  private String streamAsString(InputStream is) throws IOException {
    ByteArrayOutputStream s = new ByteArrayOutputStream();
    byte b[] = new byte[1024];
    int read;
    while ((read = is.read(b)) >= 0)
      s.write(b, 0, read);

    is.close();
    return s.toString("ASCII");
  }
}
