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
 ******************************************************************************/
package org.openthinclient.util.dpkg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeepObjectCopy {

	private static final Logger logger = LoggerFactory.getLogger(DeepObjectCopy.class);

	public static Object clone(Object copyObject, PackageManagerTaskSummary taskSummary)
			throws PackageManagerException {
		try {
			ByteArrayOutputStream byArrOutStr = new ByteArrayOutputStream(4096);
			ObjectOutputStream objOutStr = new ObjectOutputStream(byArrOutStr);
			objOutStr.writeObject(copyObject);
			ByteArrayInputStream byArrInStr = new ByteArrayInputStream(byArrOutStr
					.toByteArray());
			ObjectInputStream objINStr = new ObjectInputStream(byArrInStr);
			Object deepCopy = objINStr.readObject();
			return deepCopy;
		} catch (IOException e) {
			String errorMessage = I18N.getMessage("DeepObjectCopy.clone.IOException");
			if (taskSummary != null) {
				taskSummary.addWarning(errorMessage);
				logger.error(errorMessage);
			} else
				logger.error(errorMessage);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			String errorMessage = I18N.getMessage("DeepObjectCopy.clone.ClassNotFoundException");
			if (taskSummary != null) {
				taskSummary.addWarning(errorMessage);
				logger.error(errorMessage);
			} else
				logger.error(errorMessage);
			e.printStackTrace();
		}
		return null;
	}

}
