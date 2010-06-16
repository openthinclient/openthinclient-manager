package org.openthinclient.console;

import java.util.HashMap;

/**
 * QUICK AND VERY DIRTY !!!
 */
public class ClientStatus {

	private static HashMap<String, String> clientStatus = new HashMap<String, String>();

	public static void setClientStatus(String clientName, String status) {
		clientStatus.put(clientName, status);
	}

	public static String getClientStatus(String clientName) {
		if (clientStatus.get(clientName) == null
				|| clientStatus.get(clientName) == "Unchecked")
			return "Unchecked";
		else
			return clientStatus.get(clientName);
	}
}
