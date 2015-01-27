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
package org.openthinclient.console.util;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import org.openthinclient.console.nodes.views.DirObjectEditor;

import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.Validator;

/**
 * A validator which validates the parent JComponent it is attached to as well
 * as all children.
 */
public class ChildValidator implements Validator {
  private final List<Validator> childValidators = new LinkedList<Validator>();

  public void addValidatorFrom(JComponent child) {
    Validator childValidator = (Validator) child
        .getClientProperty(DirObjectEditor.KEY_VALIDATOR);
    if (null != childValidator)
      childValidators.add(childValidator);
  }

  /*
   * @see com.jgoodies.validation.Validator#validate()
   */
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    for (Validator childValidator : childValidators)
      result.addAllFrom(childValidator.validate());

    return result;
  }
}
