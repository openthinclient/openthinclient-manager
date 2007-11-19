package org.openthinclient.ldap;

import java.util.Enumeration;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapName;

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

		if (logger.isDebugEnabled())
			logger.debug("destroySubcontext: " + targetName);
		try {
			ctx.destroySubcontext(targetName);
		} catch (final Exception e) {
		}
	}

	/**
	 * @param name
	 * @param connectionDescriptor
	 * @return
	 * @throws NamingException
	 * 
	 * FIXME: respect upper case DN requirements
	 */
	public static Name makeAbsoluteName(String name,
			LDAPConnectionDescriptor connectionDescriptor) throws NamingException {
		final Name parsedName = connectionDescriptor.getNameParser().parse(name);

		// if the name is relative, append ctx base dn
		if (!connectionDescriptor.contains(parsedName))
			parsedName.addAll(0, connectionDescriptor.getBaseDNName());

		return parsedName;
	}

	/**
	 * @param name
	 * @param connectionDescriptor
	 * @return
	 * @throws NamingException
	 * 
	 * FIXME: respect upper case DN requirements
	 */
	public static Name makeRelativeName(String name,
			LDAPConnectionDescriptor connectionDescriptor) throws NamingException {
		final Name parsedName = connectionDescriptor.getNameParser().parse(name);

		// return name directly if it is not absolute
		if (!parsedName.startsWith(connectionDescriptor.getBaseDNName()))
			return parsedName;

		// don't remove suffix, if the connections base DN is zero-sized
		if (connectionDescriptor.getBaseDNName().size() == 0)
			return parsedName;

		return parsedName.getSuffix(connectionDescriptor.getBaseDNName().size());
	}

	// FIXME: simplify this!
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

	// FIXME: simplify this!
	@Deprecated
	public static String idToLowerCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		final String[] s = member.split(",");
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
			if (i + 1 < s.length)
				ret = ret + ",";
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}

	/**
	 * Adjust the case of the attribute names in the given name according to the
	 * needs of the target directory. E.g. ActiveDirectory wants all upper-case
	 * names.
	 * 
	 * FIXME: making use of the fact that the parsed names are actually
	 * {@link LdapName}s could make this more efficient.
	 * 
	 * @param memberDN
	 * @param connectionDescriptor
	 * @return
	 * @throws NamingException
	 */
	public static String fixNameCase(String memberDN,
			LDAPConnectionDescriptor connectionDescriptor) throws NamingException {

		if (!connectionDescriptor.guessDirectoryType()
				.requiresUpperCaseRDNAttributeNames())
			return memberDN;

		// use context's name parser to split the name into parts
		final Name parsed = connectionDescriptor.getNameParser().parse(memberDN);
		Name adjusted = null;
		for (final Enumeration<String> e = parsed.getAll(); e.hasMoreElements();) {
			String part = e.nextElement();

			final int idx = part.indexOf('=');
			final char c[] = part.toCharArray();
			for (int i = 0; i < idx; i++)
				c[i] = Character.toUpperCase(c[i]);
			part = new String(c);

			if (null == adjusted)
				adjusted = connectionDescriptor.getNameParser().parse(part);
			else
				adjusted.add(part);
		}

		return adjusted.toString();
	}
}
