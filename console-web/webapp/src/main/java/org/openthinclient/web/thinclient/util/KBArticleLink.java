package org.openthinclient.web.thinclient.util;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_TIP_LINK;

public class KBArticleLink {

	private static final Logger LOGGER = LoggerFactory.getLogger(KBArticleLink.class);

	private static IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
	private static String KBURLPrefix = getKBURLPrefix();
	private static String getKBURLPrefix() {
		java.util.Properties props = new java.util.Properties();
		try {
			InputStream in = ClassLoader.getSystemResourceAsStream("application.properties");
			props.load(in);
			in.close();
		} catch(IOException ex) {
			LOGGER.error("Could not read application properties");
			return null;
		}
		String version = props.getProperty("application.version", null);
		if(version == null) {
			LOGGER.error("Could not read application version");
			return null;
		}
		return "https://wiki.openthinclient.org/display/_PK/OMD"
				+ version.replaceAll("(\\d+)\\.(\\d+).*","$1$2")
				+ "/";
	}

	public static String getLink(String kbArticle) {
		if(KBURLPrefix == null || kbArticle == null) {
			return null;
		} else {
			return String.format("<a href=\"%s%s%s\" class=\"kblink\" target=\"_blank\">%s</a>",
					KBURLPrefix, kbArticle,
					UI.getCurrent().getLocale().getLanguage().equals("de")? "" : "#googtrans(de|en)",
					mc.getMessage(UI_PROFILE_TIP_LINK));
		}
	}

}
