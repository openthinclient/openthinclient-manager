package org.openthinclient.ldap;

import java.text.MessageFormat;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DiropLogger logs directory operations by formatting them as LDIF.
 * 
 * @author levigo
 */
public class DiropLogger {
	/**
	 * Logger to be used to log directory read operations
	 */
	private final Logger readLogger;

	/**
	 * Logger to be used to log directory write operations
	 */
	private final Logger writeLogger;

	/** Singleton instance */
	public static DiropLogger LOG = new DiropLogger(DiropLogger.class
			.getPackage().getName()
			+ ".DIROP");

	private DiropLogger(String prefix) {
		readLogger = LoggerFactory.getLogger(prefix + ".READ");
		writeLogger = LoggerFactory.getLogger(prefix + ".WRITE");
	}

	public boolean isReadEnabled() {
		return readLogger.isDebugEnabled();
	}

	public boolean isWriteEnabled() {
		return writeLogger.isDebugEnabled();
	}

	public void logModify(Name dn, ModificationItem mods[], String comment) {
		if (isWriteEnabled())
			logModify(dn.toString(), mods, comment);
	}

	public void logModify(String dn, ModificationItem mods[], String comment) {
		if (isWriteEnabled())
			try {
				writeLogger.debug("# " + comment);

				writeLogger.debug("dn: " + dn);
				writeLogger.debug("changetype: modify");
				for (final ModificationItem mi : mods) {
					final String id = mi.getAttribute().getID();
					switch (mi.getModificationOp()){
						case DirContext.ADD_ATTRIBUTE :
							writeLogger.debug("add: " + id);
							break;
						case DirContext.REMOVE_ATTRIBUTE :
							writeLogger.debug("remove: " + id);
							break;
						case DirContext.REPLACE_ATTRIBUTE :
							writeLogger.debug("replace: " + id);
							break;
					}
					for (final NamingEnumeration<?> e = mi.getAttribute().getAll(); e
							.hasMore();)
						writeLogger.debug(id + ": " + e.next());
					writeLogger.debug("-");
				}
				writeLogger.debug("");
			} catch (final Exception e) {
				writeLogger.error("Can't log operation: ", e);
			}
	}

	public void logAdd(Name dn, Attributes attributes, String comment) {
		if (isWriteEnabled())
			logAdd(dn.toString(), attributes, comment);
	}

	public void logAdd(String dn, Attributes attributes, String comment) {
		if (isWriteEnabled())
			try {
				writeLogger.debug("# " + comment);

				writeLogger.debug("dn: " + dn);
				writeLogger.debug("changetype: add");
				logAttributes(attributes);
				writeLogger.debug("");
			} catch (final NamingException e) {
				writeLogger.error("Can't log operation: ", e);
			}
	}

	public void logDelete(Name dn, String comment) {
		if (isWriteEnabled())
			logDelete(dn.toString(), comment);
	}

	public void logDelete(String dn, String comment) {
		if (isWriteEnabled()) {
			writeLogger.debug("# " + comment);

			writeLogger.debug("dn: " + dn);
			writeLogger.debug("changetype: delete");
			writeLogger.debug("");
		}
	}

	private void logAttributes(Attributes attributes) throws NamingException {
		for (final NamingEnumeration<? extends Attribute> e = attributes.getAll(); e
				.hasMore();) {
			final Attribute a = e.next();
			final String id = a.getID();
			for (final NamingEnumeration<?> f = a.getAll(); f.hasMore();)
				writeLogger.debug(id + ": " + f.next());
		}
	}

	public void logModRDN(Name oldName, Name newName, String comment) {
		if (isWriteEnabled()) {
			writeLogger.debug("# " + comment);
			writeLogger.debug("dn: " + oldName.toString());
			writeLogger.debug("changetype: modrdn");
			writeLogger.debug("newrdn: " + newName.get(newName.size() - 1));
			writeLogger.debug("");
		}
	}

	public void logReadComment(String pattern, Object... args) {
		if (isReadEnabled())
			readLogger.debug(MessageFormat.format("# " + pattern, args));
	}

	public void logGetAttributes(Name name, String[] attributes, String comment) {
		if (isReadEnabled())
			logGetAttributes(name.toString(), attributes, comment);
	}

	public void logGetAttributes(String dn, String[] attributes, String comment) {
		if (isReadEnabled()) {
			readLogger.debug("# GET ATTRIBUTES: " + comment);
			readLogger.debug("# dn: " + dn);
			if (null != attributes) {
				final StringBuilder sb = new StringBuilder("# fetching only: ");
				for (final String s : attributes)
					sb.append(s).append(" ");
				readLogger.debug(sb.toString());
			}
			readLogger.debug("");
		}
	}

	public void logSearch(String dn, String filter, Object filterArgs[],
			SearchControls sc, String comment) {
		if (isReadEnabled()) {
			readLogger.debug("# SEARCH: " + comment);
			readLogger.debug("# base: " + dn);

			readLogger.debug("# filter: " + filter);
			if (null != filterArgs)
				for (final Object arg : filterArgs)
					readLogger.debug("#    " + arg);

			readLogger.debug("# scope: "
					+ (sc.getSearchScope() == SearchControls.OBJECT_SCOPE ? "object" : sc
							.getSearchScope() == SearchControls.ONELEVEL_SCOPE
							? "onelevel"
							: "sub") + " timelimit: " + sc.getTimeLimit() + " countlimit: "
					+ sc.getCountLimit());
			readLogger.debug("");
		}
	}
}
