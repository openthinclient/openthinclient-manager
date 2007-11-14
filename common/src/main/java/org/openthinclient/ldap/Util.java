package org.openthinclient.ldap;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;

/**
 * A bunch of static utility methods.
 * 
 * @author levigo
 */
public class Util {
	private static final Logger logger = Logger.getLogger(Util.class);

	/**
	 * Recursively delete a tree at/below a given {@link Name}.
	 * 
	 * @param ctx
	 * @param targetName
	 * @param tx
	 * @throws NamingException
	 */
	public static void deleteRecursively(DirContext ctx, Name targetName)
			throws NamingException {

		NamingEnumeration<NameClassPair> children = ctx.list(targetName);
		try {
			while (children.hasMore()) {
				NameClassPair child = children.next();
				targetName.add(child.getName());
				deleteRecursively(ctx, targetName);
				targetName.remove(targetName.size() - 1);
			}
		} finally {
			children.close();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("destroySubcontext: " + targetName);
		}
		try {
			ctx.destroySubcontext(targetName);
		} catch (Exception e) {
		}
	}

	/**
	 * @param dn
	 * @param ctx
	 * @return
	 * @throws NamingException
	 */
	public static Name makeAbsoluteName(String dn, DirContext ctx)
			throws NamingException {
		if (!dn.endsWith(ctx.getNameInNamespace())) {
			if (dn.length() > 0) {
				dn = dn + "," + ctx.getNameInNamespace();
			} else {
				dn = ctx.getNameInNamespace();
			}
		}
		return ctx.getNameParser("").parse(dn);
	}

	/**
	 * @param dn
	 * @param ctx
	 * @return
	 * @throws NamingException
	 */
	public static Name makeRelativeName(String dn, DirContext ctx)
			throws NamingException {
		// // FIXME: cache name parser
		if ((dn.length() > 0) && dn.endsWith(ctx.getNameInNamespace())
				&& !dn.equalsIgnoreCase(ctx.getNameInNamespace())) {
			dn = dn.substring(0, dn.length() - ctx.getNameInNamespace().length() - 1);
		}
		return ctx.getNameParser("").parse(dn);
	}

	// FIXME: simplify this!
	public static String idToUpperCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		String[] s = member.split(",");
		for (int i = 0; s.length > i; i++) {
			if (s[i].startsWith("cn="))
				s[i] = s[i].replaceFirst("cn=", "CN=");
			if (s[i].startsWith("dc="))
				s[i] = s[i].replaceFirst("dc=", "DC=");
			if (s[i].startsWith("ou="))
				s[i] = s[i].replaceFirst("ou=", "OU=");
			if (s[i].startsWith("l="))
				s[i] = s[i].replaceFirst("l=", "L=");
			ret = ret + s[i].trim(); // delete whitespaces
			if ((i + 1) < s.length) {
				ret = ret + ",";
			}
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}

	// FIXME: simplify this!
	public static String idToLowerCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		String[] s = member.split(",");
		for (int i = 0; s.length > i; i++) {
			if (s[i].startsWith("CN="))
				s[i] = s[i].replaceFirst("CN=", "cn=");
			if (s[i].startsWith("DC="))
				s[i] = s[i].replaceFirst("DC=", "dc=");
			if (s[i].startsWith("OU="))
				s[i] = s[i].replaceFirst("OU=", "ou=");
			if (s[i].startsWith("L="))
				s[i] = s[i].replaceFirst("L=", "l=");
			ret = ret + s[i].trim(); // delete whitespaces
			if ((i + 1) < s.length) {
				ret = ret + ",";
			}
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}
}
