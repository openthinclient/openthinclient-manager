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
package org.openthinclient.ldap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author levigo
 */
public class Filter {
  private static final Pattern SHIFT_PATTERN = Pattern
      .compile("(.*?)(?:\\{(\\d+)\\}(.*?))*");

  private String expression;
  private Object args[];

  public Filter(String expression, Object... args) {
    this.expression = expression;
    this.args = args;
  }

  public String getExpression(int argumentOffset) {
    if (argumentOffset == 0) {
      return expression;
    }

    Matcher m = SHIFT_PATTERN.matcher(expression);
    assert m.matches() : "SHIFT_PATTERN doesn't match";

    int groupCount = m.groupCount();
    StringBuffer sb = new StringBuffer(m.group(1));
    int i = 2;
    while (i <= groupCount) {
      int groupNumber = Integer.parseInt(m.group(i));
      sb.append("{").append(groupNumber + argumentOffset).append("}");
      i++;
      if (i <= groupCount) {
        sb.append(m.group(i));
        i++;
      }
    }

    return sb.toString();
  }

  public void fillArguments(Object target[], int argumentOffset) {
    System.arraycopy(args, 0, target, argumentOffset, args.length);
  }

  public Object[] getArgs() {
    return args;
  }

  public int getArgumentCount() {
    return args.length;
  }
}
