package org.openthinclient.ldap;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of static utility methods.
 * 
 * @author levigo
 */
public class Util {
	private static final Logger logger = LoggerFactory.getLogger(Util.class);

	/**
	 * Recursively delete a tree at/below a given {@link Name}.
	 * 
	 * @param ctx
	 * @param targetName
	 * @throws NamingException
	 */
	public static void deleteRecursively(DirContext ctx, Name targetName)
			throws NamingException {
		deleteRecursively(ctx, targetName, null);
	}

	/**
	 * Recursively delete a tree at/below a given {@link Name}.
	 * 
	 * @param ctx
	 * @param targetName
	 * @param skipNameByRegex
	 * @throws NamingException
	 */
	public static void deleteRecursively(DirContext ctx, Name targetName,
			String skipNameByRegex) throws NamingException {

		final NamingEnumeration<NameClassPair> children = ctx.list(targetName);
		try {
			while (children.hasMore()) {
				final NameClassPair child = children.next();
				if (null != skipNameByRegex && child.getName().matches(skipNameByRegex))
					continue;
				targetName.add(child.getName());
				deleteRecursively(ctx, targetName, skipNameByRegex);
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
