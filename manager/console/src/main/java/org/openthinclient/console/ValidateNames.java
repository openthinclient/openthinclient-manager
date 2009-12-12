package org.openthinclient.console;

public class ValidateNames {

	enum classEnum {
		Realm, Client, User, UserGroup, Application, ApplicationGroup, Device, Location, Printer, HardwareType;
	}

	public String validate(String name, Class className) {

		if (name.length() > 0)
			switch (classEnum.valueOf(className.getSimpleName())){
				case Client :
					return validateClientName(name);

				case User :
					return validateUserName(name);

				default :
					return validateRest(name);
			}
		else
			return Messages.getString("ValidateName.name.mandatory");
	}

	private String validateClientName(String name) {
		// only start with a normal letter
		if (String.valueOf(name.charAt(0)).matches("[^a-zA-Z]"))
			return Messages.getString("ValidateName.name.start");
		// only contain following characters a..z A..Z 0..9 . -
		else if (name.matches(".*[^\\w|^.|^-].*"))
			return Messages.getString("ValidateName.name.illegal");
		// no special character directly behind another special charachter
		else if (name.matches(".*[.|-]{2}.*"))
			return Messages.getString("ValidateName.name.symbol");
		// no special character at the end of the name
		else if (String.valueOf(name.charAt(name.length() - 1)).matches("[^\\w]"))
			return Messages.getString("ValidateName.name.end");
		// a minimum of two characters
		else if (name.length() < 2)
			return Messages.getString("ValidateName.name.length.min");
		// a maximum of 64 characters
		else if (name.length() > 64)
			return Messages.getString("ValidateName.name.length.max");
		else
			return null;
	}

	// FIXME if escaping in ldap works better - then this part would be redundant
	private String validateUserName(String name) {
		/**
		 * All tested operating systems (windows, linux distributions like ubuntu,
		 * cent-os, free-bsd, ...) has very different restrictions for their
		 * usernames (some allow few or none - others allow almost all special
		 * characters) so it is at this moment not possible to find the smallest
		 * common denominator
		 */
		// only start with a normal letter
		if (String.valueOf(name.charAt(0)).matches("[^a-zA-Z]"))
			return Messages.getString("ValidateName.name.start");
		// only contain following characters a..z A..Z 0..9 blanks and
		// .-_:'"`*~^@!|$%?/<>{}[]
		else if (name
				.matches(".*[^\\w|^\\s|^.|^\\-|^_|^:|^'|^\"|^`|^*|^~|^\\^|^@|^!|^\\||^$|^%|^?|^/|^<|^>|^{|^}|^\\[|^\\]].*"))
			return Messages.getString("ValidateName.name.illegal");
		// a maximum of 64 characters
		else if (name.length() > 64)
			return Messages.getString("ValidateName.name.length.max");
		else
			return null;
	}

	// FIXME if escaping in ldap works better - then this part would be redundant
	private String validateRest(String name) {
		// only start with a normal letter
		if (String.valueOf(name.charAt(0)).matches("[^a-zA-Z]"))
			return Messages.getString("ValidateName.name.start");
		// only contain following characters a..z A..Z 0..9 blanks and
		// .-_:'"`*~^@!|$%?/<>{}[]
		else if (name
				.matches(".*[^\\w|^\\s|^.|^\\-|^_|^:|^'|^\"|^`|^*|^~|^\\^|^@|^!|^\\||^$|^%|^?|^/|^<|^>|^{|^}|^\\[|^\\]].*"))
			return Messages.getString("ValidateName.name.illegal");
		// a maximum of 64 characters
		else if (name.length() > 64)
			return Messages.getString("ValidateName.name.length.max");
		else
			return null;

	}
}