package org.openthinclient.web.thinclient.util;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_TIP_LINK;

public class ContextInfoUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContextInfoUtil.class);

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
		return "https://docs.openthinclient.com/" + version.replaceAll("(\\d+)\\.(\\d+).*","$1-$2") + "/key/";
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

	public static String prepareTip(String tip, String kbArticle) {
		String kbArticleLink = getLink(kbArticle);
		if (tip == null) {
			return kbArticleLink != null ? kbArticleLink : null;
		} else if (kbArticleLink == null) {
			return tip;
		} else {
			return tip + kbArticleLink;
		}
	}
}
