package org.openthinclient.ldap;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

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

		final NamingEnumeration<NameClassPair> children = ctx.list(targetName);
		try {
			while (children.hasMore()) {
				final NameClassPair child = children.next();
				targetName.add(child.getName());
				deleteRecursively(ctx, targetName);
				targetName.remove(targetName.size() - 1);
			}
		} finally {
			children.close();
		}

		if (logger.isTraceEnabled())
			logger.trace("destroySubcontext: " + targetName);
		try {
			ctx.destroySubcontext(targetName);
		} catch (final Exception e) {
		}
	}

	// FIXME: get rid of this altogether
	@Deprecated
	public static String idToUpperCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		final String[] s = member.split(",");
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
			if (i + 1 < s.length)
				ret = ret + ",";
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}

	/**
	 * @param schema
	 * @param sc
	 * @throws NamingException
	 */
	public static boolean hasObjectClass(DirContext schema, String className)
			throws NamingException {
		final SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.OBJECT_SCOPE);
		try {
			schema.list("ClassDefinition/" + className);
			return true;
		} catch (final NameNotFoundException e) {
			return false;
		}
	}
}
