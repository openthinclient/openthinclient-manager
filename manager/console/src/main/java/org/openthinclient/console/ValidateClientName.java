package org.openthinclient.console;

import java.util.ArrayList;

public class ValidateClientName {

	/**
	 * This method checks the client name to ensure it could be used as a valid
	 * hostname. Only the basic restrictions are implemented! Only
	 * (case-insensitive) alphanumeric characters, dots and hyphens are permitted.
	 * Symbols (dot / hyphen) are only permitted as separators. The hostname must
	 * be between 2 and 24 characters long.
	 */
	public String validate(String name, ArrayList<String> existingNames) {

		if (name.length() > 0) {
			if (String.valueOf(name.charAt(0)).matches("[^a-zA-Z]"))
				return Messages.getString("ValidateClientName.name.start");
			else if (name.matches(".*[^\\w|^.|^-].*"))
				return Messages.getString("ValidateClientName.name.illegal");
			else if (name.matches(".*[.|-]{2}.*"))
				return Messages.getString("ValidateClientName.name.symbol");
			else if (String.valueOf(name.charAt(name.length() - 1)).matches("[^\\w]"))
				return Messages.getString("ValidateClientName.name.end");
			else if (name.length() > 24)
				return Messages.getString("ValidateClientName.name.length");
			// following statement is used only from the setName() in DirObjectNode
			else if (existingNames != null && existingNames.contains(name))
				return Messages.getString("ValidateClientName.name.exists");
			else
				return null;
		} else
			return Messages.getString("ValidateClientName.name.mandatory");

	}
}